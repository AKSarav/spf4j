
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
package org.spf4j.perf.io;

import java.io.Closeable;
import java.io.IOException;
import java.util.concurrent.ExecutionException;
import org.spf4j.base.AbstractRunnable;
import org.spf4j.concurrent.DefaultScheduler;
import org.spf4j.perf.MeasurementRecorder;
import org.spf4j.perf.impl.RecorderFactory;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spf4j.base.Runtime;
import org.spf4j.base.SysExits;
import org.spf4j.jmx.JmxExport;
import org.spf4j.jmx.Registry;

/**
 * This class allows you to poll and recordAt to a file the heap commited and heap used for your java process. start
 * data recording by calling the startMemoryUsageSampling method, stop the data recording by calling the method:
 * startMemoryUsageSampling.
 *
 * @author zoly
 */
public final class OpenFilesSampler {

  private OpenFilesSampler() {
  }

  private static ScheduledFuture<?> samplingFuture;
  private static AccumulatorRunnable accumulator;

  private static final Logger LOG = LoggerFactory.getLogger(OpenFilesSampler.class);

  private static volatile CharSequence lastWarnLsof = "";

  static {
    org.spf4j.base.Runtime.queueHook(2, new AbstractRunnable(true) {
      @Override
      public void doRun() {
        stop();
      }
    });
    Registry.export(OpenFilesSampler.class);
  }

  public static void start(final long sampleTimeMillis) {
    start(sampleTimeMillis, Runtime.MAX_NR_OPENFILES - Runtime.MAX_NR_OPENFILES / 10,
            Runtime.MAX_NR_OPENFILES, true);
  }

  @JmxExport
  public static void start(@JmxExport("sampleTimeMillis") final long sampleTimeMillis,
          @JmxExport("shutdownOnError") final boolean shutdownOnError) {
    start(sampleTimeMillis, Runtime.MAX_NR_OPENFILES - Runtime.MAX_NR_OPENFILES / 10,
            Runtime.MAX_NR_OPENFILES, shutdownOnError);
  }

  @JmxExport
  public static String getWarnLsofDetail() {
    return lastWarnLsof.toString();
  }

  public static void start(final long sampleTimeMillis,
          final int warnThreshold, final int errorThreshold, final boolean shutdownOnError) {
    start(sampleTimeMillis, warnThreshold, errorThreshold, shutdownOnError, (int) sampleTimeMillis * 10);
  }

  public static synchronized void start(final long sampleTimeMillis,
          final int warnThreshold, final int errorThreshold, final boolean shutdownOnError,
          final int aggTimeMillis) {
    if (samplingFuture == null) {
      accumulator = new AccumulatorRunnable(errorThreshold, shutdownOnError,
              warnThreshold, aggTimeMillis);
      samplingFuture = DefaultScheduler.INSTANCE.scheduleWithFixedDelay(accumulator,
              sampleTimeMillis, sampleTimeMillis, TimeUnit.MILLISECONDS);
    } else {
      throw new IllegalStateException("Open file usage sampling already started " + samplingFuture);
    }
  }

  @JmxExport
  public static synchronized void stop() {
    if (samplingFuture != null) {
      samplingFuture.cancel(false);
      samplingFuture = null;
      accumulator.close();
      accumulator = null;
    }
  }

  @JmxExport
  public static synchronized boolean isStarted() {
    return samplingFuture != null;
  }

  @JmxExport
  public static String getLsof() throws IOException, InterruptedException, ExecutionException, TimeoutException {
    CharSequence lsofOutput = Runtime.getLsofOutput();
    return lsofOutput == null ? "unable to obtain lsof" : lsofOutput.toString();
  }

  @JmxExport
  public static int getMaxNrOpenFiles() {
    return Runtime.MAX_NR_OPENFILES;
  }

  @JmxExport
  public static int getNrOpenFiles() throws IOException, InterruptedException, ExecutionException, TimeoutException {
    return Runtime.getNrOpenFiles();
  }

  @JmxExport
  public static int getWarnThreshold() {
    if (accumulator == null) {
      return -1;
    }
    return accumulator.getWarnThreshold();
  }

  @JmxExport
  public static int getErrorThreshold() {
    if (accumulator == null) {
      return -1;
    }
    return accumulator.getErrorThreshold();
  }


  private static class AccumulatorRunnable extends AbstractRunnable implements Closeable {

    private final int errorThreshold;
    private final boolean shutdownOnError;
    private final int warnThreshold;
    private final MeasurementRecorder nrOpenFiles;

    AccumulatorRunnable(final int errorThreshold, final boolean shutdownOnError,
            final int warnThreshold, final int aggMillis) {
      this.errorThreshold = errorThreshold;
      this.shutdownOnError = shutdownOnError;
      this.warnThreshold = warnThreshold;
      this.nrOpenFiles = RecorderFactory.createScalableMinMaxAvgRecorder("nr-open-files", "count", aggMillis);
    }

    @Override
    public void doRun() throws Exception {
      long time = System.currentTimeMillis();
      int nrOf = Runtime.getNrOpenFiles();
      if (nrOf > errorThreshold) {
        try {
          lastWarnLsof = Runtime.getLsofOutput();
        } catch (IOException ex) {
          String msg = ex.getMessage();
          if (msg == null || !msg.contains("Too many open files")) {
            throw ex;
          }
        }
        LOG.error("Nr open files is {} and exceeds error threshold {}, detail:\n{}",
                nrOf, errorThreshold, lastWarnLsof);
        if (shutdownOnError) {
          Runtime.goDownWithError(null, SysExits.EX_IOERR);
        }
      } else if (nrOf > warnThreshold) {
        try {
          lastWarnLsof = Runtime.getLsofOutput();
        } catch (IOException ex) {
          String msg = ex.getMessage();
          if (msg == null || !msg.contains("Too many open files")) {
            throw ex;
          }
        }
        LOG.warn("Nr open files is {} and exceeds warn threshold {}, detail:\n{} ",
                nrOf, warnThreshold, lastWarnLsof);
        if (!Runtime.gc(60000)) {
          LOG.warn("Unable to trigger GC although running low on file resources");
        } else {
          LOG.warn("gc executed nr open files reduced by {} files", nrOf - Runtime.getNrOpenFiles());
        }
      }
      this.nrOpenFiles.recordAt(time, nrOf);
    }

    @Override
    public void close() {
      try {
        this.nrOpenFiles.close();
      } catch (IOException ex) {
        throw new RuntimeException(ex);
      }
    }

    public int getErrorThreshold() {
      return errorThreshold;
    }

    public boolean isShutdownOnError() {
      return shutdownOnError;
    }

    public int getWarnThreshold() {
      return warnThreshold;
    }

  }

}
