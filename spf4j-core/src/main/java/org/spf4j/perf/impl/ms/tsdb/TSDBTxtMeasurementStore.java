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
package org.spf4j.perf.impl.ms.tsdb;

import com.google.common.base.Charsets;
import com.google.common.collect.Interner;
import com.google.common.collect.Interners;
import org.spf4j.perf.MeasurementsInfo;
import org.spf4j.perf.MeasurementStore;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import javax.annotation.PreDestroy;
import javax.annotation.concurrent.ThreadSafe;
import org.spf4j.io.Csv;
import org.spf4j.jmx.JmxExport;
import org.spf4j.perf.impl.ms.Id2Info;

/**
 * File based store implementation.
 * @author zoly
 */
@ThreadSafe
public final class TSDBTxtMeasurementStore
    implements MeasurementStore {

    private final BufferedWriter writer;

    private final String fileName;

    private static final Interner<String> INTERNER = Interners.newStrongInterner();

    public TSDBTxtMeasurementStore(final File file) throws IOException {
        this.writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file, true), Charsets.UTF_8));
        this.fileName = INTERNER.intern(file.getPath());
    }

    @Override
    public long alocateMeasurements(final MeasurementsInfo measurement,
                                    final int sampleTimeMillis) {
        return Id2Info.getId(measurement);
    }

    @Override
    public void saveMeasurements(final long tableId,
            final long timeStampMillis, final long ... measurements)
            throws IOException {
        MeasurementsInfo measurementInfo = Id2Info.getInfo(tableId);
        String groupName = measurementInfo.getMeasuredEntity().toString();
        synchronized (fileName) {
            Csv.writeCsvElement(groupName, writer);
            writer.write(',');
            writer.write(Long.toString(timeStampMillis));
//            writer.write(',');
//            writer.write(Integer.toString(sampleTimeMillis));
            for (int i = 0; i < measurements.length; i++) {
                String measurementName = measurementInfo.getMeasurementName(i);
                writer.write(',');
                Csv.writeCsvElement(measurementName, writer);
                writer.write(',');
                writer.write(Long.toString(measurements[i]));
            }
            writer.write('\n');
        }
    }

    @PreDestroy
    @Override
    public void close() throws IOException {
        writer.close();
    }

    @JmxExport(description = "flush out buffers")
    @Override
    public void flush() throws IOException {
        writer.flush();
    }

    @Override
    public String toString() {
        return "TSDBTxtMeasurementStore{" + "fileName=" + fileName + '}';
    }

}
