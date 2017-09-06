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
package org.spf4j.jmx;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.reflect.TypeToken;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.File;
import java.io.InvalidObjectException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Future;
import javax.management.openmbean.OpenDataException;
import org.junit.Assert;
import org.junit.Test;
import org.spf4j.jmx.mappers.Spf4jOpenTypeMapper;
import org.spf4j.tsdb2.avro.ColumnDef;
import org.spf4j.tsdb2.avro.TableDef;
import org.spf4j.tsdb2.avro.Type;

/**
 *
 * @author Zoltan Farkas
 */

@SuppressFBWarnings({ "SE_BAD_FIELD_INNER_CLASS", "SIC_INNER_SHOULD_BE_STATIC_ANON" })
public class OpenTypeConverterTest {

  private final Spf4jOpenTypeMapper conv = new Spf4jOpenTypeMapper();

  @Test
  public void testConverter() {
    JMXBeanMapping mxBeanMapping = conv.apply(File.class);
    Assert.assertNull(mxBeanMapping);
  }

  @Test
  public void testConverter2() {
    JMXBeanMapping mxBeanMapping2 = conv.apply(ColumnDef[].class);
    Assert.assertNotNull(mxBeanMapping2);
  }

  @Test
  public void testConverterPrimArray() throws OpenDataException, InvalidObjectException {
    JMXBeanMapping mxBeanMapping2 = conv.apply(int[].class);
    Assert.assertNotNull(mxBeanMapping2);
    Object obj = mxBeanMapping2.toOpenValue(new int [] {1, 2, 3});
    Object fromOpenValue = mxBeanMapping2.fromOpenValue(obj);
    Assert.assertArrayEquals(new int [] {1, 2, 3}, (int[]) fromOpenValue);
  }

  @Test
  public void testConverter3() throws OpenDataException {
    JMXBeanMapping mxBeanMapping2 = conv.apply(TableDef[].class);
    Assert.assertNotNull(mxBeanMapping2);
    Object toOpenValue = mxBeanMapping2.toOpenValue(new TableDef[] {
      TableDef.newBuilder().setId(4).setDescription("bla").setName("name")
              .setSampleTime(10)
              .setColumns(Arrays.asList(ColumnDef.newBuilder().setName("bla").setType(Type.LONG)
                      .setDescription("bla").setUnitOfMeasurement("um").build())).build()
    });
    System.out.println(toOpenValue);
  }

  @Test
  public void testConverterSet() throws OpenDataException, InvalidObjectException {
    JMXBeanMapping mxBeanMapping2 = conv.apply((new TypeToken<Set<TableDef>>() {}).getType());
    Assert.assertNotNull(mxBeanMapping2);
    Object toOpenValue = mxBeanMapping2.toOpenValue(ImmutableSet.of(
      TableDef.newBuilder().setId(4).setDescription("bla").setName("name")
              .setSampleTime(10)
              .setColumns(Arrays.asList(ColumnDef.newBuilder().setName("bla").setType(Type.LONG)
                      .setDescription("bla").setUnitOfMeasurement("um").build())).build()
    ));
    System.out.println(toOpenValue);
    Object fromOpenValue = mxBeanMapping2.fromOpenValue(toOpenValue);
    System.out.println(fromOpenValue);
    Assert.assertTrue("must be set, not " + fromOpenValue.getClass(), fromOpenValue instanceof Set);
  }

  @Test
  public void testConverterList() throws OpenDataException, InvalidObjectException {
    JMXBeanMapping mxBeanMapping2 = conv.apply((new TypeToken<List<ColumnDef>>() {}).getType());
    Assert.assertNotNull(mxBeanMapping2);
    Object ov = mxBeanMapping2.toOpenValue(Arrays.asList(ColumnDef.newBuilder().setName("bla").setType(Type.LONG)
            .setDescription("bla").setUnitOfMeasurement("um").build()));
    System.out.println(ov);
    mxBeanMapping2.fromOpenValue(ov);
  }

  @Test
  public void testConverterFuture() throws OpenDataException, InvalidObjectException {
    JMXBeanMapping mxBeanMapping2 = conv.apply((new TypeToken<Future<Integer>>() {}).getType());
    Assert.assertNotNull(mxBeanMapping2);
  }


  @Test
  @SuppressFBWarnings("PRMC_POSSIBLY_REDUNDANT_METHOD_CALLS")
  public void testConverterMap() throws OpenDataException, InvalidObjectException {
    JMXBeanMapping mxBeanMapping2 = conv.apply((new TypeToken<Map<String, ColumnDef>>() {}).getType());
    Assert.assertNotNull(mxBeanMapping2);
    Object ov = mxBeanMapping2.toOpenValue(ImmutableMap.of("k1", ColumnDef.newBuilder().setName("bla").setType(Type.LONG)
            .setDescription("bla").setUnitOfMeasurement("um").build(),
            "K2",
            ColumnDef.newBuilder().setName("bla2").setType(Type.LONG)
                    .setDescription("bla").setUnitOfMeasurement("um").build()));
    System.out.println(ov);
    mxBeanMapping2.fromOpenValue(ov);
  }

}
