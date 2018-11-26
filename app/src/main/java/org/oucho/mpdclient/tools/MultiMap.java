/*
 * MPDclient - Music Player Daemon (MPD) remote for android
 * Copyright (C) 2017  Old-Geek
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

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
