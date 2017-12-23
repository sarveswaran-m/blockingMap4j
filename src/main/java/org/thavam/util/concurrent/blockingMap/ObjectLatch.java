/*
 * ObjectLatch.java
 *
 * Created on August 8, 2008, 12:44 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */
package org.thavam.util.concurrent.blockingMap;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * <H1>A Blocking Object Latch</H1> This class implements a blocking object
 * latch, that acts as a synchronizer between a producer of an object and it's
 * consumer(s). <p> An object is set with
 * <code>set()</code>only ONCE. Further attempts to set the object are just
 * ignored.<br> Consumers request the object with
 * <code>get()</code>. If the object is not already set, consumers are blocked
 * waiting until the object is available or until an interrupt
 * (InteruptedException) terminates the wait. The map can be tested for object
 * availability with isAvailable(), which answers true if the object has already
 * been set. <br>
 *
 * @author Sarveswaran M
 * @version 1.1 08/12/08 based on the implementation by Alfred Peisl
 */
class ObjectLatch<R> {

    /**
     * The object.
     */
    /*
     * ==> object is set and got on different threads should be volatile,to get
     * rid of caching issues
     */
    private volatile R object = null;
    /**
     * The latch counter created and set to 1.
     */
    private final CountDownLatch latch = new CountDownLatch(1);

    /*
     * isAvailable() & set(R object)form a critical section lock required to
     * co-ordinate these sections
     */
    private final ReadWriteLock setLock = new ReentrantReadWriteLock();

    /**
     * Checks if the object is already available (has been already set).
     *
     * @return true, if the object is already available (has been already set)
     */
    public boolean isAvailable() {
        setLock.readLock().lock();
        try {
            return latch.getCount() == 0;
        } finally {
            setLock.readLock().unlock();
        }
    }

    /**
     * Sets the object if it is not already set. Otherwise ignore this request.
     * A product produced by producer cannot be simply over-ridden by a another
     * product from the same or different consumer. It should be consumed by a
     * consumer before another product with the same key can be put on the map
     *
     * @return  null if mapping object set on latch, else returns the existing object in latch
     * @param object the object
     */
    public R set(R object) {
        //version 1
//        setLock.writeLock().lock();
//        try {
//            if (!isAvailable()) {
//                this.object = object;
//                latch.countDown();
//            }
//        } finally {
//            setLock.writeLock().unlock();
//        }
        
        //version 2
        setLock.writeLock().lock();
        try {
            if (!isAvailable()) {
                this.object = object;
                latch.countDown();
                return null;
            } else{
                return this.object;
            }
        } finally {
            setLock.writeLock().unlock();
        }
    }

    /**
     * Get the object if it is already available (has already been set). <p> If
     * it is not available, this method returns immediately with null
     *
     * @return the object if it is already available (has already been set)
     *
     * @throws InterruptedException
     */
    public R getImmediately() throws InterruptedException {
        boolean available = latch.await(Integer.MIN_VALUE,TimeUnit.NANOSECONDS);
        //not part of any invariant
        //no need to lock/synchronize
        return (available ? object : null);
    }

    /**
     * Get the object if it is already available (has already been set). <p> If
     * it is not available, wait until it is or until an interrupt
     * (InterruptedException) terminates the wait.
     *
     * @return the object if it is already available (has already been set)
     *
     * @throws InterruptedException
     */
    public R get() throws InterruptedException {
        latch.await(Integer.MAX_VALUE,TimeUnit.DAYS);
        //not part of any invariant
        //no need to lock/synchronize
        return object;
    }

    /**
     * Get the object if it is already available (has already been set).
     * <p>Causes the current thread to wait until the latch has counted down to
     * zero, unless the thread is interrupted, or the specified waiting time
     * elapses.
     *
     * @return the object if it is already available (has already been set)
     *
     * @throws InterruptedException
     */
    public R get(long time, TimeUnit unit) throws InterruptedException {
        latch.await(time,unit);
        //not part of any invariant
        //no need to lock/synchronize
        return object;
    }
}
