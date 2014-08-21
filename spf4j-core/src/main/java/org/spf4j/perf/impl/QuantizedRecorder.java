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

import com.google.common.math.IntMath;
import org.spf4j.perf.EntityMeasurements;
import org.spf4j.perf.EntityMeasurementsInfo;
import org.spf4j.perf.MeasurementProcessor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.annotation.concurrent.ThreadSafe;

/**
 *
 * @author zoly
 */
@ThreadSafe
public final class QuantizedRecorder implements MeasurementProcessor {

    private long minMeasurement;
    private long maxMeasurement;
    private long measurementCount;
    private long measurementTotal;
    private final int quantasPerMagnitude;
    private final long[] magnitudes;
    private final EntityMeasurementsInfo info;
    private final int factor;
    private final int lowerMagnitude;
    private final int higherMagnitude;
    /**
     * (long.min - f ^ l), (f ^ l - f ^ (l+1)), ... (f ^(L-1) - f ^ L) (f^L - long.max)
     *
     * f = 10 m = 0 M = 5
     *
     */
    private final long[] quatizedMeasurements;

    public QuantizedRecorder(final Object measuredEntity, final String unitOfMeasurement,
            final int factor, final int lowerMagnitude,
            final int higherMagnitude, final int quantasPerMagnitude) {
        assert (quantasPerMagnitude <= factor);
        assert (lowerMagnitude < higherMagnitude);
        assert (quantasPerMagnitude > 0);
        this.factor = factor;
        this.lowerMagnitude = lowerMagnitude;
        this.higherMagnitude = higherMagnitude;
        minMeasurement = Long.MAX_VALUE;
        maxMeasurement = Long.MIN_VALUE;
        measurementCount = 0;
        measurementTotal = 0;
        this.quatizedMeasurements = new long[(higherMagnitude - lowerMagnitude) * quantasPerMagnitude + 2];
        this.quantasPerMagnitude = quantasPerMagnitude;
        magnitudes = new long[higherMagnitude - lowerMagnitude + 1];

        int idx = 0;
        if (lowerMagnitude < 0) {
            int toMagnitude = Math.min(-1, higherMagnitude);
            int toValue = -IntMath.pow(factor, -toMagnitude);
            idx = toMagnitude - lowerMagnitude;
            int j = idx;
            while (j >= 0) {
                magnitudes[j--] = toValue;
                toValue *= factor;
            }
            idx++;
        }
        if (lowerMagnitude <= 0 && higherMagnitude >= 0) {
            magnitudes[idx++] = 0;
        }

        int fromMagnitude = Math.max(1, lowerMagnitude);
        int fromValue = IntMath.pow(factor, fromMagnitude);
        int j = idx;
        while (j < magnitudes.length) {
            magnitudes[j++] = fromValue;
            fromValue *= factor;
        }

        final List<String> uom = new ArrayList();

        final List<String> result = new ArrayList();
        result.add("total");
        uom.add(unitOfMeasurement);
        result.add("count");
        uom.add("count");
        result.add("min");
        uom.add(unitOfMeasurement);
        result.add("max");
        uom.add(unitOfMeasurement);
        result.add("QNI_" + magnitudes[0]);
        uom.add("count");

        if (magnitudes.length > 0) {
            long prevVal = magnitudes[0];
            for (int i = 1; i < magnitudes.length; i++) {
                long magVal = magnitudes[i];
                long intSize = magVal - prevVal;
                for (j = 0; j < quantasPerMagnitude; j++) {
                    result.add("Q" + (prevVal + intSize * j / quantasPerMagnitude)
                            + "_" + (prevVal + intSize * (j + 1) / quantasPerMagnitude));
                    uom.add("count");
                }
                prevVal = magVal;
            }
            result.add("Q" + prevVal
                    + "_PI");
            uom.add("count");
        }
        info = new EntityMeasurementsInfoImpl(measuredEntity, unitOfMeasurement,
                result.toArray(new String[result.size()]), uom.toArray(new String[uom.size()]));

    }

    //CHECKSTYLE:OFF
    private QuantizedRecorder(final EntityMeasurementsInfo info, final int factor,
            final int lowerMagnitude, final int higherMagnitude,
            final long minMeasurement, final long maxMeasurement,
            final long measurementCount, final long measurementTotal,
            final int quantasPerMagnitude, final long[] magnitudes, final long[] quatizedMeasurements) {
        //CHECKSTYLE:ON
        assert (quantasPerMagnitude <= factor);
        assert (lowerMagnitude < higherMagnitude);
        assert (quantasPerMagnitude > 0);
        this.factor = factor;
        this.lowerMagnitude = lowerMagnitude;
        this.higherMagnitude = higherMagnitude;
        this.minMeasurement = minMeasurement;
        this.maxMeasurement = maxMeasurement;
        this.measurementCount = measurementCount;
        this.measurementTotal = measurementTotal;
        this.quantasPerMagnitude = quantasPerMagnitude;
        this.magnitudes = magnitudes;
        this.quatizedMeasurements = quatizedMeasurements;
        this.info = info;
    }

    @Override
    public synchronized void record(final long measurement) {
        measurementCount++;
        measurementTotal += measurement;  // TODO: check for overflow
        if (measurement < minMeasurement) {
            minMeasurement = measurement;
        }
        if (measurement > maxMeasurement) {
            maxMeasurement = measurement;
        }
        long m0 = magnitudes[0];
        if (m0 > measurement) {
            quatizedMeasurements[0]++;
        } else {
            long prevMag = m0;
            int i = 1;
            for (; i < magnitudes.length; i++) {
                long mag = magnitudes[i];
                if (mag > measurement) {
                    int qidx = (i - 1) * quantasPerMagnitude + (int) (quantasPerMagnitude
                            * (((double) (measurement - prevMag)) / (mag - prevMag))) + 1;
                    quatizedMeasurements[qidx]++;
                    break;
                }
                prevMag = mag;
            }
            if (i == magnitudes.length) {
                quatizedMeasurements[quatizedMeasurements.length - 1]++;
            }
        }
    }

    @Override
    public synchronized long[] getMeasurements() {
        long[] result = new long[info.getNumberOfMeasurements()];
        int i = 0;
        result[i++] = this.measurementTotal;
        result[i++] = this.measurementCount;

        result[i++] = this.minMeasurement;
        result[i++] = this.maxMeasurement;

        for (int j = 0; j < this.quatizedMeasurements.length; j++) {
            result[i++] = this.quatizedMeasurements[j];
        }
        return result;
    }

    @Override
    public EntityMeasurementsInfo getInfo() {
        return info;
    }

    @Override
    public EntityMeasurements aggregate(final EntityMeasurements mSource) {

        if (mSource instanceof QuantizedRecorder) {
            QuantizedRecorder other = (QuantizedRecorder) mSource;
            long[] quantizedM;
            long minMeasurementM;
            long maxMeasurementM;
            long measurementCountM;
            long measurementTotalM;
            synchronized (this) {
                quantizedM = quatizedMeasurements.clone();
                minMeasurementM = this.minMeasurement;
                maxMeasurementM = this.maxMeasurement;
                measurementCountM = this.measurementCount;
                measurementTotalM = this.measurementTotal;
            }
            QuantizedRecorder otherClone =  other.createClone();
            final long[] lQuatizedMeas = otherClone.getQuatizedMeasurements();
            for (int i = 0; i < quantizedM.length; i++) {

                quantizedM[i] += lQuatizedMeas[i];
            }
            return new QuantizedRecorder(info, factor, lowerMagnitude, higherMagnitude,
                    Math.min(minMeasurementM, otherClone.getMinMeasurement()),
                    Math.max(maxMeasurementM, otherClone.getMaxMeasurement()),
                    measurementCountM + otherClone.getMeasurementCount(),
                    measurementTotalM + otherClone.getMeasurementTotal(),
                    quantasPerMagnitude, magnitudes, quantizedM);
        } else {
            throw new IllegalArgumentException("Cannot aggregate " + this + " with " + mSource);
        }


    }

    @Override
    public synchronized QuantizedRecorder createClone() {
        return new QuantizedRecorder(info, factor, lowerMagnitude, higherMagnitude,
                minMeasurement, maxMeasurement, measurementCount, measurementTotal,
                quantasPerMagnitude, magnitudes, quatizedMeasurements.clone());
    }

    @Override
    public synchronized QuantizedRecorder reset() {
        QuantizedRecorder result = createClone();
        this.minMeasurement = Long.MAX_VALUE;
        this.maxMeasurement = Long.MIN_VALUE;
        this.measurementCount = 0;
        this.measurementTotal = 0;
        Arrays.fill(this.quatizedMeasurements, 0L);
        return result;
    }

    @Override
    public synchronized String toString() {
        return "QuantizedRecorder{" + "info=" + info + ", minMeasurement=" + minMeasurement + ", maxMeasurement="
                + maxMeasurement + ", measurementCount=" + measurementCount + ", measurementTotal="
                + measurementTotal + ", quantasPerMagnitude=" + quantasPerMagnitude + ", magnitudes="
                + Arrays.toString(magnitudes) + ", quatizedMeasurements="
                + Arrays.toString(quatizedMeasurements) + '}';
    }

    public synchronized long getMaxMeasurement() {
        return maxMeasurement;
    }

    public synchronized long getMeasurementCount() {
        return measurementCount;
    }

    public synchronized long getMeasurementTotal() {
        return measurementTotal;
    }

    public synchronized long getMinMeasurement() {
        return minMeasurement;
    }

    public synchronized long[] getQuatizedMeasurements() {
        return quatizedMeasurements.clone();
    }

    @Override
    public synchronized EntityMeasurements createLike(final Object entity) {
        return new QuantizedRecorder(entity, info.getUnitOfMeasurement(),
                this.factor, lowerMagnitude, higherMagnitude, quantasPerMagnitude);
    }

    @Override
    public long[] getMeasurementsAndReset() {
        return reset().getMeasurements();
    }
}
