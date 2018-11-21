package org.oucho.mpdclient.tools;

import java.io.Serializable;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;


public class MultiMap<K, V> implements Serializable {

    private static final long serialVersionUID = 6716321360137860110L;

    private final Map<K, List<V>> mInternalMap;

    public MultiMap() {
        super();
        mInternalMap = new HashMap<>();
    }

    public boolean containsKey(final K key) {
        return mInternalMap.containsKey(key);
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null) {
            return false;
        }
        if (getClass() != o.getClass()) {
            return false;
        }
        final MultiMap<?, ?> other = (MultiMap<?, ?>) o;
        return mInternalMap.equals(other.mInternalMap);
    }

    public List<V> get(final K key) {
        return mInternalMap.get(key);
    }


    @Override
    public int hashCode() {
        return mInternalMap.hashCode();
    }


    public void put(final K key, final V value) {
        List<V> valueList = mInternalMap.get(key);
        if (valueList == null) {
            valueList = new LinkedList<>();
            mInternalMap.put(key, valueList);
        }
        valueList.add(value);
    }

    public void remove(final K key) {
        mInternalMap.remove(key);
    }



}
