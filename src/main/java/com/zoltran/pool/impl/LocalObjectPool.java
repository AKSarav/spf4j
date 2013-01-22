
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
package com.zoltran.pool.impl;

import com.zoltran.pool.ObjectBorower;
import com.zoltran.pool.ObjectCreationException;
import com.zoltran.pool.ObjectDisposeException;
import com.zoltran.pool.ObjectPool;
import com.zoltran.pool.ObjectReturnException;
import com.zoltran.pool.SmartObjectPool;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.ReentrantLock;

/**
 *
 * @author zoly
 */
public class LocalObjectPool<T> implements ObjectPool<T>, ObjectBorower<ObjectHolder<T>> {

    private final Queue<ObjectHolder<T>> localObjects;
    private final Map<T, ObjectHolder<T>> borrowedObjects;
    private final SmartObjectPool<ObjectHolder<T>> globalPool;
    private int reqReturnObjects;
    private final Thread thread;
    private final ReentrantLock lock;

    public LocalObjectPool(SmartObjectPool<ObjectHolder<T>> globalPool) {
        localObjects = new LinkedList<ObjectHolder<T>>();
        borrowedObjects = new HashMap<T, ObjectHolder<T>>();
        this.globalPool = globalPool;
        reqReturnObjects = 0;
        thread = Thread.currentThread();
        lock = new ReentrantLock();
    }

    @Override
    public T borrowObject() throws ObjectCreationException,
            InterruptedException, TimeoutException {
        lock.lock();
        try {
            T object;
            ObjectHolder<T> objectHolder;
            do {
                if (localObjects.isEmpty()) {
                    objectHolder = globalPool.borrowObject(this);
                } else {
                    objectHolder = localObjects.remove();
                }
                object = objectHolder.borrowOrCreateObjectIfPossible();
            } while (object == null);
            borrowedObjects.put(object, objectHolder);
            return object;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void returnObject(T object, Exception e) throws ObjectReturnException, ObjectDisposeException {
        lock.lock();
        try {
            ObjectHolder holder = borrowedObjects.remove(object);
            if (holder == null) {
                throw new IllegalArgumentException("Object " + object + " has not been borrowed from this pool");
            }
            try {
                holder.returnObject(object, e);
            } finally {
                if (reqReturnObjects > 0) {
                    reqReturnObjects--;
                    globalPool.returnObject(holder, this);
                } else {
                    localObjects.add(holder);
                }
            }
            
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void dispose() throws ObjectDisposeException {
        throw new UnsupportedOperationException("LocalPool dispose is not supported");
    }

    @Override
    public boolean scan(ScanHandler<ObjectHolder<T>> handler) throws Exception {
        lock.lock();
        try {
            for (ObjectHolder<T> object : localObjects) {
                if (!handler.handle(object)) {
                    return false;
                }
            }
            return true;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public Object requestReturnObject() {
        boolean acquired = lock.tryLock();
        if (acquired) {
            lock.lock();
            try {
                ObjectHolder<T> objectHolder = returnObjectIfAvailable();
                if (objectHolder != null) {
                    return objectHolder;
                } else {
                    reqReturnObjects++;
                    return ObjectBorower.REQUEST_MADE;
                }
            } finally {
                lock.unlock();
            }
        } else {
            return null;
        }
    }

    @Override
    public ObjectHolder<T> returnObjectIfAvailable() {
        boolean acquired = lock.tryLock();
        if (acquired) {
            try {
                if (!localObjects.isEmpty()) {
                    return localObjects.remove();
                } else {
                    return null;
                }
            } finally {
                lock.unlock();
            }
        } else {
            return null;
        }
    }

    @Override
    public Collection<ObjectHolder<T>> returnObjectsIfNotNeeded() {
        boolean acquired = lock.tryLock();
        if (acquired) {
            try {
                if (!thread.isAlive()) {
                    if (!borrowedObjects.isEmpty()) {
                        throw new IllegalStateException("Objects not returned by dead thread: " + borrowedObjects);
                    }
                    return localObjects;
                }
                return null;
            } finally {
                lock.unlock();
            }
        } else {
            return null;
        }
    }

    @Override
    public String toString() {
        return "LocalObjectPool{" + "localObjects=" + localObjects + ", borrowedObjects=" + borrowedObjects + ", reqReturnObjects=" + reqReturnObjects + ", thread=" + thread + '}';
    }
}
