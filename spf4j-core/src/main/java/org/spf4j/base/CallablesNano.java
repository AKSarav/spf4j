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
package org.spf4j.base;

import com.google.common.annotations.Beta;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import javax.annotation.ParametersAreNonnullByDefault;
import org.spf4j.base.Callables.AdvancedRetryPredicate;
import static org.spf4j.base.Callables.DEFAULT_EXCEPTION_RETRY;
import org.spf4j.base.Callables.FibonacciBackoffRetryPredicate;
import org.spf4j.base.Callables.TimeoutCallable;
import org.spf4j.base.Callables.TimeoutDelayPredicate;
import org.spf4j.base.Callables.TimeoutDelayPredicate2DelayPredicate;

/**
 * Utility class for executing stuff with retry logic.
 *
 * @author zoly
 */
@ParametersAreNonnullByDefault
@Beta
//CHECKSTYLE IGNORE RedundantThrows FOR NEXT 2000 LINES
public final class CallablesNano {

  private CallablesNano() {
  }

  public static <T, EX extends Exception> T executeWithRetry(final NanoTimeoutCallable<T, EX> what,
          final int nrImmediateRetries,
          final long maxRetryWaitNanos, final Class<EX> exceptionClass)
          throws InterruptedException, EX {
    return executeWithRetry(what, nrImmediateRetries, maxRetryWaitNanos,
            (TimeoutNanoDelayPredicate<? super T, T>) TimeoutNanoDelayPredicate.NORETRY_FOR_RESULT,
            DEFAULT_EXCEPTION_RETRY, exceptionClass);
  }

  public static <T, EX extends Exception> T executeWithRetry(final NanoTimeoutCallable<T, EX> what,
          final int nrImmediateRetries,
          final long maxRetryWaitNanos,
          final AdvancedRetryPredicate<Exception> retryOnException,
          final Class<EX> exceptionClass)
          throws InterruptedException, EX {
    return executeWithRetry(what, nrImmediateRetries, maxRetryWaitNanos,
            (TimeoutNanoDelayPredicate<? super T, T>) TimeoutNanoDelayPredicate.NORETRY_FOR_RESULT,
            retryOnException, exceptionClass);
  }

  /**
   * After the immediate retries are done, delayed retry with randomized Fibonacci values up to the specified max is
   * executed.
   *
   * @param <T> - the type returned by the Callable that is retried.
   * @param <EX> - the Exception thrown by the retried callable.
   * @param what - the callable to retry.
   * @param nrImmediateRetries - the number of immediate retries.
   * @param maxWaitNanos - maximum wait time in between retries.
   * @param retryOnReturnVal - predicate to control retry on return value;
   * @param retryOnException - predicate to retry on thrown exception.
   * @return the result of the callable.
   * @throws java.lang.InterruptedException - thrown if interrupted.
   * @throws EX - the exception declared to be thrown by the callable.
   */
  public static <T, EX extends Exception> T executeWithRetry(final NanoTimeoutCallable<T, EX> what,
          final int nrImmediateRetries, final long maxWaitNanos,
          final TimeoutNanoDelayPredicate<? super T, T> retryOnReturnVal,
          final AdvancedRetryPredicate<Exception> retryOnException,
          final Class<EX> exceptionClass)
          throws InterruptedException, EX {
    final long deadline = what.getDeadline();
    return Callables.executeWithRetry(what, new TimeoutDelayPredicate2DelayPredicate<>(deadline, retryOnReturnVal),
            new FibonacciBackoffRetryPredicate<>(retryOnException, nrImmediateRetries,
                    maxWaitNanos / 100, maxWaitNanos, Callables::rootClass, deadline,
                    () -> System.nanoTime(), TimeUnit.NANOSECONDS), exceptionClass);
  }

  public abstract static class NanoTimeoutCallable<T, EX extends Exception> extends TimeoutCallable<T, EX> {

    public NanoTimeoutCallable(final long timeoutNanos) {
      super(System.nanoTime() + timeoutNanos);
    }

  }

  public static <T, EX extends Exception> T executeWithRetry(final NanoTimeoutCallable<T, EX> what,
          final TimeoutNanoDelayPredicate<T, T> retryOnReturnVal,
          final TimeoutNanoDelayPredicate<Exception, T> retryOnException,
          final Class<EX> exceptionClass)
          throws InterruptedException, EX {
    return Callables.executeWithRetry(what, retryOnReturnVal, retryOnException, exceptionClass);
  }

  public static <T, EX extends Exception> T executeWithRetry(final NanoTimeoutCallable<T, EX> what,
          final TimeoutNanoDelayPredicate<Exception, T> retryOnException, final Class<EX> exceptionClass)
          throws InterruptedException, EX {
    return Callables.executeWithRetry(what,
            (TimeoutDelayPredicate<T, T>) TimeoutDelayPredicate.NORETRY_FOR_RESULT,
            retryOnException, exceptionClass);
  }

  public interface TimeoutNanoDelayPredicate<T, R> extends TimeoutDelayPredicate<T, R> {

    @Override
    Callables.RetryDecision<R> getDecision(T value, long deadlineNanos, Callable<R> what);

    TimeoutNanoDelayPredicate NORETRY_FOR_RESULT = new TimeoutNanoDelayPredicate<Object, Object>() {

      @Override
      public Callables.RetryDecision<Object> getDecision(final Object value,
              final long deadline, final Callable<Object> what) {
        return Callables.RetryDecision.abort();
      }

    };

  }

}
