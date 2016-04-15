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
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.spf4j.base.AbstractRunnable;
import static org.spf4j.base.Runtime.WAIT_FOR_SHUTDOWN_MILLIS;

/**
 * This executor aims to be a general purpose executor for async tasks.
 * (equivalent to ForkJoinPool.commonPool())
 * @author zoly
 */
@SuppressFBWarnings("HES_EXECUTOR_NEVER_SHUTDOWN") // THere is a shutdownhook being registered which FB does not see
public final class DefaultExecutor {

    private DefaultExecutor() {
    }
    
    public static final ExecutorService INSTANCE;

    static {
        final int coreThreads = Integer.getInteger("defaultExecutor.coreThreads", 0);
        final int maxIdleMillis = Integer.getInteger("defaultExecutor.maxIdleMillis", 60000);
        final boolean isDaemon = Boolean.getBoolean("defaultExecutor.daemon");
        final String impParam = "defaultExecutor.implementation";
        final String value = System.getProperty(impParam, "fjp");
        switch (value) {
          case "spf4j":
            LifoThreadPoolExecutorSQP lifoExec = new LifoThreadPoolExecutorSQP("defaultExecutor", coreThreads,
                    Integer.MAX_VALUE, maxIdleMillis, 0, isDaemon, Integer.getInteger("defaultExecutor.spinlockCount",
                            1024));
            lifoExec.exportJmx();
            INSTANCE = lifoExec;
            break;
          case "fjp":
            INSTANCE = new ForkJoinPool(32767);
            break;
          case "legacy":
            new ThreadPoolExecutor(coreThreads, Integer.MAX_VALUE, maxIdleMillis, TimeUnit.MILLISECONDS,
                new SynchronousQueue<Runnable>(), new CustomThreadFactory("DefaultExecutor", isDaemon));
          default:
            throw new IllegalArgumentException("Ivalid setting for " + impParam + " = " + value);
        }

        org.spf4j.base.Runtime.queueHookAtEnd(new AbstractRunnable(true) {

            @Override
            public void doRun() throws InterruptedException {
                INSTANCE.shutdown();
                INSTANCE.awaitTermination(WAIT_FOR_SHUTDOWN_MILLIS, TimeUnit.MILLISECONDS);
                List<Runnable> remaining = INSTANCE.shutdownNow();
                if (remaining.size() > 0) {
                    System.err.println("Remaining tasks: " + remaining);
                }
            }
        });
    }

}
