package org.spf4j.concurrent;

import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.LockSupport;

/**
 * Special purpose queue for a single value
 * Custom designed for the LifoThreadPool
 *
 * @author zoly
 */
public final class UnitQueuePU<T> {

    private final AtomicReference<T> value = new AtomicReference<>();

    private final Thread readerThread;

    public UnitQueuePU(final Thread readerThread) {
        this.readerThread = readerThread;
    }



    public T poll() {
        T result = value.getAndSet(null);
        if (result != null) {
            return result;
        } else {
            return null;
        }
    }

    private static final Semaphore SPIN_LIMIT = new Semaphore(org.spf4j.base.Runtime.NR_PROCESSORS - 1);

    public T poll(final long timeoutNanos, final long spinCount) throws InterruptedException {
        T result = poll();
        if (result != null) {
            return result;
        }
        if (org.spf4j.base.Runtime.NR_PROCESSORS > 1 && spinCount > 0) {
            boolean tryAcquire = SPIN_LIMIT.tryAcquire();
            if (tryAcquire) {
                try {
                    int i = 0;
                    while (i < spinCount) {
                        if (i % 4 == 0) {
                            result = poll();
                            if (result != null) {
                                return result;
                            }
                        }
                        i++;
                    }
                } finally {
                    SPIN_LIMIT.release();
                }
            }
        }

            long deadlineNanos = System.nanoTime() + timeoutNanos;
                while ((result = value.getAndSet(null)) == null) {
                    final long to = deadlineNanos - System.nanoTime();
                    if (to <= 0) {
                        return null;
                    }
                    LockSupport.parkNanos(to);
                }
        return result;
    }

    public boolean offer(final T offer) {
        boolean result = value.compareAndSet(null, offer);
        if (result) {
            LockSupport.unpark(readerThread);
        }
        return result;
    }

}
