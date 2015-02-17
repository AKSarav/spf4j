package org.spf4j.recyclable.impl;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.concurrent.TimeoutException;
import org.spf4j.ds.UpdateablePriorityQueue;
import org.spf4j.recyclable.ObjectBorrowException;
import org.spf4j.recyclable.ObjectCreationException;
import org.spf4j.recyclable.ObjectDisposeException;
import org.spf4j.recyclable.RecyclingSupplier;

/**
 * a object sharing pool.
 * this pool allows for non exclusive object sharing.
 * TODO: synchronization is too coarse, can be improved.
 * @author zoly
 */
public final class SharingObjectPool<T> implements RecyclingSupplier<T> {

    private final Factory<T> factory;

    public static final class SharedObject<T> {

        private int nrTimesShared;

        private final T object;

        public SharedObject(final T object) {
            this(object, 0);
        }

        public SharedObject(final T object, final int nrTimeShared) {
            this.object = object;
            this.nrTimesShared = nrTimeShared;
        }


        public T getObject() {
            return object;
        }

        private void inc() {
            nrTimesShared++;
        }

        private void dec() {
            nrTimesShared--;
        }

        public int getNrTimesShared() {
            return nrTimesShared;
        }

        @Override
        public String toString() {
            return "SharedObject{" + "nrTimesShared=" + nrTimesShared + ", object=" + object + '}';
        }



    }

    private static final Comparator<SharedObject<?>> SH_COMP = new Comparator<SharedObject<?>>() {

        @Override
        public int compare(final SharedObject<?> o1, final SharedObject<?> o2) {
            return o1.nrTimesShared - o2.getNrTimesShared();
        }

    };

    private final UpdateablePriorityQueue<SharedObject<T>> pooledObjects;
    private final Map<T, UpdateablePriorityQueue<SharedObject<T>>.ElementRef> o2Queue;

    private int nrObjects;
    private final int maxSize;
    private boolean closed;

    public SharingObjectPool(final Factory<T> factory, final int coreSize, final int maxSize)
            throws ObjectCreationException {
        this.factory = factory;
        this.pooledObjects = new UpdateablePriorityQueue<>(maxSize, SH_COMP);
        this.nrObjects = 0;
        this.closed = false;
        o2Queue = new IdentityHashMap<>(maxSize);
        for (int i = 0; i < coreSize; i++) {
            createObject(0);
        }
        this.maxSize = maxSize;
    }

    @Override
    public synchronized T get() throws ObjectCreationException, ObjectBorrowException,
            InterruptedException, TimeoutException {
        if (closed) {
            throw new ObjectBorrowException("Reclycler is closed " + this);
        }
        if (nrObjects > 0) {
            UpdateablePriorityQueue<SharedObject<T>>.ElementRef peekEntry = pooledObjects.peekEntry();
            final SharedObject<T> elem = peekEntry.getElem();
            if (elem.getNrTimesShared() == 0) {
                elem.inc();
                peekEntry.elementMutated();
                return elem.getObject();
            } else if (nrObjects < maxSize) {
                return createObject(1);
            } else {
                return elem.getObject();
            }
        } else {
            return createObject(1);
        }
    }

    private T createObject(final int nrTimesShared) throws ObjectCreationException {
        T obj = factory.create();
        o2Queue.put(obj, pooledObjects.add(new SharedObject<>(obj, nrTimesShared)));
        nrObjects++;
        return obj;
    }

    @Override
    @SuppressFBWarnings("REC_CATCH_EXCEPTION")
    public synchronized void recycle(final T object, final Exception e) {
        if (e != null) {
            if (o2Queue.containsKey(object)) { // element still in queue
                boolean isValid;
                try {
                    isValid = factory.validate(object, e); // validate
                } catch (Exception ex) {
                    // findbugs suggest rethrowing Runtime exception here.
                    // not realisting since people missuse RuntimeExceptions.
                    isValid = false;
                }
                if (!isValid) { // remove from pool
                    UpdateablePriorityQueue.ElementRef qref = o2Queue.remove(object);
                    nrObjects--;
                    qref.remove();
                }
            } // element already retired. TODO: retirement queue to validate the object beloged tot this pool.
        } else {
            //return to queue
            UpdateablePriorityQueue<SharedObject<T>>.ElementRef ref = o2Queue.get(object);
            ref.getElem().dec();
            ref.elementMutated();
        }
    }

    @Override
    public void recycle(final T object) {
        recycle(object, null);
    }

    @Override
    public synchronized void dispose() throws ObjectDisposeException {
        closed = true;
        ObjectDisposeException exres = null;
        for (SharedObject<T> obj : pooledObjects) {
            try {
                factory.dispose(obj.getObject());
            } catch (ObjectDisposeException ex) {
                if (exres == null) {
                    exres = ex;
                } else {
                    ex.addSuppressed(exres);
                    exres = ex;
                }
            }
        }
        if (exres != null) {
            throw exres;
        }
    }

    @Override
    public synchronized String toString() {
        return "SharingObjectPool{" + "factory=" + factory + ", pooledObjects=" + pooledObjects + '}';
    }

}
