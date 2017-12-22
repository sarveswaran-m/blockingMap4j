/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thavam.util.concurrent;

import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * <H1>A Blocking Map</H1> Blocking map is a Map that additionally supports
 * operations that wait for a key to be available when retrieving an element.
 * Blocking map acts as a synchronizer between producers and consumers.
 *
 * <p> BlockingMap methods come in three forms, with different ways of handling
 * operations that cannot be satisfied immediately, but may be satisfied at some
 * point in the future: one returns a special value, the second blocks the
 * current thread indefinitely until the operation can succeed, and the third
 * blocks for only a given maximum time limit before giving up. These methods
 * are summarized in the following table:
 *
 * <p> <table BORDER CELLPADDING=3 CELLSPACING=1> <tr> <td></td> <td
 * ALIGN=CENTER><em>Special value</em></td> <td
 * ALIGN=CENTER><em>Blocks</em></td> <td ALIGN=CENTER><em>Times out</em></td>
 * </tr> <tr> <td><b>Insert</b></td> <td>{@link #put put(key, value)}</td> <td>{@link #offer offer(key, value)}</td> <td>{@link #offer(Object, Object, long, TimeUnit) offer(key, value, time, unit)}</td>
 * </tr> <tr> <td><b>Remove</b></td> <td>{@link #remove remove(key)}</td> <td>{@link #take take(key)}</td> <td>{@link #take(Object, long, TimeUnit) take(key, time, unit)}</td>
 * </tr> <tr> <td><b>Examine</b></td> <td>{@link #get get()}</td> <td><em>not
 * applicable</em></td> <td><em>not applicable</em></td> </tr> </table>
 *
 * <p> A BlockingMap does not accept null elements. Implementations throw
 * NullPointerException on attempts to put or offer a null. A null is used as a
 * sentinel value to indicate failure of get & take operations.
 *
 * <p> A BlockingMap may be capacity bounded. At any given time it may have a
 * remainingCapacity beyond which no additional elements can be put without
 * blocking. A BlockingQueue without any intrinsic capacity constraints always
 * reports a remaining capacity of Integer.MAX_VALUE.
 *
 * <p> BlockingMap implementations are designed to be used primarily for
 * producer-consumer queues, but additionally support the Collection interface.
 * So, for example, it is possible to put a group of key-value pairs to a map
 * using putAll(map). However, such operations are in general not performed very
 * efficiently, and are intended for only occasional use.
 *
 * <p> BlockingMap implementations are thread-safe. All queuing methods achieve
 * their effects atomically using internal locks or other forms of concurrency
 * control. However, the bulk Collection operations addAll, containsAll,
 * retainAll and removeAll are not necessarily performed atomically unless
 * specified otherwise in an implementation. So it is possible, for example, for
 * putAll(map) to fail (throwing an exception) after adding only some of the
 * elements in map.
 *
 * <p> A BlockingMap does not intrinsically support any kind of "close" or
 * "shutdown" operation to indicate that no more items will be added. The needs
 * and usage of such features tend to be implementation-dependent. For example,
 * a common tactic is for producers to insert special end-of-stream or poison
 * objects, that are interpreted accordingly when taken by consumers.
 *
 * <p> BlockingMap implementations generally do not define element-based
 * versions of methods equals and hashCode but instead inherit the identity
 * based versions from class Object, because element-based equality is not
 * always well-defined for queues with the same elements but different ordering
 * properties.
 *
 *
 * <p> Producers cannot
 * <code>put(key, value)/offer(key, value)</code> on a key that is already
 * available on the map. Attempts to put a mapping whose key is already
 * available on the map are ignored. However, the same mapping can be put in to
 * the map after it is taken by consumer(s)
 *
 *
 * Usage example, based on a typical producer-consumer scenario. Note that a
 * BlockingMap can safely be used with multiple producers and multiple
 * consumers.
 *
 * @param <K> the type of keys maintained by this map
 * @param <V> the type of mapped values
 * @author Sarveswaran M
 * @version 1.1, Semantics & contracts for a blocking map defined
 */
public interface BlockingMap<K, V> extends Map<K, V> {

    /**
     * Returns <tt>true</tt> if this map contains a mapping for the specified
     * key.
     *
     * @param key key whose presence in this map is to be tested
     * @return <tt>true</tt> if this map contains a mapping for the specified
     * key
     * @throws ClassCastException if the key is of an inappropriate type for
     * this map (optional)
     * @throws NullPointerException if the specified key is null
     */
    public boolean isKeyAvailable(K key);

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
     * this map (optional)
     * @throws NullPointerException if the specified key is null and this map
     * does not permit null keys (optional)
     */
    @Override
    V get(Object key);

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
    V put(K key, V value);

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
     */
    @Override
    V remove(Object key);

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
    V offer(K key, V value) throws InterruptedException;

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
     */
    V take(Object key) throws InterruptedException;

    /**
     * Associates the specified value with the specified key in this map. If the
     * map previously contained a mapping for the key, the old value is replaced
     * by the specified value.
     *
     * <p> If the Map is bounded and there is no space to put the new mapping,
     * this method blocks till space becomes available or the specified time
     * elapses. offer on an unbound map will always succeed
     *
     * <p> Producers cannot offer a mapping on a key that is already available
     * on the map. Attempts to such a mapping are ignored. However, the same
     * mapping can be successfully offered after the existing mapping is taken
     * by consumer(s)
     *
     *
     * @param key key with which the specified value is to be associated
     * @param value value to be associated with the specified key
     * @param timeout how long to wait before giving up, in units of
     * <tt>unit</tt>
     * @param unit a <tt>TimeUnit</tt> determining how to interpret the
     * <tt>timeout</tt> parameter
     * @return the previous value associated with <tt>key</tt>, or <tt>null</tt>
     * if there was no mapping for <tt>key</tt>. (A <tt>null</tt> return can
     * also indicate a time out
     *
     * @throws InterruptedException if interrupted while waiting
     * @throws ClassCastException if the class of the specified element prevents
     * it from being added to this queue
     * @throws NullPointerException if the specified element is null
     * @throws IllegalArgumentException if some property of the specified
     * element prevents it from being added to this queue
     */
    V offer(K key, V value, long timeout, TimeUnit unit) throws InterruptedException;

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
     */
    V take(Object key, long timeout, TimeUnit unit) throws InterruptedException;
}