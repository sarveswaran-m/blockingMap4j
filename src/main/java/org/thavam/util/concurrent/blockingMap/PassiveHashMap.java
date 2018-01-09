/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thavam.util.concurrent.blockingMap;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * This class represents the passive or cleared state BlockingHashMap. Throws
 * <tt>IllegalStateException</tt> on all operations. All implementations of
 * BlockingHashMap shall use one instance of <tt>PassiveHashMap<tt>. Hence, this
 * has to be a singleton.
 *
 * This class is <i>immutable</i>
 * 
 * Type information not used since this is a singleton that used as proxy for maps
 * holding different types 
 * @author Sarveswaran M
 *
 * @version 1.1 15/04/12
 */
/**
 * Should be accessible outside the package Meant to be used from
 * BlockingHashMap
 */
class PassiveHashMap<K,V> implements BlockingMap<K,V> {

    //prevent instantiation by any other class
    private PassiveHashMap() {
    }

    @Override
    public boolean isKeyAvailable(Object key) {
        throw new IllegalStateException("Map Shutdown.Not Active");
    }

    @Override
    public V get(Object key) {
        throw new IllegalStateException("Map Shutdown.Not Active");
    }

    @Override
    public V put(K key, V value) {
        throw new IllegalStateException("Map Shutdown.Not Active");
    }

    @Override
    public V remove(Object key) {
        throw new IllegalStateException("Map Shutdown.Not Active");
    }

    @Override
    public V offer(Object key, Object value) throws InterruptedException {
        throw new IllegalStateException("Map Shutdown.Not Active");
    }

    @Override
    public V take(Object key) throws InterruptedException {
        throw new IllegalStateException("Map Shutdown.Not Active");
    }

    @Override
    public V offer(Object key, Object value, long timeout, TimeUnit unit) {
        throw new IllegalStateException("Map Shutdown.Not Active");
    }

    @Override
    public V take(Object key, long timeout, TimeUnit unit) {
        throw new IllegalStateException("Map Shutdown.Not Active");
    }

    @Override
    public int size() {
        throw new IllegalStateException("Map Shutdown.Not Active");
    }

    @Override
    public boolean isEmpty() {
        throw new IllegalStateException("Map Shutdown.Not Active");
    }

    @Override
    public boolean containsKey(Object key) {
        throw new IllegalStateException("Map Shutdown.Not Active");
    }

    @Override
    public boolean containsValue(Object value) {
        throw new IllegalStateException("Map Shutdown.Not Active");
    }

//    @Override
//    public void putAll(Map<? extends K, ? extends V> m) {
//        throw new IllegalStateException("Map Shutdown.Not Active");
//    }
    @Override
    public void clear() {
        throw new IllegalStateException("Map Shutdown.Not Active");
    }

    @Override
    public Set<K> keySet() {
        throw new IllegalStateException("Map Shutdown.Not Active");
    }

    @Override
    public Collection<V> values() {
        throw new IllegalStateException("Map Shutdown.Not Active");
    }

    @Override
    public Set<Entry<K, V>> entrySet() {
        throw new IllegalStateException("Map Shutdown.Not Active");
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> m) {
        throw new UnsupportedOperationException("Map Shutdown.Not Active");
    }
    
    @SuppressWarnings("rawtypes")
    private static class SingletonHolder{
        private static volatile PassiveHashMap singletonInstance = new PassiveHashMap();
    }
	
    
    @SuppressWarnings("unchecked")
	public static <K,V> PassiveHashMap<K,V> getInstance() {
        return SingletonHolder.singletonInstance;
    }
}
