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
package org.spf4j.pool.impl;

import org.spf4j.base.Callables;
import org.spf4j.concurrent.RetryExecutor;
import org.spf4j.pool.ObjectBorrowException;
import org.spf4j.pool.ObjectCreationException;
import org.spf4j.pool.ObjectDisposeException;
import org.spf4j.pool.ObjectPool;
import org.spf4j.pool.ObjectReturnException;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.util.Arrays;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeoutException;
import junit.framework.Assert;
import org.junit.Test;

/**
 *
 * @author zoly
 */
public final class ObjectPoolBuilderTest {
  
    /**
     * Test of build method, of class ObjectPoolBuilder.
     */
    @Test
    public void testBuild()
            throws ObjectCreationException, ObjectBorrowException,
            InterruptedException, TimeoutException, ObjectReturnException, ObjectDisposeException {
        System.out.println("build");
        ObjectPool<ExpensiveTestObject> pool = new ObjectPoolBuilder(10, new ExpensiveTestObjectFactory()).build();
        System.out.println(pool);
        ExpensiveTestObject object = pool.borrowObject();
        System.out.println(pool);
        pool.returnObject(object, null);
        System.out.println(pool);
    }
    
 
    @Test(timeout = 20000)
    public void testPoolUseNoFailures()
                throws ObjectCreationException, ObjectBorrowException, InterruptedException,
                TimeoutException, ObjectReturnException, ObjectDisposeException, ExecutionException {
        System.out.println("poolUse");
        final ObjectPool<ExpensiveTestObject> pool
                = new ObjectPoolBuilder(10, new ExpensiveTestObjectFactory(1000000, 1000000, 1, 5)).build();
        runTest(pool, 0, 10000);
        pool.dispose();
    }
    
    
    @Test(timeout = 20000)
    public void testPoolUse()
            throws ObjectCreationException, ObjectBorrowException, InterruptedException,
            TimeoutException, ObjectReturnException, ObjectDisposeException, ExecutionException {
        System.out.println("poolUse");
        final ObjectPool<ExpensiveTestObject> pool
                = new ObjectPoolBuilder(10, new ExpensiveTestObjectFactory()).build();
        runTest(pool, 0, 10000);
        try {
            pool.dispose();
        } catch (ObjectDisposeException ex) {
            ex.printStackTrace();
        }
    }
    
    @Test(timeout = 200000)
    public void testPoolUseWithMaintenance()
            throws ObjectCreationException, ObjectBorrowException, InterruptedException,
            TimeoutException, ObjectReturnException, ObjectDisposeException, ExecutionException {
        System.out.println("poolUseWithMainteinance");

        final ObjectPool<ExpensiveTestObject> pool = new ObjectPoolBuilder(10, new ExpensiveTestObjectFactory())
                .withMaintenance(org.spf4j.base.DefaultScheduler.INSTANCE, 10, true).build();
        runTest(pool, 5, 100000);
        try {
            pool.dispose();
        } catch (ObjectDisposeException ex) {
            ex.printStackTrace();
        }
    }

    private volatile boolean isDeadlock = false;
    
    private Thread startDeadlockMonitor(final long deadlockTimeout) {
        isDeadlock = false;
        Thread monitor = new Thread(new Runnable() {

            @Override
            public void run() {
                try {
                    Thread.sleep(deadlockTimeout);
                    ThreadMXBean threadMX = ManagementFactory.getThreadMXBean();
                    System.err.println(Arrays.toString(threadMX.dumpAllThreads(true, true)));
                    isDeadlock = true;
                } catch (InterruptedException ex) {
                    // terminating monitor
                    return;
                }
            }
        });
        monitor.start();
        return monitor;
    }

    private void runTest(final ObjectPool<ExpensiveTestObject> pool,
            final long sleepBetweenSubmit, final long deadlockTimeout) throws InterruptedException, ExecutionException {
        Thread monitor = startDeadlockMonitor(deadlockTimeout);
        ExecutorService execService = Executors.newFixedThreadPool(10);
        BlockingQueue<Future<Integer>> completionQueue = new LinkedBlockingDeque<Future<Integer>>();
        RetryExecutor<Integer> exec
                = new RetryExecutor<Integer>(execService, 8, 16, 5000, Callables.RETRY_FOR_ANY_EXCEPTION,
                 completionQueue);
        int nrTests = 1000;        
        for (int i = 0; i < nrTests; i++) {
            exec.submit(new TestCallable(pool, i));
            Thread.sleep(sleepBetweenSubmit);
        }
        for (int i = 0; i < nrTests; i++) {
            System.out.println("Task " + completionQueue.take().get() + " finished ");
        }
        monitor.interrupt();
        monitor.join();
        Thread.sleep(100);
        Assert.assertEquals(0, completionQueue.size());
        if (isDeadlock) {
            Assert.fail("deadlock detected");
        }
    }
   
  
}
