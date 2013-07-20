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

import com.google.common.base.Charsets;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * File based Lock implementation, that can be used as IPC method.
 *
 * @author zoly
 */
public final class FileBasedLock implements Lock, java.io.Closeable {

    public static final Map<File, Lock> LOCKS = new HashMap<File, Lock>();
    private final RandomAccessFile file;
    private final Lock jvmLock;
    private FileLock fileLock;

    public FileBasedLock(final File lockFile) throws FileNotFoundException {
        file = new RandomAccessFile(lockFile, "rws");
        synchronized (LOCKS) {
            Lock lock = LOCKS.get(lockFile);
            if (lock == null) {
                lock = new ReentrantLock();
                LOCKS.put(lockFile, lock);
            }
            jvmLock = lock;
        }
        fileLock = null;
    }

    @Override
    public void lock() {
        jvmLock.lock();
        try {
            fileLock = file.getChannel().lock();
            file.write(org.spf4j.base.Runtime.PROCESS_NAME.getBytes(Charsets.UTF_8));
        } catch (IOException ex) {
            jvmLock.unlock();
            throw new RuntimeException(ex);
        } catch (RuntimeException ex) {
            jvmLock.unlock();
            throw ex;
        }
    }

    @Override
    public void lockInterruptibly() throws InterruptedException {
        jvmLock.lockInterruptibly();
        try {
            final FileChannel channel = file.getChannel();
            //CHECKSTYLE:OFF
            while ((fileLock = channel.tryLock()) == null && !Thread.interrupted()) {
                //CHECKSTYLE:ON
                Thread.sleep(1);
            }
            file.write(org.spf4j.base.Runtime.PROCESS_NAME.getBytes(Charsets.UTF_8));
        } catch (IOException ex) {
            jvmLock.unlock();
            throw new RuntimeException(ex);
        } catch (RuntimeException ex) {
            jvmLock.unlock();
            throw ex;
        }
    }

    @Override
    public boolean tryLock() {
        if (jvmLock.tryLock()) {
            try {
                fileLock = file.getChannel().tryLock();
                if (fileLock != null) {
                    file.write(org.spf4j.base.Runtime.PROCESS_NAME.getBytes(Charsets.UTF_8));
                    return true;
                } else {
                    return false;
                }
            } catch (IOException ex) {
                jvmLock.unlock();
                throw new RuntimeException(ex);
            } catch (RuntimeException ex) {
                jvmLock.unlock();
                throw ex;
            }
        } else {
            return false;
        }
    }

    @Override
    public boolean tryLock(final long time, final TimeUnit unit) throws InterruptedException {
        if (jvmLock.tryLock()) {
            try {
                long waitTime = 0;
                long maxWaitTime = unit.toMillis(time);
                while (waitTime < maxWaitTime
                        //CHECKSTYLE:OFF
                        && (fileLock = file.getChannel().tryLock()) != null
                        //CHECKSTYLE:ON
                        && !Thread.interrupted()) {
                    Thread.sleep(1);
                    waitTime++;
                }
                if (fileLock != null) {
                    file.write(org.spf4j.base.Runtime.PROCESS_NAME.getBytes(Charsets.UTF_8));
                    return true;
                } else {
                    return false;
                }
            } catch (IOException ex) {
                jvmLock.unlock();
                throw new RuntimeException(ex);
            } catch (RuntimeException ex) {
                jvmLock.unlock();
                throw ex;
            }
        } else {
            return false;
        }
    }

    @Override
    public void unlock() {
        try {
            fileLock.release();
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
        jvmLock.unlock();
    }

    @Override
    public Condition newCondition() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    protected void finalize() throws Throwable {
        try {
            super.finalize();
        } finally {
            close();
        }
    }

    @Override
    public void close() throws IOException {
        try {
            file.close();
        } finally {
            jvmLock.unlock();
        }
    }
}
