/*
 * BlockingHashMap.java
 *
 * Created on August 8, 2008, 12:41 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */
package org.thavam.util.concurrent.blockingMap;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * An unbound {@linkplain BlockingMap blocking map} backed by a hashmap that is
 * concurrent. This map offers no guarantee on the order of retrieval.
 *
 * <p>This is similar to unbounded buffer in which the synchronizer
 * elements inserted by producers and extracted by consumers. The only twist is
 * that each product has a key & consumers know which product they are
 * interested in. Attempts to <tt>put/offer</tt> an element into the map will
 * always succeed because this is an unbound map; attempts to <tt>take</tt>
 * element corresponding to a key that is not available on the map will block.
 *
 *
 * <p> This map can be shutdown using <tt>clear</tt>. All consumers blocked on
 * the map while invoking clear will be throw <tt>InterruptedException</tt> or
 * return with <tt>null</tt>. Attempting any operation after shutdown will throw
 * <tt>IllegalStateException</tt>.
 *
 * <p>This class implements some of <em>optional</em> methods of the {@link Map}.
 *
 *
 *
 * @author Sarveswaran M
 *
 * @param <K> the type of keys maintained by this map
 * @param <V> the type of mapped values
 * @version 1.2, 13/04/12 Architecture revised. Conforms to Map interface. Major
 * enhancements
 * @version 1.1, 08/12/08 based on the implementation by Alfred Peisl
 */
public class BlockingHashMap<K, V> implements BlockingMap<K, V> {

    /**
     *
     * state pattern employed since map exhibits distintively different behavior
     * before and after clear/shutdown
     *
     */
    private volatile BlockingMap<K, V> state;
    /*
     *
     * flag to multiple clear()
     */
    private final AtomicBoolean cleared = new AtomicBoolean(false);

    public BlockingHashMap() {
        this.state = new ActiveBlockingHashMap<K, V>();
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
        return state.isKeyAvailable(key);
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
    @Override
    public boolean containsKey(Object key) {
        return state.containsKey(key);
    }

    /**
     * Returns the value to which the specified key is mapped, or {@code null}
     * if this map contains no mapping for the key.
     *
     * <p> Note that {@code null} is used as a special marker to indicate the
     * absence of the requested key
     *
     * @param key the key whose associated value is to be returned
     * @return the value to which the specified key is mapped, or
     *         {@code null} if this map contains no mapping for the key
     * @throws ClassCastException if the key is of an inappropriate type for
     * this map
     * @throws NullPointerException if the specified key is null and this map
     * does not permit null keys (optional)
     * @throws IllegalStateException if the map has been shut-down
     */
    @Override
    public V get(Object key) {
        return state.get(key);
    }

    /**
     *
     * Associates the specified value with the specified key in this map. If the
     * map previously contained a mapping for the key, the old value is replaced
     * by the specified value.
     *
     * <p> If the Map is bounded and there is no space to put the new mapping,
     * this method returns with <tt>null</tt>. put on an unbound map will always
     * succeed
     *
     * <p> Producers cannot put on a key that is already available on the map.
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
        return state.put(key, value);
    }

    /**
     * Removes the mapping for a key from this map if it is present.
     *
     * <p>Returns the value to which this map previously associated the key, or
     * <tt>null</tt> if the map contained no mapping for the key.
     *
     * <p>The map will not contain a mapping for the specified key once the call
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
    @Override
    public V remove(Object key) {
        return state.remove(key);
    }

    /**
     * Associates the specified value with the specified key in this map. If the
     * map previously contained a mapping for the key, the old value is replaced
     * by the specified value.
     *
     * <p> If the Map is bounded and there is no space to put the new mapping,
     * this method blocks till space becomes available. offer on an unbound map
     * will always succeed
     *
     * <p> Producers cannot offer a mapping on a key that is already available
     * on the map. Attempts to such a mapping are ignored. However, the same
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
    public V offer(K key, V value) throws InterruptedException {
        return state.offer(key, value);
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
    public V take(Object key) throws InterruptedException {
        return state.take(key);
    }

    /**
     * Associates the specified value with the specified key in this map. If the
     * map previously contained a mapping for the key, the old value is replaced
     * by the specified value.
     *
     * <p> If the Map is bounded and there is no space to put the new mapping,
     * this method blocks till space becomes available or the specified time
     * elapses. offer on an unbound map will always succeed
     *
     *
     * <p> Producers cannot offer a mapping on a key that is already available
     * on the map. Attempts to such a mapping are ignored. However, the same
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
    public V offer(K key, V value, long timeout, TimeUnit unit) throws InterruptedException {
        return state.offer(key, value, timeout, unit);
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
    public V take(Object key, long timeout, TimeUnit unit) throws InterruptedException {
        return state.take(key, timeout, unit);
    }

    /**
     * Shuts down this blocking map & removes all mappings from this map.The map 
     * will be empty after this call.
     *
     * <p> Interrupts any threads waiting on any key in map before clearing.
     * This is done to prevent threads being blocked forever
     *
     * @throws IllegalStateException if the map has been shut-down
     */
    @Override
    public void clear() {
        //clear the map only if it has not been cleared yet
        if (!cleared.getAndSet(true)) {
            BlockingMap<K, V> oldState = state;
            state = PassiveHashMap.getInstance();
            oldState.clear();
        } else {
            state.clear();
        }
    }

    /**
     * Returns true if this map maps one or more keys to the specified value.
     *
     * @param value value whose presence in this map is to be tested
     * @return true if this map maps one or more keys to the specified value
     * @throws IllegalStateException if the map has been shut-down
     * @throws UnsupportedOperationException if map is in passive state
     */
    @Override
    public boolean containsValue(Object value) {
        return state.containsValue(value);
    }

    /**
     * Returns true if this map contains no key-value mappings.
     *
     * @return true if this map contains no key-value mappings
     *
     */
    @Override
    public boolean isEmpty() {
        return state.isEmpty();
    }

    /**
     * Returns the number of key-value mappings in this map
     *
     * @return the number of key-value mappings in this map
     * @throws IllegalStateException if the map has been shut-down
     */
    @Override
    public int size() {
        return state.size();
    }

    /**
     * Not supported Semantics of addition/removal to map outside the
     * producer/consumer methods not defined
     *
     * @throws IllegalStateException if the map has been shut-down
     * @throws UnsupportedOperationException if map is in passive state
     */
    @Override
    public Set<Map.Entry<K, V>> entrySet() {
        return state.entrySet();
    }

    /**
     * Not supported Semantics of addition/removal to map outside the
     * producer/consumer methods not defined
     *
     * @throws IllegalStateException if the map has been shut-down
     * @throws UnsupportedOperationException if map is in passive state
     */
    @Override
    public Set<K> keySet() {
        return state.keySet();
    }

    /**
     * Not supported Semantics of addition/removal to map outside the
     * producer/consumer methods not defined
     *
     * @throws IllegalStateException if the map has been shut-down
     * @throws UnsupportedOperationException if map is in passive state
     */
    @Override
    public Collection<V> values() {
        return state.values();
    }

    /**
     * To be supported
     *
     * @throws UnsupportedOperationException if map is in passive state
     * @throws IllegalStateException if the map has been shut-down
     */
    @Override
    public void putAll(Map<? extends K, ? extends V> m) {
        state.putAll(m);
    }
}
