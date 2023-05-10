package soot.jimple.infoflow.collections.test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class MapList<K, V> extends HashMap<K, List<V>> {
    public void add(K key, V value) {
        this.computeIfAbsent(key, k -> new ArrayList<>()).add(value);
    }

    public void remove(K key, int i) {
        this.computeIfAbsent(key, k -> new ArrayList<>()).remove(i);
    }

    public V get(K key, int i) {
        return this.computeIfAbsent(key, k -> new ArrayList<>()).get(i);
    }
}
