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
package org.spf4j.perf.impl;

import java.io.Closeable;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import javax.annotation.concurrent.ThreadSafe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spf4j.base.AbstractRunnable;
import org.spf4j.base.DefaultScheduler;
import org.spf4j.base.Pair;
import org.spf4j.perf.EntityMeasurements;
import org.spf4j.perf.EntityMeasurementsSource;
import org.spf4j.perf.MeasurementDatabase;
import org.spf4j.perf.MeasurementProcessor;
import org.spf4j.perf.MeasurementRecorder;
import org.spf4j.perf.MeasurementRecorderSource;

@ThreadSafe
public final class ScalableMeasurementRecorderSource implements
        MeasurementRecorderSource, EntityMeasurementsSource, Closeable {

    private static final Logger LOG = LoggerFactory.getLogger(ScalableMeasurementRecorder.class);

    
    private final Map<Thread, Map<Object, MeasurementProcessor>> measurementProcessorMap;
    
    private final ThreadLocal<Map<Object, MeasurementProcessor>> threadLocalMeasurementProcessorMap;
    

    private final ScheduledFuture<?> samplingFuture;
    private final MeasurementProcessor processorTemplate;
    
    public ScalableMeasurementRecorderSource(final MeasurementProcessor processor,
            final int sampleTimeMillis, final MeasurementDatabase database) {
        this.processorTemplate = processor;
        measurementProcessorMap = new HashMap<Thread, Map<Object, MeasurementProcessor>>();
        threadLocalMeasurementProcessorMap = new ThreadLocal<Map<Object, MeasurementProcessor>>() {

            @Override
            protected Map<Object, MeasurementProcessor> initialValue() {
                Map<Object, MeasurementProcessor> result = new HashMap<Object, MeasurementProcessor>();
                synchronized (measurementProcessorMap) {
                    measurementProcessorMap.put(Thread.currentThread(), result);
                }
                return result;
            }
            
        };
        samplingFuture = DefaultScheduler.scheduleAllignedAtFixedRateMillis(new AbstractRunnable(true) {
            
            private volatile long lastRun = 0;
            
            @Override
            public void doRun() throws IOException {
                long currentTime = System.currentTimeMillis();
                if (currentTime > lastRun) {
                    lastRun = currentTime;
                    for (EntityMeasurements m
                            : ScalableMeasurementRecorderSource.this.getEntitiesMeasurements(true).values()) {
                        database.saveMeasurements(m.getInfo(), m.getMeasurements(true), currentTime, sampleTimeMillis);
                    }
                } else {
                    LOG.warn("Last measurement recording was at {} current run is {}, something is wrong",
                            lastRun, currentTime);
                }
            }
        }, sampleTimeMillis);
    }
    
    @Override
    public MeasurementRecorder getRecorder(final Object forWhat) {
        Map<Object, MeasurementProcessor> recorders = threadLocalMeasurementProcessorMap.get();
        synchronized (recorders) {
            MeasurementProcessor result = recorders.get(forWhat);
            if (result == null)  {
                result = (MeasurementProcessor) processorTemplate.createLike(
                        Pair.of(processorTemplate.getInfo().getMeasuredEntity(), forWhat));
                recorders.put(forWhat, result);
            }
            return result;
        }
    }

    @Override
    public Map<Object, EntityMeasurements> getEntitiesMeasurements(final boolean reset) {
        
        Map<Object, EntityMeasurements> result = new HashMap<Object, EntityMeasurements>();
        
        synchronized (measurementProcessorMap) {
            for (Map.Entry<Thread, Map<Object, MeasurementProcessor>> entry : measurementProcessorMap.entrySet()) {
                
                Map<Object, MeasurementProcessor> measurements = entry.getValue();
                synchronized (measurements) {
                    for (Map.Entry<Object, MeasurementProcessor> lentry : measurements.entrySet()) {

                        Object what = lentry.getKey();
                        EntityMeasurements existingMeasurement = result.get(what);
                        if (existingMeasurement == null) {
                            existingMeasurement = lentry.getValue().createClone(reset);
                        } else {
                            existingMeasurement = existingMeasurement.aggregate(lentry.getValue().createClone(reset));
                        }
                        result.put(what, existingMeasurement);
                    }
                }
            }
        }
        return result;
        
        
    }
    
    @Override
    public void close() {
        samplingFuture.cancel(false);
    }

    @Override
    protected void finalize() throws Throwable {
        try {
            super.finalize();
        } finally {
            this.close();
        }
    }
    
    
}
