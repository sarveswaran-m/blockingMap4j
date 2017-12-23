/*x
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thavam.util.concurrent.blockingMapTester;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.thavam.util.concurrent.blockingMap.BlockingHashMap;
import org.thavam.util.concurrent.blockingMap.BlockingMap;

/**
 *
 * @author Sarveswaran M
 */
public class BlockingMapTester {

    private final Map<Integer, String> referenceMap;
    private final BlockingMap<Integer, String> blockingMap;
    private final Queue<String> productionErrors
            = new ConcurrentLinkedQueue<String>();
    private final Queue<Map.Entry<Integer, String>> comsumptionErrors
            = new ConcurrentLinkedQueue<Map.Entry<Integer, String>>();
    private List<Callable<Map.Entry<Integer, String>>> consumers;
    private List<Callable<String>> producers;
    private final ExecutorService executor;
    //these attributes need to be used from inner classes
    //hence forced to be defined here
    int i = 0;
    int j = 0;

    public BlockingMapTester() {
        referenceMap = new ConcurrentHashMap<Integer, String>();
        for (i = 0; i < 100; i++) {
            referenceMap.put(i, "Stringy " + i);
        }
        blockingMap = new BlockingHashMap<Integer, String>();
        executor = Executors.newCachedThreadPool();
    }

    void createConsumers() {
        consumers = new ArrayList<Callable<Map.Entry<Integer, String>>>();
        for (i = 0; i < 100; i++) {

            consumers.add(new Callable<Map.Entry<Integer, String>>() {

                final int key = i;

                @Override
                public Map.Entry<Integer, String> call() {

                    Map.Entry<Integer, String> entry = null;
                    try {
//                        System.out.println("taking " + i);
                        final String valueString = blockingMap.take(key);
                        System.out.println("blockingmap.take(" + key + ") = " + valueString);
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
                        Logger.getLogger(BlockingMapTester.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    return entry;
                }
            });
        }
    }

    void startConsuming() {
        executor.submit(new FutureTask(new Callable<Object>() {

            @Override
            public Object call() throws Exception {
                try {
                    List<Future<Map.Entry<Integer, String>>> products = executor.invokeAll(consumers);
                    for (Future<Map.Entry<Integer, String>> product : products) {
                        Map.Entry<Integer, String> returnedEntry = product.get();
                        if (!(returnedEntry.getValue().equals(
                                referenceMap.get(returnedEntry.getKey())))) {
                            comsumptionErrors.add(returnedEntry);
                        }
                    }
                } catch (ExecutionException ex) {
                    Logger.getLogger(BlockingMapTester.class.getName()).log(Level.SEVERE, null, ex);
                } catch (InterruptedException ex) {
                    Logger.getLogger(BlockingMapTester.class.getName()).log(Level.SEVERE, null, ex);
                }
                return null;
            }
        }));

    }

    void createProducers() {
        producers = new ArrayList<Callable<String>>();
        for (j = 0; j < 100; j++) {

            producers.add(new Callable<String>() {

                final int key = j;

                @Override
                public String call() {
                    return blockingMap.put(key, referenceMap.get(key));
                }
            });
        }

    }

    void startProducing() {
        executor.submit(new FutureTask(new Callable<Object>() {

            @Override
            public Object call() throws Exception {

                try {
                    List<Future<String>> productionAcks = executor.invokeAll(producers);
                    for (Future<String> productionAck : productionAcks) {
                        String ack = productionAck.get();
                        if (ack != null) {
                            productionErrors.add(ack);
                        }
                    }
                } catch (ExecutionException ex) {
                    Logger.getLogger(BlockingMapTester.class.getName()).log(Level.SEVERE, null, ex);
                } catch (InterruptedException ex) {
                    Logger.getLogger(BlockingMapTester.class.getName()).log(Level.SEVERE, null, ex);
                }
                return null;
            }
        }));
    }

    void checkForConsumptionErrors() {
        Logger.getLogger(BlockingMapTester.class.getName()).log(Level.SEVERE,
                "errors found : {0}", comsumptionErrors);

    }

    void checkForProductionErrors() {
        Logger.getLogger(BlockingMapTester.class.getName()).log(Level.SEVERE,
                "errors found : {0}", productionErrors);
    }

    String getErrorDescription(Queue<Map.Entry<Integer, String>> error) {
        StringBuilder errorMsg = new StringBuilder();
        Map.Entry<Integer, String> errorEntry;
        while ((errorEntry = error.poll()) != null) {
            errorMsg.append(errorEntry.toString());
        }
        return errorMsg.toString();
    }

    void shutDownSynchronizer() {
        blockingMap.clear();
    }

    public static void main(String ar[]) throws InterruptedException {
        BlockingMapTester tester = new BlockingMapTester();
        tester.createConsumers();
        tester.startConsuming();
        tester.createProducers();
//        Thread.sleep(5000);

        tester.startProducing();
        Thread.sleep(10000);
        tester.checkForProductionErrors();
        tester.checkForConsumptionErrors();
        tester.shutDownSynchronizer();
        //tester.shutDownSynchronizer();
    }
}
