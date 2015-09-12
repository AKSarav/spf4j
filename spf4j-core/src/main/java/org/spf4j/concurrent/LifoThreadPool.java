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

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Queue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.locks.ReentrantLock;
import org.spf4j.jmx.JmxExport;

/**
 * See LifoThreadPoolBuilder for creating lifo  thread pools.
 *
 * @author zoly
 */
public interface LifoThreadPool extends ExecutorService {

    void exportJmx();

    void unregisterJmx();

    @JmxExport
    int getMaxIdleTimeMillis();

    @JmxExport
    int getMaxThreadCount();

    @JmxExport
    @SuppressFBWarnings(value = "MDM_WAIT_WITHOUT_TIMEOUT", justification = "Holders of this lock will not block")
    int getNrQueuedTasks();

    @JmxExport
    String getPoolName();

    @JmxExport
    int getQueueSizeLimit();

    ReentrantLock getStateLock();

    Queue<Runnable> getTaskQueue();

    @JmxExport
    int getThreadCount();

    @JmxExport
    int getThreadPriority();

    @JmxExport
    boolean isDaemonThreads();

}
