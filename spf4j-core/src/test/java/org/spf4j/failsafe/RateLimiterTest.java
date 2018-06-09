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

import java.util.concurrent.TimeUnit;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spf4j.base.TimeSource;

/**
 * @author Zoltan Farkas
 */
public class RateLimiterTest {

  private static final Logger LOG = LoggerFactory.getLogger(RateLimiterTest.class);

  @Test(expected = IllegalArgumentException.class)
  public void testRateLimitInvalid() {
    new RateLimiter(1000, 9);
  }

  @Test
  public void testRateLimitArgs() {
    try (RateLimiter rateLimiter = new RateLimiter(17, 10)) {
    LOG.debug("Rate Limiter = {}", rateLimiter);
    Assert.assertEquals(1d, rateLimiter.getPermitsPerReplenishInterval(), 0.001);
    }
  }

  @Test
  public void testRateLimitTryAcquisition() throws InterruptedException {
    try (RateLimiter rateLimiter = new RateLimiter(10, 10)) {
      LOG.debug("Rate Limiter = {}", rateLimiter);
      Assert.assertFalse(rateLimiter.tryAcquire(20, 0, TimeUnit.MILLISECONDS));
      long startTime = TimeSource.nanoTime();
      boolean tryAcquire = rateLimiter.tryAcquire(20, 2, TimeUnit.SECONDS);
      LOG.debug("waited {} ns for {}", (TimeSource.nanoTime() - startTime), rateLimiter);
      Assert.assertTrue(tryAcquire);
      Assert.assertFalse(rateLimiter.tryAcquire(20, 1, TimeUnit.SECONDS));
    }
  }


//  @Test
//  public void testRateLimitTryAcquisition2() throws InterruptedException {
//    ScheduledExecutorService mockExec = Mockito.mock(ScheduledExecutorService.class);
//    Mockito.when(mockExec.scheduleAtFixedRate(Mockito.any(),
//            Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(LOG)
//    try (RateLimiter rateLimiter = new RateLimiter(10, 10)) {
//      LOG.debug("Rate Limiter = {}", rateLimiter);
//      Assert.assertFalse(rateLimiter.tryAcquire(20, 0, TimeUnit.MILLISECONDS));
//      long startTime = TimeSource.nanoTime();
//      boolean tryAcquire = rateLimiter.tryAcquire(20, 2, TimeUnit.SECONDS);
//      LOG.debug("waited {} ns for {}", (TimeSource.nanoTime() - startTime), rateLimiter);
//      Assert.assertTrue(tryAcquire);
//      Assert.assertFalse(rateLimiter.tryAcquire(20, 1, TimeUnit.SECONDS));
//    }
//  }



}
