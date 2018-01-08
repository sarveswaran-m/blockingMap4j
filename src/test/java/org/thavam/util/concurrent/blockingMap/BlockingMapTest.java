/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thavam.util.concurrent.blockingMap;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
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
 * @author emeensa
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
     * Test of clear to interrupt blocked threads both consumers wait for their
     * keys of interest before producers start producing. This is ensured by
     * making producers sleep before production.
     *
     * consumer2 waits on non-existent key, there it will block for-ever on
     * clear, consumer2 should be interrupted
     *
     * Bug Id #1 
     * https://github.com/sarveswaran-m/blockingMap4j/issues/1
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

}
