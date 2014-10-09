 /*
 * Copyright (c) 2001, Zoltan Farkas All Rights Reserved.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.spf4j.concurrent;

import com.google.common.cache.CacheLoader;
import com.google.common.cache.CacheStats;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import javax.annotation.ParametersAreNonnullByDefault;
import org.spf4j.base.MemorizedCallable;

/**
 * 
 * @author zoly
 */
@ParametersAreNonnullByDefault
public final class UnboundedLoadingCache<K, V> implements LoadingCache<K, V> {

    private final ConcurrentMap<K, MemorizedCallable<? extends V>> map;

    private final CacheLoader<K, V> loader;

    public UnboundedLoadingCache(final int initialSize, final CacheLoader<K, V> loader) {
        this.map = new ConcurrentHashMap<>(
                initialSize, 0.75f, getDefaultConcurrency());
        this.loader = loader;
    }

    public static int getDefaultConcurrency() {
        int defConcurrency = org.spf4j.base.Runtime.NR_PROCESSORS;
        if (defConcurrency > 16) {
            defConcurrency = 16;
        }
        return defConcurrency;
    }

    @Override
    public V get(final K key) throws ExecutionException {
        MemorizedCallable<? extends V> existingValHolder = map.get(key);
        if (existingValHolder == null) {
            MemorizedCallable<? extends V> newHolder = new MemorizedCallable(new Callable<V>() {
                @Override
                public V call() throws Exception {
                    return loader.load(key);
                }
            });
            existingValHolder = map.putIfAbsent(key, newHolder);
            if (existingValHolder == null) {
                existingValHolder = newHolder;
            }
        }
        try {
            return existingValHolder.call();
        } catch (Exception ex) {
            throw new ExecutionException(ex);
        }

    }

    @Override
    public V getUnchecked(final K key) {
        try {
            return get(key);
        } catch (ExecutionException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public ImmutableMap<K, V> getAll(final Iterable<? extends K> keys) throws ExecutionException {
        ImmutableMap.Builder<K, V> builder = ImmutableMap.builder();
        for (K key : keys) {
            builder.put(key, get(key));
        }
        return builder.build();
    }

    @Override
    public V apply(final K key) {
        if (key == null) {
            throw new IllegalArgumentException("key cannot be null for " + this);
        } else {
            return getUnchecked(key);
        }
    }

    @Override
    public void refresh(final K key) {
        getUnchecked(key);
    }

    @Override
    public ConcurrentMap<K, V> asMap() {
        throw new UnsupportedOperationException();
    }

    @Override
    public V getIfPresent(final Object key) {
        MemorizedCallable<? extends V> existingValHolder = map.get(key);
        if (existingValHolder != null) {
            try {
                return existingValHolder.call();
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        } else {
            return null;
        }
    }

    @Override
    public V get(final K key, final Callable<? extends V> valueLoader) throws ExecutionException {
        MemorizedCallable<? extends V> existingValHolder = map.get(key);
        if (existingValHolder == null) {
            MemorizedCallable<? extends V> newHolder = new MemorizedCallable(valueLoader);
            existingValHolder = map.putIfAbsent(key, newHolder);
            if (existingValHolder == null) {
                existingValHolder = newHolder;
            }
        }
        try {
            return existingValHolder.call();
        } catch (Exception ex) {
            throw new ExecutionException(ex);
        }
    }

    @Override
    public ImmutableMap<K, V> getAllPresent(final Iterable<?> keys) {
        ImmutableMap.Builder<K, V> builder = ImmutableMap.builder();
        for (K key : (Iterable<K>) keys) {
            V val = getIfPresent(key);
            if (val != null) {
                builder.put(key, val);
            }
        }
        return builder.build();
    }

    @Override
    public void put(final K key, final V value) {
        map.put(key, new MemorizedCallable<V>(new Callable<V>() {

            @Override
            public V call() throws Exception {
                return value;
            }
        }));
    }

    @Override
    public void putAll(final Map<? extends K, ? extends V> m) {
        for (Map.Entry<? extends K, ? extends V> entry : m.entrySet()) {
            put(entry.getKey(), entry.getValue());
        }
    }

    @Override
    public void invalidate(final Object key) {
        map.remove(key);
    }

    @Override
    public void invalidateAll(final Iterable<?> keys) {
        for (K key : (Iterable<K>) keys) {
            invalidate(key);
        }
    }

    @Override
    public void invalidateAll() {
        map.clear();
    }

    @Override
    public long size() {
        return map.size();
    }

    @Override
    public CacheStats stats() {
        throw new UnsupportedOperationException("Not supported");
    }

    @Override
    public void cleanUp() {
        map.clear();
    }

}
