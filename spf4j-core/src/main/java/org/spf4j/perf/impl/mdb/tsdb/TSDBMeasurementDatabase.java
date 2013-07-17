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
package org.spf4j.perf.impl.mdb.tsdb;

import com.google.common.base.Charsets;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import org.spf4j.base.AbstractRunnable;
import org.spf4j.base.Arrays;
import org.spf4j.base.DefaultScheduler;
import org.spf4j.base.Pair;
import org.spf4j.perf.EntityMeasurementsInfo;
import org.spf4j.perf.MeasurementDatabase;
import org.spf4j.perf.impl.chart.Charts;
import org.spf4j.perf.tsdb.TSTable;
import org.spf4j.perf.tsdb.TimeSeriesDatabase;
import java.awt.image.BufferedImage;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import javax.annotation.PreDestroy;
import javax.annotation.concurrent.ThreadSafe;
import javax.imageio.ImageIO;
import javax.management.InstanceAlreadyExistsException;
import javax.management.MBeanRegistrationException;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;
import org.jfree.chart.JFreeChart;
import org.joda.time.DateTimeConstants;
import org.joda.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author zoly
 */
@ThreadSafe
public final class TSDBMeasurementDatabase
    implements MeasurementDatabase, Closeable, TSDBMeasurementDatabaseMBean {

    private final TimeSeriesDatabase database;
    private volatile ScheduledFuture<?> future;
    private static final Logger LOG = LoggerFactory.getLogger(TSDBMeasurementDatabase.class);

    public TSDBMeasurementDatabase(final String databaseName) throws IOException {
        this.database = new TimeSeriesDatabase(databaseName, new byte[]{});
    }
    private static final AtomicInteger COUNTER = new AtomicInteger(0);

    public void registerJmx() throws MalformedObjectNameException, InstanceAlreadyExistsException,
            MBeanRegistrationException, NotCompliantMBeanException {
        ManagementFactory.getPlatformMBeanServer().registerMBean(this,
                new ObjectName("SPF4J:name=TSDBMeasurementDatabase" + COUNTER.getAndIncrement()));
    }

    public void closeOnShutdown() {
        Runtime.getRuntime().addShutdownHook(new Thread(new AbstractRunnable(false) {
            @Override
            public void doRun() throws Exception {
                close();
            }
        }, "tsdb shutdown"));
    }

    public void flushEvery(final int intervalMillis) {
        future = DefaultScheduler.INSTANCE.scheduleAtFixedRate(new AbstractRunnable(false) {
            @Override
            public void doRun() throws Exception {
                database.flush();
            }
        }, intervalMillis, intervalMillis, TimeUnit.MILLISECONDS);
    }

    @Override
    public void alocateMeasurements(final EntityMeasurementsInfo measurement,
                                    final int sampleTimeMillis) throws IOException {
        String groupName = measurement.getMeasuredEntity().toString();
        alocateMeasurements(groupName, measurement, sampleTimeMillis);
    }
    
    private void alocateMeasurements(final String groupName, final EntityMeasurementsInfo measurement,
            final int sampleTimeMillis) throws IOException {
        synchronized (database) {
            if (!database.hasTSTable(groupName)) {
                String[] measurementNames = measurement.getMeasurementNames();
                byte[] uom = measurement.getUnitOfMeasurement().getBytes(Charsets.UTF_8);
                byte[][] metaData = new byte[measurementNames.length][];
                String [] uoms = measurement.getMeasurementUnits();
                for (int i = 0; i < metaData.length; i++) {
                    metaData[i] = uoms[i].getBytes(Charsets.UTF_8);
                }
                database.addTSTable(groupName, uom, sampleTimeMillis, measurementNames,
                        metaData);
            }
        }
    }


    @Override
    public void saveMeasurements(final EntityMeasurementsInfo measurementInfo,
                    final long[] measurements, final long timeStampMillis, final int sampleTimeMillis)
            throws IOException {
        String groupName = measurementInfo.getMeasuredEntity().toString();
        alocateMeasurements(groupName, measurementInfo, sampleTimeMillis);
        database.write(timeStampMillis, groupName, measurements);
    }

    @PreDestroy
    @Override
    public void close() throws IOException {
        if (future != null) {
            future.cancel(false);
        }
        database.close();
    }

    private static String fixName(final String name) {
        StringBuilder result = new StringBuilder(name.length());
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (Character.isJavaIdentifierPart(c)) {
                result.append(c);
            }
        }
        return result.toString();
    }

    @Override
    public List<String> generateCharts(final int width, final int height) throws IOException {
        long startTime = ManagementFactory.getRuntimeMXBean().getStartTime();
        long endTime = System.currentTimeMillis();
        return generateCharts(startTime, endTime, width, height);
    }

    /**
     * Quantized recorders will have min, max avg charts and distribution charts
     * generated. Counting recorders will have simple charts generated.
     *
     * @param startTimeMillis
     * @param endTimeMillis
     * @param width
     * @param height
     * @return
     * @throws IOException
     */
    @Override
    public List<String> generateCharts(final long startTimeMillis, final long endTimeMillis,
            final int width, final int height) throws IOException {
        try {
            database.flush();
            List<String> result = new ArrayList<String>();
            Collection<TSTable> columnsInfo = database.getTSTables();
            for (TSTable info : columnsInfo) {
                Pair<long[], long[][]> data = database.read(info.getTableName(), startTimeMillis, endTimeMillis);
                if (data.getFirst().length > 0) {
                    if (canGenerateMinMaxAvgCount(info)) {
                        result.add(generateMinMaxAvgCountChart(info, data, width, height));
                    }
                    if (canGenerateHeatChart(info)) {
                        result.add(generateHeatChart(info, data, width, height));
                    }
                }
            }
            Multimap<String, TSTable> counters = getCounters(columnsInfo);
            for (Map.Entry<String, Collection<TSTable>> entry : counters.asMap().entrySet()) {
                long[][] timestamps = new long[entry.getValue().size()][];
                double[][] cdata = new double[entry.getValue().size()][];
                double[][] cdata2 = new double[entry.getValue().size()][];
                int i = 0;
                String[] measurementNames = new String[cdata.length];
                String[] measurementNames2 = new String[cdata2.length];
                String uom1 = "count";
                String uom2 = "";
                for (TSTable colInfo : entry.getValue()) {
                    Pair<long[], long[][]> data = database.read(colInfo.getTableName(), startTimeMillis, endTimeMillis);
                    timestamps[i] = data.getFirst();
                    cdata[i] = Arrays.getColumnAsDoubles(data.getSecond(), colInfo.getColumnIndex("count"));
                    cdata2[i] = Arrays.getColumnAsDoubles(data.getSecond(), colInfo.getColumnIndex("total"));
                    measurementNames[i] = colInfo.getTableName() + ".count";
                    measurementNames2[i] = colInfo.getTableName() + ".total";
                    uom2 = new String(colInfo.getTableMetaData(), Charsets.UTF_8);
                    i++;
                }
                result.add(generateCountChart(entry.getKey(), timestamps, measurementNames,
                        measurementNames2, uom1, uom2, cdata, cdata2, width, height));
            }
            LOG.info("Generated charts {}", result);
            return result;
        } catch (IOException ex) {
            LOG.error("Error while generating charts", ex);
            throw ex;
        } catch (RuntimeException ex) {
            LOG.error("Error while generating charts", ex);
            throw ex;
        }
    }

    private static Multimap<String, TSTable> getCounters(final Collection<TSTable> columnInfos) {
        Multimap<String, TSTable> result = HashMultimap.create();
        for (TSTable info : columnInfos) {
            if (isCounterOnly(info)) {
                String groupName = info.getTableName();
                if (groupName.startsWith("(")) {
                    int cidx = groupName.indexOf(',');
                    if (cidx > 0) {
                        groupName = groupName.substring(1, cidx);
                    }
                }
                result.put(groupName, info);
            }
        }
        return result;
    }

    public static boolean isCounterOnly(final TSTable info) {
        String[] columns = info.getColumnNames();
        return columns.length == 2 && columns[0].equals("count")
                && columns[1].equals("total");
    }

    public static boolean canGenerateMinMaxAvgCount(final TSTable info) {
        return ((info.getColumnIndex("min") >= 0)
                && (info.getColumnIndex("max") >= 0)
                && (info.getColumnIndex("total") >= 0)
                && (info.getColumnIndex("count") >= 0));
    }

    public static boolean canGenerateCount(final TSTable info) {
        return ((info.getColumnIndex("count") >= 0));
    }
    
    
    public static boolean canGenerateHeatChart(final TSTable info) {
        for (String mname : info.getColumnNames()) {
            if (mname.startsWith("Q") && mname.contains("_")) {
                return true;
            }
        }
        return false;
    }

    private String generateMinMaxAvgCountChart(
            final TSTable info, final Pair<long[], long[][]> data,
            final int width, final int height) throws IOException {
        long[][] vals = data.getSecond();
        double[] min = Arrays.getColumnAsDoubles(vals, info.getColumnIndex("min"));
        double[] max = Arrays.getColumnAsDoubles(vals, info.getColumnIndex("max"));
        double[] total = Arrays.getColumnAsDoubles(vals, info.getColumnIndex("total"));
        double[] count = Arrays.getColumnAsDoubles(vals, info.getColumnIndex("count"));
        for (int i = 0; i < count.length; i++) {
            if (count[i] == 0) {
                min[i] = 0;
                max[i] = 0;
            }
        }
        long[] timestamps = data.getFirst();
        BufferedImage combined = Charts.createMinMaxAvgCountImg("Measurements for "
                + info.getTableName() + " generated by spf4j",
                timestamps, min, max, total, count, new String(info.getTableMetaData(), Charsets.UTF_8), width, height);
        File dbFile = new File(database.getDBFilePath());
        File graphicFile = File.createTempFile(dbFile.getName() + "_" + fixName(info.getTableName()), ".mmac.png",
                dbFile.getParentFile());
        ImageIO.write(combined, "png", graphicFile);
        return graphicFile.getPath();
    }

    private String generateCountChart(
            final String groupName, final long[][] timestamps,
            final String[] measurementNames, final String[] measurementNames2,
            final String uom1, final String uom2,
            final double[][] measurements, final double[][] measurements2,
            final int width, final int height) throws IOException {
        BufferedImage count = Charts.createTimeSeriesJFreeChart("Measurements for "
                + groupName + " generated by spf4j",
                timestamps, measurementNames, uom1, measurements).createBufferedImage(width, height / 2);

        BufferedImage total = Charts.createTimeSeriesJFreeChart(null,
                timestamps, measurementNames2, uom2, measurements2).createBufferedImage(width, height / 2);

        BufferedImage combined = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        combined.getGraphics().drawImage(count, 0, 0, null);
        combined.getGraphics().drawImage(total, 0, height / 2, null);


        File dbFile = new File(database.getDBFilePath());
        File graphicFile = File.createTempFile(dbFile.getName() + "_" + fixName(groupName), ".count.png",
                dbFile.getParentFile());
        ImageIO.write(combined, "png", graphicFile);
        return graphicFile.getPath();
    }


    private String generateHeatChart(final TSTable info, final Pair<long[], long[][]> data,
            final int width, final int height) throws IOException {
        JFreeChart chart = TimeSeriesDatabase.createHeatJFreeChart(data, info);
        BufferedImage img = chart.createBufferedImage(width, height);
        File dbFile = new File(database.getDBFilePath());
        File graphicFile = File.createTempFile(dbFile.getName() + "_" + fixName(info.getTableName()), ".dist.png",
                dbFile.getParentFile());
        ImageIO.write(img, "png", graphicFile);
        return graphicFile.getAbsolutePath();
    }

    @Override
    public List<String> generate(final Properties props) throws IOException {
        int width = Integer.parseInt(props.getProperty("width", "1200"));
        int height = Integer.parseInt(props.getProperty("height", "800"));
        long startTime = Long.parseLong(props.getProperty("startTime",
                Long.toString(new LocalDate().withDayOfWeek(DateTimeConstants.MONDAY).toDate().getTime())));
        long endTime = Long.parseLong(props.getProperty("endTime", Long.toString(System.currentTimeMillis())));
        return generateCharts(startTime, endTime, width, height);
    }

    @Override
    public List<String> getParameters() {
        return java.util.Arrays.asList("width", "height", "startTime", "endTime");
    }

}
