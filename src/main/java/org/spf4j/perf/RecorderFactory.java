
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
package org.spf4j.perf;

import org.spf4j.perf.impl.CountingRecorder;
import org.spf4j.perf.impl.MinMaxAvgRecorder;
import org.spf4j.perf.impl.QuantizedRecorder;
import org.spf4j.perf.impl.ScalableMeasurementRecorder;
import org.spf4j.perf.impl.ScalableMeasurementRecorderSource;
import org.spf4j.perf.impl.mdb.tsdb.TSDBMeasurementDatabase;
import java.io.File;
import java.lang.management.ManagementFactory;

/**
 *
 * @author zoly
 */
public final class RecorderFactory {
    
    private RecorderFactory() { }
    
    public static final TSDBMeasurementDatabase TS_DATABASE;
    
    static {
        try {
            TS_DATABASE = new TSDBMeasurementDatabase(System.getProperty("perf.db.folder",
            System.getProperty("java.io.tmpdir")) + File.separator + System.getProperty("perf.db.name",
                    ManagementFactory.getRuntimeMXBean().getName() + ".tsdb"));
            TS_DATABASE.registerJmx();
            TS_DATABASE.flushEvery(600000);
            TS_DATABASE.closeOnShutdown();
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }
    
    public static MeasurementRecorder createScalableQuantizedRecorder(
            final Object forWhat, final String unitOfMeasurement, final int sampleTimeMillis,
             final int factor, final int lowerMagnitude,
            final int higherMagnitude, final int quantasPerMagnitude) {
        return new ScalableMeasurementRecorder(new QuantizedRecorder(forWhat,
                unitOfMeasurement, factor, lowerMagnitude, higherMagnitude,
                quantasPerMagnitude), sampleTimeMillis, TS_DATABASE);
    }
    
    public static MeasurementRecorder createScalableCountingRecorder(
            final Object forWhat, final String unitOfMeasurement, final int sampleTimeMillis) {
        return new ScalableMeasurementRecorder(new CountingRecorder(forWhat,
                unitOfMeasurement), sampleTimeMillis, TS_DATABASE);
    }
    
    
        public static MeasurementRecorder createScalableMinMaxAvgRecorder(
            final Object forWhat, final String unitOfMeasurement, final int sampleTimeMillis) {
        return new ScalableMeasurementRecorder(new MinMaxAvgRecorder(forWhat,
                unitOfMeasurement), sampleTimeMillis, TS_DATABASE);
    }
    
    
    public static MeasurementRecorderSource createScalableQuantizedRecorderSource(
            final Object forWhat, final String unitOfMeasurement, final int sampleTimeMillis,
            final int factor, final int lowerMagnitude,
            final int higherMagnitude, final int quantasPerMagnitude) {
        return new ScalableMeasurementRecorderSource(new QuantizedRecorder(forWhat,
                unitOfMeasurement, factor, lowerMagnitude, higherMagnitude, quantasPerMagnitude),
                sampleTimeMillis, TS_DATABASE);
    }
    
    
    public static MeasurementRecorderSource createScalableCountingRecorderSource(
            final Object forWhat, final String unitOfMeasurement, final int sampleTimeMillis) {
        return new ScalableMeasurementRecorderSource(new CountingRecorder(forWhat,
                unitOfMeasurement), sampleTimeMillis, TS_DATABASE);
    }
    
   public static MeasurementRecorderSource createScalableMinMaxAvgRecorderSource(
            final Object forWhat, final String unitOfMeasurement, final int sampleTimeMillis) {
        return new ScalableMeasurementRecorderSource(new MinMaxAvgRecorder(forWhat,
                unitOfMeasurement), sampleTimeMillis, TS_DATABASE);
    }
    
    
}
