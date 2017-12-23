/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thavam.util.concurrent.blockingMapTester;

import org.thavam.util.concurrent.blockingMap.BlockingHashMap;
import org.thavam.util.concurrent.blockingMap.BlockingMap;

/**
 *
 * @author M8081120
 */
public class SimpleTester {

    //thread names
    public static final String CONSUMER_1 = "consumer-1";
    public static final String CONSUMER_2 = "consumer-2";
    public static final String PRODUCER_1 = "producer-1";
    public static final String PRODUCER_2 = "producer-2";
    //map containing conditions
    static volatile BlockingMap<Integer, String> blockingMap = new BlockingHashMap();
    //flag on which threads wait/notify
    boolean suspend = false;
    //self reference
    final SimpleTester blockingMapTester;
    //blocking thread
    static Thread consumer, consumer2;
    //notifying thread
    static Thread producer1, producer2;

    /**
     * Creates a new instance of blockingMapTester
     */
    public SimpleTester() {
        blockingMapTester = this;
    }

    static void print(String msg) {
        System.out.println(Thread.currentThread().getName() + " : " + msg);
    }

    public static void main(String ar[]) {
        //first consumer thread
        consumer = new Thread(CONSUMER_1) {

            public void run() {
                String value = null;
                try {
                    value = blockingMap.take(new Integer(1));
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                print("blockingMap.get(new Integer(1)) = " + value);
            }
        };

        //second consumer thread
        consumer2 = new Thread(CONSUMER_2) {

            public void run() {
                String value = null;
                try {
                    value = blockingMap.take(new Integer(2));
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                print("blockingMap.get(new Integer(2)) = " + value);
            }
        };

        //producer thread 1
        producer1 = new Thread(PRODUCER_1) {

            public void run() {
                try {
                    Thread.sleep(5000);
                    blockingMap.put(new Integer(2), "two");

//                    blockingMap.clear();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        };

        //producer thread 1
        producer2 = new Thread(PRODUCER_2) {

            public void run() {
                try {
                    Thread.sleep(5000);
                    blockingMap.put(new Integer(1), "one");
//                    blockingMap.clear();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        };

        //producer starts filling after a delay of 5s
        //by the time producer1 starts filling,consumers would already be waiting

        consumer.start();
        consumer2.start();
        producer1.start();
        producer2.start();

        try {
            Thread.sleep(20000);
        } catch (Exception e) {
            e.printStackTrace();
        }

        blockingMap.clear();
//        producer1.start();
        blockingMap.clear();
    }
}