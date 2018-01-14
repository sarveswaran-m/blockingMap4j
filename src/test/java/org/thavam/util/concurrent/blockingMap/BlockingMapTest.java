/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thavam.util.concurrent.blockingMap;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Sarveswaran M
 */
public class BlockingMapTest {

    //thread names
    public static final String CONSUMER_1 = "consumer-1";
    public static final String CONSUMER_2 = "consumer-2";
    public static final String PRODUCER_1 = "producer-1";
    public static final String PRODUCER_2 = "producer-2";

    //map containing conditions
    private static volatile BlockingMap<Integer, String> blockingMap = null;

    //value returned on interruption
    private static final String INTERRUPTED = "INTERRUPTED";
    private static ExecutorService executor;
    //blocking thread
    private static Callable<String> producer1, producer2;
    private static FutureTask<String> consumerTask1, consumerTask2;

    private static void printDecoratedMessage(String msg) {
        Logger.getLogger(BlockingMapTest.class.getName()).log(
                Level.INFO, Thread.currentThread().getName() + " : " + msg);
    }

    public BlockingMapTest() {
        blockingMap = new BlockingHashMap<>();
        executor = Executors.newCachedThreadPool();
    }

    @BeforeClass
    public static void setUpClass() {

        //first consumer thread
        consumerTask1 = new FutureTask<String>(new Callable<String>() {
            @Override
            public String call() {
                String value = null;
                try {
                    value = blockingMap.take(1);
                } catch (InterruptedException ex) {
                    printDecoratedMessage("Exception on consumer 1");
                    value = INTERRUPTED;
                }
                return value;
            }
        });

        //second consumer thread
        consumerTask2 = new FutureTask<String>(new Callable<String>() {
            @Override
            public String call() {
                String value = null;
                try {
                    value = blockingMap.take(Integer.MIN_VALUE);
                } catch (InterruptedException ex) {
                    printDecoratedMessage("Exception on consumer 2");
                    value = INTERRUPTED;
                }
                return value;
            }
        });

        //producer thread 1
        producer1 = new Callable<String>() {
            @Override
            public String call() throws Exception {
                String value = null;
                try {
                    Thread.sleep(500);
                    value = blockingMap.put(1, "one");
                } catch (InterruptedException e) {
                    //interrupted
                }
                return value;
            }
        };

        //producer thread 2
        producer2 = new Callable<String>() {
            @Override
            public String call() throws Exception {
                String value = null;
                try {
                    Thread.sleep(500);
                    value = blockingMap.put(2, "two");
                } catch (InterruptedException e) {
                    //interrupted
                }
                return value;
            }
        };
    }

    @AfterClass
    public static void tearDownClass() {
        executor.shutdown();
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

    /**
     * Test of clear to interrupt blocked threads. Both consumers wait for their
     * keys of interest before producers start producing. This is ensured by
     * making producers sleep before production.
     *
     * consumer2 waits on non-existent key, there it will block for-ever on
     * clear, consumer2 should be interrupted
     *
     * Bug Id #1 https://github.com/sarveswaran-m/blockingMap4j/issues/1
     */
    @Test
    public void testClear() throws InterruptedException, ExecutionException {
        /**
         * log messages introduced to understand thread interaction. should be
         * removed.
         *
         * Assertion for marker value on blocked consumer or expecting
         * Interrupted exception would suffice
         */
        printDecoratedMessage("Starting consumers");

        executor.submit(consumerTask1);
        executor.submit(consumerTask2);

        printDecoratedMessage("Starting Consumers");
        executor.submit(producer1);
        executor.submit(producer2);

        executor.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    printDecoratedMessage("waiting for results on different thread:");
                    printDecoratedMessage("consumerTask1.get() = " + consumerTask1.get());
                    printDecoratedMessage("consumerTask2.get() = " + consumerTask2.get());
                } catch (InterruptedException ex) {
                    printDecoratedMessage("Interrupted : consumerTask2.get() = ");
                } catch (ExecutionException ex) {
                    //Logger.getLogger(BlockingMapTest.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        });

        printDecoratedMessage("waiting for producers to complete production");
        Thread.sleep(1200);

        printDecoratedMessage("clearing synchronizer :: This will interrupt any waiting consumer");
        blockingMap.clear();

        assertTrue(consumerTask1.get().equalsIgnoreCase("one")
                && (consumerTask2.get().equalsIgnoreCase(INTERRUPTED)));

    }

    private static final int FAIL_TIMEOUT = 2000;
    private volatile String t;

    /**
     * Figures out a finalization issue found for BlockingHashMap requiring a
     * complex implementation for
     * {@link AsyncDictionary#tryTake(String, int)}.<br>
     */
    @Test(timeout = 2 * FAIL_TIMEOUT + 1000)
    public final void testClearWithRunningTake() {
        final ExecutorService executor = Executors.newFixedThreadPool(2);
        final BlockingHashMap<String, String> q = new BlockingHashMap<String, String>();
        t = null;
        final int TIME_MS_BACKGROUND_THREAD_SLEEP_INTERVAL = 300;
        final CountDownLatch l1 = new CountDownLatch(1);
        final CountDownLatch l2 = new CountDownLatch(1);
        executor.execute(new Runnable() {
            @Override
            public void run() {
                l1.countDown();
                try {
                    t = q.take("some non existent key", Integer.MAX_VALUE, TimeUnit.MILLISECONDS);
                    fail("interrupted exception expected due to call to clear");
                } catch (InterruptedException e) {
                    // expected exception
                }
                l2.countDown();
            }
        });

        try {
            l1.await(FAIL_TIMEOUT, TimeUnit.MILLISECONDS); // wait for background tread was started
            Thread.sleep(TIME_MS_BACKGROUND_THREAD_SLEEP_INTERVAL); // give background thread some more time to ensure take is really waiting
            q.clear();
            l2.await(FAIL_TIMEOUT, TimeUnit.MILLISECONDS); // wait on background thread is finished
            assertNull(t);
        } catch (InterruptedException e) {
            fail();
        } finally {
            executor.shutdown();
        }
    }

    /**
     * Spawns two producers, two consumers & checks for consistent behavior of
     * blocking map.
     */
    @Test
    public final void testSimpleConcurrency() {

        //consumer threads
        Thread consumer1, consumer2;
        //producer threads
        Thread producer1, producer2;

        final int TIME_MS_BEFORE_PRODUCE_ELEMENT = 300;
        final CountDownLatch bothDone = new CountDownLatch(2);
        final AtomicInteger backgroundThreadExceptionsCounter = new AtomicInteger(0);

        final BlockingMap<Integer, String> blockingMap = new BlockingHashMap<Integer, String>();

        //first consumer thread
        consumer1 = new Thread("consumer-1") {

            public void run() {
                try {
                    blockingMap.take(new Integer(1));
                    bothDone.countDown();
                } catch (InterruptedException e) {
                    backgroundThreadExceptionsCounter.incrementAndGet();
                    e.printStackTrace();
                }
            }
        };

        //second consumer thread
        consumer2 = new Thread("consumer-2") {

            public void run() {
                try {
                    blockingMap.take(new Integer(2));
                    bothDone.countDown();
                } catch (InterruptedException e) {
                    backgroundThreadExceptionsCounter.incrementAndGet();
                    e.printStackTrace();
                }
            }
        };

        //producer thread 1
        producer1 = new Thread("producer-1") {

            public void run() {
                try {
                    Thread.sleep(TIME_MS_BEFORE_PRODUCE_ELEMENT);
                    blockingMap.put(new Integer(2), "two");
                } catch (InterruptedException e) {
                    backgroundThreadExceptionsCounter.incrementAndGet();
                    e.printStackTrace();
                }
            }
        };

        //producer thread 1
        producer2 = new Thread("producer-2") {

            public void run() {
                try {
                    Thread.sleep(TIME_MS_BEFORE_PRODUCE_ELEMENT);
                    blockingMap.put(new Integer(1), "one");
                } catch (InterruptedException e) {
                    backgroundThreadExceptionsCounter.incrementAndGet();
                    e.printStackTrace();
                }
            }
        };

        consumer1.start();
        consumer2.start();
        producer1.start();
        producer2.start();

        try {
            assertTrue("not all expected elements put to map received within a given time period",
                    bothDone.await(3 * TIME_MS_BEFORE_PRODUCE_ELEMENT, TimeUnit.MILLISECONDS));
        } catch (InterruptedException e) {
            fail();
        }

        assertEquals("unexpected exception happeded in background thread", 0, backgroundThreadExceptionsCounter.get());
    }

    @Test
    public void testSuffisticatedConcurrency() throws Exception {
        final int numberOfConsumersProducers = 100;

        final Map<Integer, String> referenceMap;
        final BlockingMap<Integer, String> blockingMap;

        List<Callable<Map.Entry<Integer, String>>> consumers;
        List<Callable<String>> producers;
        final ExecutorService executor;

        final Queue<String> productionErrors = new ConcurrentLinkedQueue<String>();
        final Queue<Map.Entry<Integer, String>> comsumptionErrors = new ConcurrentLinkedQueue<Map.Entry<Integer, String>>();
        final Queue<Exception> catchedExceptions = new ConcurrentLinkedQueue<Exception>();

        // fill reference map
        referenceMap = new ConcurrentHashMap<Integer, String>();
        for (int i = 0; i < numberOfConsumersProducers; i++) {
            referenceMap.put(i, "Stringy " + i);
        }

        blockingMap = new BlockingHashMap<Integer, String>();
        executor = Executors.newCachedThreadPool();

        final CountDownLatch consumingFinished = new CountDownLatch(1);

        try {
            consumers = createConsumers(numberOfConsumersProducers, blockingMap, catchedExceptions);
            startConsuming(executor, comsumptionErrors, consumers, referenceMap, consumingFinished, catchedExceptions);

            producers = createProducers(numberOfConsumersProducers, blockingMap, referenceMap);
            startProducing(executor, productionErrors, producers, catchedExceptions);

            assertTrue("consuming timed out", consumingFinished.await(2, TimeUnit.SECONDS));
            blockingMap.clear();

            assertTrue(productionErrors.isEmpty());
            assertTrue(comsumptionErrors.isEmpty());
            if (catchedExceptions.size() > 0) {
                fail(catchedExceptions.peek().getMessage());
            }
        } finally {
            executor.shutdown();
        }
    }

    private List<Callable<Map.Entry<Integer, String>>> createConsumers(int numberOfConsumers, final BlockingMap<Integer, String> blockingMap, final Queue<Exception> catchedExceptions) {
        List<Callable<Map.Entry<Integer, String>>> consumers = new ArrayList<Callable<Map.Entry<Integer, String>>>();
        for (int i = 0; i < numberOfConsumers; i++) {
            final int key = i;
            consumers.add(new Callable<Map.Entry<Integer, String>>() {
                @Override
                public Map.Entry<Integer, String> call() {
                    Map.Entry<Integer, String> entry = null;
                    try {
                        final String valueString = blockingMap.take(key);
//                        System.out.println("blockingmap.take(" + key + ") = " + valueString);
                        entry = new Map.Entry<Integer, String>() {
                            final Integer entryKey = key;
                            final String value = valueString;

                            @Override
                            public Integer getKey() {
                                return entryKey;
                            }

                            @Override
                            public String getValue() {
                                return value;
                            }

                            @Override
                            public String setValue(String value) {
                                throw new UnsupportedOperationException("Not supported yet.");
                            }

                            @Override
                            public String toString() {
                                return ("key,value : " + entryKey + ":" + value);
                            }
                        };
                    } catch (InterruptedException ex) {
                        catchedExceptions.add(ex);
                    }
                    return entry;
                }
            });
        }
        return consumers;
    }

    private void startConsuming(
            final ExecutorService executor,
            final Queue<Map.Entry<Integer, String>> comsumptionErrors,
            final List<Callable<Map.Entry<Integer, String>>> consumers,
            final Map<Integer, String> referenceMap,
            final CountDownLatch consumingFinished,
            final Queue<Exception> catchedExceptions) {
        executor.submit(new FutureTask<Void>(new Runnable() {

            @Override
            public void run() {
                try {
                    List<Future<Map.Entry<Integer, String>>> products = executor.invokeAll(consumers);
                    for (Future<Map.Entry<Integer, String>> product : products) {
                        Map.Entry<Integer, String> returnedEntry = product.get();
                        if (!(returnedEntry.getValue().equals(referenceMap.get(returnedEntry.getKey())))) {
                            comsumptionErrors.add(returnedEntry);
                        }
                    }
                    consumingFinished.countDown();
                } catch (ExecutionException ex) {
                    catchedExceptions.add(ex);
                } catch (InterruptedException ex) {
                    catchedExceptions.add(ex);
                }
            }
        }, null));

    }

    private List<Callable<String>> createProducers(
            int numberOfProducers,
            final BlockingMap<Integer, String> blockingMap,
            final Map<Integer, String> referenceMap) {
        List<Callable<String>> producers = new ArrayList<Callable<String>>();
        for (int j = 0; j < numberOfProducers; j++) {
            final int key = j;
            producers.add(new Callable<String>() {
                @Override
                public String call() {
                    return blockingMap.put(key, referenceMap.get(key));
                }
            });
        }
        return producers;
    }

    private void startProducing(
            final ExecutorService executor,
            final Queue<String> productionErrors,
            final List<Callable<String>> producers,
            final Queue<Exception> catchedExceptions) {
        executor.submit(new FutureTask<String>(new Callable<String>() {

            @Override
            public String call() throws Exception {

                try {
                    List<Future<String>> productionAcks = executor.invokeAll(producers);
                    for (Future<String> productionAck : productionAcks) {
                        String ack = productionAck.get();
                        if (ack != null) {
                            productionErrors.add(ack);
                        }
                    }
                } catch (ExecutionException | InterruptedException ex) {
                    catchedExceptions.add(ex);
                }
                return null;
            }
        }));
    }

    @Test
    public final void testIsKeyAvailable() {
        BlockingMap<String, String> blockingMap = new BlockingHashMap<String, String>();
        blockingMap.put("key", "value");

        assertFalse(blockingMap.isKeyAvailable("non-existing key"));
        assertTrue(blockingMap.isKeyAvailable("key"));
        assertTrue(blockingMap.isKeyAvailable("key"));
    }

    @Test
    public final void testGetObject() {
        BlockingMap<String, String> blockingMap = new BlockingHashMap<String, String>();
        blockingMap.put("key", "value");

        assertNull(blockingMap.get("non-existing key"));
        assertNotNull(blockingMap.get("key"));
        assertEquals("value", blockingMap.get("key"));
        assertEquals("value", blockingMap.get("key"));
    }

    @Test
    public final void testPutKV() {
        BlockingMap<String, String> blockingMap = new BlockingHashMap<String, String>();
        blockingMap.put("key", "value");

        assertNotNull(blockingMap.get("key"));
        assertEquals("value", blockingMap.get("key"));

        blockingMap.put("key", "other value");
        assertEquals("value", blockingMap.get("key"));
    }

    @Test
    public final void testRemoveObject() {
        BlockingMap<String, String> blockingMap = new BlockingHashMap<String, String>();
        blockingMap.put("key", "value");

        assertNotNull(blockingMap.get("key"));
        assertEquals("value", blockingMap.get("key"));

        //remove is expected to return currently mapped value. Hence, the assertion
        assertEquals("value", blockingMap.remove("key"));
        assertNull(blockingMap.get("key"));
    }

    @Test
    public final void testTakeK() {
        BlockingMap<String, String> blockingMap = new BlockingHashMap<String, String>();
        blockingMap.put("key", "value");

        String gotValue;
        try {
            gotValue = blockingMap.take("key");
            assertNotNull(gotValue);
            assertEquals("value", gotValue);
            assertFalse(blockingMap.isKeyAvailable("key"));
            assertTrue(blockingMap.isEmpty());
        } catch (InterruptedException e) {
            fail("unexpected interrruption on take");
        }
    }

    @Test
    public final void testTakeKLongTimeUnit() {
        BlockingMap<String, String> blockingMap = new BlockingHashMap<String, String>();
        blockingMap.put("key", "value");

        String gotValue;
        try {
            gotValue = blockingMap.take("key", 1, TimeUnit.SECONDS);
            assertNotNull(gotValue);
            assertEquals("value", gotValue);
            assertFalse(blockingMap.isKeyAvailable("key"));
            assertTrue(blockingMap.isEmpty());
        } catch (InterruptedException e) {
            fail("unexpected interrruption on take");
        }
    }

    @Test
    public final void testTakeKLongTimeUnit_TimingOUt() {
        final int WAIT_TIME = 300;
        BlockingMap<String, String> blockingMap = new BlockingHashMap<String, String>();

        String gotValue;
        try {
            long startTime = System.currentTimeMillis();
            gotValue = blockingMap.take("non-existing key", WAIT_TIME, TimeUnit.MILLISECONDS);
            long duration = System.currentTimeMillis() - startTime;
            assertTrue("taken duration not within expected timeout time", duration <= WAIT_TIME + (WAIT_TIME / 10) && duration >= WAIT_TIME); // 10% errro is OK
            assertNull(gotValue);
            assertTrue(blockingMap.isEmpty());
        } catch (InterruptedException e) {
            fail("unexpected interrruption on take");
        }
    }

    /**
     * Tests behavior while multiple threads wait on non-existent key.
     *
     * If take(k) on a key that does not exist is invoked from multiple threads,
     * all the thread should block till the key becomes available. When the key
     * becomes available, all the blocked threads should be notified. Actual
     * removal of the mapping can & should happen from only one of the threads.
     * Therefore, the operation should be successful from only one thread.
     * Hence, value should be returned on only one of the threads and not on all
     * the threads. Null should be returned on threads on which the operation
     * was not successful.
     *
     * Following pull request discusses this in detail:
     *
     * Pull request #4 https://github.com/sarveswaran-m/blockingMap4j/pull/4
     */
    @Test
    public void testTakeOnUnAvailableKey() throws InterruptedException, ExecutionException {
        /**
         * log messages introduced to understand thread interaction. should be
         * removed.
         *
         * Assertion for marker value on blocked consumer or expecting
         * Interrupted exception would suffice
         */
        printDecoratedMessage("Starting consumers");
        final int KEY_TO_WAIT_ON = 1;
        final int TIME_MS_PRODUCER_SLEEP_INTERVAL = 300;

        consumerTask1 = new FutureTask<String>(new Callable<String>() {
            @Override
            public String call() {
                String value = null;
                try {
                    value = blockingMap.take(KEY_TO_WAIT_ON);
                } catch (InterruptedException ex) {
                    printDecoratedMessage("Exception on consumer 1");
                    value = INTERRUPTED;
                }
                return value;
            }
        });

        //second consumer thread
        consumerTask2 = new FutureTask<String>(new Callable<String>() {
            @Override
            public String call() {
                String value = null;
                try {
                    value = blockingMap.take(KEY_TO_WAIT_ON);
                } catch (InterruptedException ex) {
                    printDecoratedMessage("Exception on consumer 2");
                    value = INTERRUPTED;
                }
                return value;
            }
        });

        //producer thread 1
        producer1 = new Callable<String>() {
            @Override
            public String call() throws Exception {
                String value = null;
                try {
                    Thread.sleep(TIME_MS_PRODUCER_SLEEP_INTERVAL);
                    value = blockingMap.put(KEY_TO_WAIT_ON, "one");
                } catch (InterruptedException e) {
                    //interrupted
                }
                return value;
            }
        };

        printDecoratedMessage("Starting Consumers");
        executor.submit(consumerTask1);
        executor.submit(consumerTask2);

        printDecoratedMessage("Starting Producer");
        executor.submit(producer1);

        String[] returns = {consumerTask1.get(), consumerTask2.get()};
        String[] expectedReturns = {"one", null};

        assertTrue(Arrays.asList(returns).containsAll(Arrays.asList(expectedReturns)));
    }

}
