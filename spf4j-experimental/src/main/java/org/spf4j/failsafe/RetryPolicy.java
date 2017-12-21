/*
 * Copyright (c) 2001-2017, Zoltan Farkas All Rights Reserved.
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
 *
 * Additionally licensed with:
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.spf4j.failsafe;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;

/**
 * @author Zoltan Farkas
 */
public class RetryPolicy<T, C extends Callable<T>> {

  private final RetryPredicate<T, C> retryOnReturnVal;
  private final RetryPredicate<Exception, C> retryOnException;

  public RetryPolicy(final RetryPredicate<T, C> retryOnReturnVal,
          final RetryPredicate<Exception, C> retryOnException) {
    this.retryOnReturnVal = retryOnReturnVal;
    this.retryOnException = retryOnException;
  }

  public RetryPolicy(final RetryPredicate<Exception, C> retryOnException) {
    this(RetryPredicate.NORETRY, retryOnException);
  }

  public final <EX extends Exception> T execute(final C pwhat, final Class<EX> exceptionClass)
          throws EX, InterruptedException, TimeoutException {
    return Retry.execute(pwhat, getRetryOnReturnVal(), getRetryOnException(), exceptionClass);
  }

  public RetryPredicate<T, C> getRetryOnReturnVal() {
    return retryOnReturnVal.newInstance();
  }

  public RetryPredicate<Exception, C> getRetryOnException() {
    return retryOnException.newInstance();
  }

  public static final class Builder<T, C extends Callable<T>> {

    private static final long DEFAULT_MAX_DELAY_NANOS = TimeUnit.SECONDS.toNanos(5);

    private static final long DEFAULT_INITIAL_DELAY_NANOS = TimeUnit.MILLISECONDS.toNanos(1);

    private static final int DEFAULT_INITIAL_NODELAY_RETRIES = 3;

    private static final class PredicateDetail<A, B extends Callable> {

      private RetryPredicate defaultPredicate = RetryPredicate.NORETRY;

      private Function<A, BackoffDelay> backoffSupplier
              = (x) -> new RandomizedBackoff(new FibonacciBackoff(DEFAULT_INITIAL_NODELAY_RETRIES,
                      DEFAULT_INITIAL_DELAY_NANOS, DEFAULT_MAX_DELAY_NANOS));

      private List<PartialRetryPredicate<A, B>> predicates = new ArrayList<>();
    }

    private PredicateDetail<T, C> resultPredicate = null;
    private PredicateDetail<Exception, C> exceptionPredicate = null;

    public RetryPolicy<T, C> build(Class<T> clasz) {
      RetryPredicate<T, C> rp = resultPredicate == null ? RetryPredicate.NORETRY
              : new DefaultRetryPredicate<T>(resultPredicate.defaultPredicate,
                      resultPredicate.backoffSupplier,
                      resultPredicate.predicates.toArray(
                              new PartialRetryPredicate[resultPredicate.predicates.size()]));
      PredicateDetail<Exception, C> ep = exceptionPredicate == null ? RetryPredicate.NORETRY
              : new DefaultRetryPredicate(exceptionPredicate.defaultPredicate,
                      exceptionPredicate.backoffSupplier,
                      exceptionPredicate.predicates.toArray(
                              new PartialRetryPredicate[exceptionPredicate.predicates.size()]));
    }

  }

}
