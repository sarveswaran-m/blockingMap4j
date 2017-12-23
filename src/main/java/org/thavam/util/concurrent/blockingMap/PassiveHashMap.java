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
class PassiveHashMap implements BlockingMap {

    //prevent instantiation by any other class
    private PassiveHashMap() {
    }

    @Override
    public boolean isKeyAvailable(Object key) {
        throw new IllegalStateException("Map Shutdown.Not Active");
    }

    @Override
    public Object get(Object key) {
        throw new IllegalStateException("Map Shutdown.Not Active");
    }

    @Override
    public Object put(Object key, Object value) {
        throw new IllegalStateException("Map Shutdown.Not Active");
    }

    @Override
    public Object remove(Object key) {
        throw new IllegalStateException("Map Shutdown.Not Active");
    }

    @Override
    public Object offer(Object key, Object value) throws InterruptedException {
        throw new IllegalStateException("Map Shutdown.Not Active");
    }

    @Override
    public Object take(Object key) throws InterruptedException {
        throw new IllegalStateException("Map Shutdown.Not Active");
    }

    @Override
    public Object offer(Object key, Object value, long timeout, TimeUnit unit) {
        throw new IllegalStateException("Map Shutdown.Not Active");
    }

    @Override
    public Object take(Object key, long timeout, TimeUnit unit) {
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
    public Set keySet() {
        throw new IllegalStateException("Map Shutdown.Not Active");
    }

    @Override
    public Collection values() {
        throw new IllegalStateException("Map Shutdown.Not Active");
    }

    @Override
    public Set entrySet() {
        throw new IllegalStateException("Map Shutdown.Not Active");
    }

    @Override
    public void putAll(Map m) {
        throw new UnsupportedOperationException("Map Shutdown.Not Active");
    }

    //singleton implenentation
    //static nested class - holder for singleton instance
    private static class SingletonHolder {

        static PassiveHashMap instance = new PassiveHashMap();
    }

    public static PassiveHashMap getInstance() {
        return SingletonHolder.instance;
    }
}
