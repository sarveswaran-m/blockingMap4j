/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thavam.util.concurrent.blockingMap;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This class represents a BlockingHashMap that is active & functional.
 * BlockingHashMap delegates all operations to this map
 *
 * Explicit read & write locks used to improve throughput read locks can
 * interleave but write locks or read & write locks cannot interleave write lock
 * on take prevents more than one consumer from taking at the same time It also
 * prevents any producer from offering when a take is in progress This will
 * degrade over-all thru put of the map
 *
 * Conflicts & ambiguities arise only when operations on the same key interleave
 * Therefore, there should be explicit lock for each key This approach will
 * yield maximum throughput This will be implemented in the next version
 *
 * @author Sarveswaran M
 * @param <K> the type of keys maintained by this map
 * @param <V> the type of mapped values
 *
 * @version 1.1 15/04/12
 */
/**
 * Should not be accessible outside the package. Meant to be used from
 * BlockingHashMap
 */
class ActiveBlockingHashMap<K, V> implements BlockingMap<K, V> {

    /**
     * Map containing the countdown latch.
     */
    private final ConcurrentMap<K, ObjectLatch<V>> primaryMap;
    /**
     * map containing blocked threads,necessary to interrupt all threads waiting
     * on keys in a cleared map
     */
    private final Map<Thread, ObjectLatch<V>> blockedThreadsMap;
    /**
     *
     * operations on isAvailable(), take(..), clear(), containsValue(),
     * isEmtpy() form critical sections that should not interleave. However,
     * mutative operations are performed in take(..) & clear() only. Hence,
     * explicit read & write locks can be used to improve thru put
     *
     */
    private final ReadWriteLock primaryMapLock = new ReentrantReadWriteLock();
    private final Lock primaryMapReadLock = primaryMapLock.readLock();
    private final Lock primaryMapWriteLock = primaryMapLock.writeLock();
    /*
     *
     * flag to prevent take while or after a clear has been triggered
     */
    private final AtomicBoolean cleared = new AtomicBoolean(false);

    //package-private accessor prevents instantiation by entities from other packages
    ActiveBlockingHashMap() {
        this.primaryMap = new ConcurrentHashMap<K, ObjectLatch<V>>();
        this.blockedThreadsMap = new ConcurrentHashMap<Thread, ObjectLatch<V>>();
    }

    /**
     * Returns <tt>true</tt> if this map contains a mapping for the specified
     * key.
     *
     * @param key key whose presence in this map is to be tested
     * @return <tt>true</tt> if this map contains a mapping for the specified
     * key
     * @throws ClassCastException if the key is of an inappropriate type for
     * this map
     * @throws NullPointerException if the specified key is null
     * @throws IllegalStateException if the map has been shut-down
     */
    @Override
    public boolean isKeyAvailable(K key) {
        primaryMapReadLock.lock();
        try {
            ObjectLatch<V> latch = primaryMap.get(key);
            return ((latch != null) && (latch.isAvailable()));
        } finally {
            primaryMapReadLock.unlock();
        }
    }

    /**
     * Returns <tt>true</tt> if this map contains a mapping for the specified
     * key.
     *
     * @param key key whose presence in this map is to be tested
     * @return <tt>true</tt> if this map contains a mapping for the specified
     * key
     * @throws ClassCastException if the key is of an inappropriate type for
     * this map (optional)
     * @throws NullPointerException if the specified key is null and this map
     * does not permit null keys (optional)
     * @throws IllegalStateException if the map has been shut-down
     */
    @SuppressWarnings("unchecked")
	@Override
    public boolean containsKey(Object key) {
        return isKeyAvailable((K) key);
    }

    /**
     * Returns the value to which the specified key is mapped, or {@code null}
     * if this map contains no mapping for the key.
     *
     * <p>
     * Note that {@code null} is used as a special marker to indicate the
     * absence of the requested key
     *
     * @param key the key whose associated value is to be returned
     * @return the value to which the specified key is mapped, or {@code null}
     * if this map contains no mapping for the key
     * @throws ClassCastException if the key is of an inappropriate type for
     * this map
     * @throws NullPointerException if the specified key is null and this map
     * does not permit null keys (optional)
     * @throws IllegalStateException if the map has been shut-down
     */
    @Override
    public V get(Object key) {
        V result = null;
        ObjectLatch<V> latch = primaryMap.get(key);
        if (latch != null) {
            try {
                //this will return immediately
                result = latch.getImmediately();
            } catch (InterruptedException ex) {
                Logger.getLogger(ActiveBlockingHashMap.class.getName()).log(Level.SEVERE, null, ex);
            }
        } else {
            //key not found on map
            //return immediately
        }

        return result;
    }

    /**
     *
     * Associates the specified value with the specified key in this map. If the
     * map previously contained a mapping for the key, the old value is replaced
     * by the specified value.
     *
     * <p>
     * If the Map is bounded and there is no space to put the new mapping, this
     * method returns with <tt>null</tt>. put on an unbound map will always
     * succeed
     *
     * <p>
     * Producers cannot put on a key that is already available on the map.
     * Attempts to put a mapping whose key is already available on the map are
     * ignored. However, the same mapping can be put in to the map after it is
     * taken by consumer(s)
     *
     *
     * @param key key with which the specified value is to be associated
     * @param value value to be associated with the specified key
     * @return the previous value associated with <tt>key</tt>, or <tt>null</tt>
     * if there was no mapping for <tt>key</tt>. (A <tt>null</tt> return can
     * also indicate that there is no space available on map)
     * @throws UnsupportedOperationException if the <tt>put</tt> operation is
     * not supported by this map
     * @throws ClassCastException if the class of the specified key or value
     * prevents it from being stored in this map
     * @throws NullPointerException if the specified key or value is null and
     * this map does not permit null keys or values
     * @throws IllegalArgumentException if some property of the specified key or
     * value prevents it from being stored in this map
     */
    @Override
    public V put(K key, V value) {
        ObjectLatch<V> latch = primaryMap.get(key);

        if (latch == null) {
            primaryMap.putIfAbsent(key, new ObjectLatch<V>());
            latch = primaryMap.get(key);
        }

        return latch.set(value);
    }

    /**
     * Removes the mapping for a key from this map if it is present.
     *
     * <p>
     * Returns the value to which this map previously associated the key, or
     * <tt>null</tt> if the map contained no mapping for the key.
     *
     * <p>
     * The map will not contain a mapping for the specified key once the call
     * returns.
     *
     * @param key key whose mapping is to be removed from the map
     * @return the previous value associated with <tt>key</tt>, or <tt>null</tt>
     * if there was no mapping for <tt>key</tt>.
     * @throws UnsupportedOperationException if the <tt>remove</tt> operation is
     * not supported by this map
     * @throws ClassCastException if the key is of an inappropriate type for
     * this map (optional)
     * @throws NullPointerException if the specified key is null and this map
     * does not permit null keys (optional)
     * @throws IllegalStateException if the map has been shut-down
     */
    @SuppressWarnings("unchecked")
	@Override
    public V remove(Object key) {
        V result = null;
        try {
            //since key is available, this will return immediately
            result = take((K)key, Integer.MIN_VALUE, TimeUnit.NANOSECONDS);
        } catch (InterruptedException ex) {
            Logger.getLogger(ActiveBlockingHashMap.class.getName()).log(Level.SEVERE, null, ex);
        }

        return result;
    }

    /**
     * Associates the specified value with the specified key in this map. If the
     * map previously contained a mapping for the key, the old value is replaced
     * by the specified value.
     *
     * <p>
     * If the Map is bounded and there is no space to put the new mapping, this
     * method blocks till space becomes available. offer on an unbound map will
     * always succeed
     *
     * <p>
     * Producers cannot offer a mapping on a key that is already available on
     * the map. Attempts to such a mapping are ignored. However, the same
     * mapping can be successfully offered after the existing mapping is taken
     * by consumer(s)
     *
     * @param key key with which the specified value is to be associated
     * @param value value to be associated with the specified key
     * @return the previous value associated with <tt>key</tt>, or <tt>null</tt>
     * if there was no mapping for <tt>key</tt>. (A <tt>null</tt> return can
     * also indicate that there is no space available on map)
     * @throws InterruptedException if interrupted while waiting
     * @throws ClassCastException if the class of the specified element prevents
     * it from being added to this queue
     * @throws NullPointerException if the specified element is null
     * @throws IllegalArgumentException if some property of the specified
     * element prevents it from being added to this queue
     */
    @Override
    public V offer(K key, V value) {
        return put(key, value);
    }

    /**
     * Retrieves and removes the mapping for a key from this map if it is
     * present, waiting if necessary until the mapping becomes available.
     *
     *
     * @param key key whose mapping is to be removed from the map
     * @return the previous value associated with <tt>key</tt>, or <tt>null</tt>
     * if there was no mapping for <tt>key</tt>.
     * @throws UnsupportedOperationException if the <tt>remove</tt> operation is
     * not supported by this map
     * @throws ClassCastException if the key is of an inappropriate type for
     * this map (optional)
     * @throws NullPointerException if the specified key is null and this map
     * does not permit null keys (optional)
     * @throws InterruptedException if interrupted while waiting
     * @throws IllegalStateException if the map has been shut-down
     */
    @Override
    public V take(K key) throws InterruptedException {
        return take(key, Integer.MAX_VALUE, TimeUnit.DAYS);
    }

    /**
     * Associates the specified value with the specified key in this map. If the
     * map previously contained a mapping for the key, the old value is replaced
     * by the specified value.
     *
     * <p>
     * If the Map is bounded and there is no space to put the new mapping, this
     * method blocks till space becomes available or the specified time elapses.
     * offer on an unbound map will always succeed
     *
     *
     * <p>
     * Producers cannot offer a mapping on a key that is already available on
     * the map. Attempts to such a mapping are ignored. However, the same
     * mapping can be successfully offered after the existing mapping is taken
     * by consumer(s)
     *
     * @param key key with which the specified value is to be associated
     * @param value value to be associated with the specified key
     * @param timeout how long to wait before giving up, in units of
     * <tt>unit</tt>
     * @param unit a <tt>TimeUnit</tt> determining how to interpret the
     * <tt>timeout</tt> parameter
     * @throws InterruptedException if interrupted while waiting
     * @throws ClassCastException if the class of the specified element prevents
     * it from being added to this queue
     * @throws NullPointerException if the specified element is null
     * @throws IllegalArgumentException if some property of the specified
     * element prevents it from being added to this queue
     * @throws IllegalStateException if the map has been shut-down
     */
    @Override
    public V offer(K key, V value, long timeout, TimeUnit unit) {
        return put(key, value);
    }

    /**
     * Retrieves and removes the mapping for a key from this map if it is
     * present, waiting if necessary until the mapping becomes available or the
     * specified time elapses. offer on an unbound map will always succeed.
     *
     *
     * @param key key with which the specified value is to be associated
     * @param timeout how long to wait before giving up, in units of
     * <tt>unit</tt>
     * @param unit a <tt>TimeUnit</tt> determining how to interpret the
     * @return the previous value associated with <tt>key</tt>, or <tt>null</tt>
     * if there was no mapping for <tt>key</tt> and the call times out.
     * @throws UnsupportedOperationException if the <tt>remove</tt> operation is
     * not supported by this map
     * @throws ClassCastException if the key is of an inappropriate type for
     * this map (optional)
     * @throws NullPointerException if the specified key is null and this map
     * does not permit null keys (optional)
     * @throws InterruptedException if interrupted while waiting
     * @throws IllegalStateException if the map has been shut-down
     */
	@Override
    public V take(K key, long timeout, TimeUnit unit) throws InterruptedException {
        V result = null;

        //prevent any consumer from getting in to a blocked stated on cleared map
        if (!cleared.get()) {
            /**
             * Bug Fix : For the bug raised @ https://sourceforge.net/p/blockingmapforj/discussion/874608/thread/646e5523/
             * Using one long write lock on primaryMap for entire operation
             * would have prevented any other thread from taking. Hence, write
             * lock is acquired once for put and once again for remove
             * 
             * 
             */
            primaryMapWriteLock.lock();
            ObjectLatch<V> latch = null;
            try {
                latch = primaryMap.get(key);

                if (latch == null) {
                    primaryMap.putIfAbsent((K) key, new ObjectLatch<V>());
                    latch = primaryMap.get((K) key);
                }
            } finally {
                primaryMapWriteLock.unlock();
            }
            //put thread in map before awaiting
            blockedThreadsMap.put(Thread.currentThread(), latch);
            result = latch.get(timeout, unit);
            //remove thread after awaiting
            blockedThreadsMap.remove(Thread.currentThread());

            primaryMapWriteLock.lock();
            try {
                result = ((primaryMap.remove((K) key) == null) ? null : result);
            } finally {
                primaryMapWriteLock.unlock();
            }

        }
        return result;
    }

    /**
     * Shuts down this blocking map & removes all mappings from this map. The
     * map will be empty after this call returns.
     *
     * <p>
     * Interrupts any threads waiting on any key in map before clearing. This is
     * done to prevent threads being blocked forever
     *
     * @throws IllegalStateException if the map has been shut-down
     */
    @Override
    public void clear() {
        //clear the map only if it has not been cleared yet
        if (!cleared.getAndSet(true)) {
            primaryMapWriteLock.lock();
            try {
                for (Thread thread : blockedThreadsMap.keySet()) {
                    thread.interrupt();
                }
                primaryMap.clear();
            } finally {
                primaryMapWriteLock.unlock();
            }
        }
    }

    /**
     * Returns true if this map maps one or more keys to the specified value.
     *
     * @param value value whose presence in this map is to be tested
     * @return true if this map maps one or more keys to the specified value
     * @throws IllegalStateException if the map has been shut-down
     * @throws UnsupportedOperationException
     */
    @Override
    public boolean containsValue(Object value) {
        primaryMapReadLock.lock();
        try {
            for (ObjectLatch<V> latch : primaryMap.values()) {
                //latch().get will not block since it is invoked only after 
                //checking for availability
                if ((latch.isAvailable())
                        && (latch.getImmediately() != null)
                        && (latch.getImmediately().equals(value))) {
                    return true;
                }
            }
        } catch (InterruptedException e) {
            //do nothing
            //program flow will never find this place
        } finally {
            primaryMapReadLock.unlock();
        }
        return false;
    }

    /**
     * Returns true if this map contains no key-value mappings.
     *
     * @return true if this map contains no key-value mappings
     *
     */
    @Override
    public boolean isEmpty() {
        primaryMapReadLock.lock();
        try {
            for (ObjectLatch<V> latch : primaryMap.values()) {
                if (latch.isAvailable()) {
                    return false;
                }
            }
        } finally {
            primaryMapReadLock.unlock();
        }
        return true;
    }

    /**
     * Returns the number of key-value mappings in this map
     *
     * @return the number of key-value mappings in this map
     * @throws IllegalStateException if the map has been shut-down
     */
    @Override
    public int size() {
        int size = 0;
        primaryMapReadLock.lock();
        try {
            for (ObjectLatch<V> latch : primaryMap.values()) {
                if (latch.isAvailable()) {
                    size++;
                }
            }
        } finally {
            primaryMapReadLock.unlock();
        }
        return size;
    }

    /**
     * Not supported Semantics of addition/removal to map outside the
     * producer/consumer methods not defined
     *
     * @throws IllegalStateException if the map has been shut-down
     * @throws UnsupportedOperationException
     */
    @Override
    public Set<Map.Entry<K, V>> entrySet() {
        throw new UnsupportedOperationException();
    }

    /**
     * Not supported Semantics of addition/removal to map outside the
     * producer/consumer methods not defined
     *
     * @throws IllegalStateException if the map has been shut-down
     * @throws UnsupportedOperationException
     */
    @Override
    public Set<K> keySet() {
        throw new UnsupportedOperationException();
    }

    /**
     * Not supported Semantics of addition/removal to map outside the
     * producer/consumer methods not defined
     *
     * @throws IllegalStateException if the map has been shut-down
     * @throws UnsupportedOperationException
     */
    @Override
    public Collection<V> values() {
        throw new UnsupportedOperationException();
    }

    /**
     * To be supported
     *
     * @throws UnsupportedOperationException
     * @throws IllegalStateException if the map has been shut-down
     */
    @Override
    public void putAll(Map<? extends K, ? extends V> m) {
        throw new UnsupportedOperationException();
    }
}
