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

import com.google.common.util.concurrent.UncheckedExecutionException;
import java.io.InvalidObjectException;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;
import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.AttributeNotFoundException;
import javax.management.DynamicMBean;
import javax.management.ImmutableDescriptor;
import javax.management.InvalidAttributeValueException;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanInfo;
import javax.management.MBeanOperationInfo;
import javax.management.MBeanParameterInfo;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.openmbean.OpenDataException;
import javax.management.openmbean.OpenMBeanAttributeInfoSupport;
import javax.management.openmbean.OpenType;
import org.spf4j.base.Reflections;

public final class ExportedValuesMBean implements DynamicMBean {

  private static final Pattern INVALID_CHARS = Pattern.compile("[^a-zA-Z0-9_\\-\\.]");

  private final Map<String, ExportedValue<?>> exportedValues;

  private final Map<String, ExportedOperation> exportedOperations;

  private final ObjectName objectName;

  private final MBeanInfo beanInfo;

  ExportedValuesMBean(final ObjectName objectName,
          final ExportedValue<?>[] exported, final ExportedOperation[] operations) {
    this.exportedValues = new HashMap<>(exported.length);
    for (ExportedValue<?> val : exported) {
      this.exportedValues.put(val.getName(), val);
    }
    this.exportedOperations = new HashMap<>(operations.length);
    for (ExportedOperation op : operations) {
      this.exportedOperations.put(op.getName(), op);
    }
    this.objectName = objectName;
    this.beanInfo = createBeanInfo();
  }

  ExportedValuesMBean(final ExportedValuesMBean extend,
          final ExportedValue<?>[] exported, final ExportedOperation[] operations) {
    this.exportedValues = new HashMap<>(exported.length + extend.exportedValues.size());
    this.exportedValues.putAll(extend.exportedValues);
    for (ExportedValue<?> val : exported) {
      this.exportedValues.put(val.getName(), val);
    }
    this.exportedOperations = new HashMap<>(operations.length + extend.exportedOperations.size());
    this.exportedOperations.putAll(extend.exportedOperations);
    for (ExportedOperation op : operations) {
      this.exportedOperations.put(op.getName(), op);
    }
    this.objectName = extend.getObjectName();
    this.beanInfo = extend.beanInfo;
  }

  /**
   * @return - the object name of this mbean.
   */
  public ObjectName getObjectName() {
    return objectName;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Object getAttribute(final String name) throws AttributeNotFoundException {
    ExportedValue<?> result = exportedValues.get(name);
    if (result == null) {
      throw new AttributeNotFoundException(name);
    }
    try {
      return result.get();
    } catch (OpenDataException ex) {
      throw new UncheckedExecutionException(ex);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void setAttribute(final Attribute attribute)
          throws AttributeNotFoundException, InvalidAttributeValueException {
    String name = attribute.getName();
    ExportedValue<Object> result = (ExportedValue<Object>) exportedValues.get(name);
    if (result == null) {
      throw new AttributeNotFoundException(name);
    }
    try {
      result.set(attribute.getValue());
    } catch (InvalidObjectException ex) {
      InvalidAttributeValueException tex = new InvalidAttributeValueException("Invalid value " + attribute);
      tex.addSuppressed(ex);
      throw tex;
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public AttributeList getAttributes(final String[] names) {
    AttributeList list = new AttributeList(names.length);
    for (String name : names) {
      try {
        list.add(new Attribute(name, exportedValues.get(name).get()));
      } catch (OpenDataException ex) {
        throw new UncheckedExecutionException(ex);
      }
    }
    return list;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public AttributeList setAttributes(final AttributeList list) {
    AttributeList result = new AttributeList(list.size());
    for (Attribute attr : list.asList()) {
      ExportedValue<Object> eval = (ExportedValue<Object>) exportedValues.get(attr.getName());
      if (eval != null) {
        try {
          eval.set(attr.getValue());
          result.add(attr);
        } catch (InvalidAttributeValueException | InvalidObjectException ex) {
          throw new UncheckedExecutionException(ex);
        }
      }
    }
    return result;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Object invoke(final String name, final Object[] args, final String[] sig) {
    try {
      return exportedOperations.get(name).invoke(args);
    } catch (OpenDataException | InvalidObjectException ex) {
      throw new UncheckedExecutionException(ex);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public MBeanInfo getMBeanInfo() {
    return beanInfo;
  }

  public static ObjectName createObjectName(final String domain, final String name) {
    try {
      final String sanitizedDomain = INVALID_CHARS.matcher(domain).replaceAll("_");
      final String sanitizedName = INVALID_CHARS.matcher(name).replaceAll("_");
      StringBuilder builder = new StringBuilder();
      builder.append(sanitizedDomain).append(':');
      builder.append("name=").append(sanitizedName);
      return new ObjectName(builder.toString());
    } catch (MalformedObjectNameException e) {
      throw new IllegalArgumentException("Invalid name for " + domain + ", " + name, e);
    }
  }

  private MBeanInfo createBeanInfo() {
    MBeanAttributeInfo[] attrs = new MBeanAttributeInfo[exportedValues.size()];
    int i = 0;
    for (ExportedValue<?> val : exportedValues.values()) {
      attrs[i++] = createAttributeInfo(val);
    }
    MBeanOperationInfo[] operations = new MBeanOperationInfo[exportedOperations.size()];
    i = 0;
    for (ExportedOperation op : exportedOperations.values()) {
      MBeanParameterInfo[] paramInfos = op.getParameterInfos();
      String description = op.getDescription();
      if (description == null || description.isEmpty()) {
        description = op.getName();
      }
      OpenType<?> openType = op.getReturnOpenType();
      operations[i++] = new MBeanOperationInfo(op.getName(), description, paramInfos,
              op.getReturnType().getName(), 0, openType == null ? null
                      : new ImmutableDescriptor(new String[]{"openType", "originalType"},
            new Object[]{openType, op.getReturnType().getName()}));
    }
    return new MBeanInfo(objectName.toString(), "spf4j exported",
            attrs, null, operations, null);
  }

  private static MBeanAttributeInfo createAttributeInfo(final ExportedValue<?> val) {
    final Type oClass = val.getValueClass();
    Class<?> valClass = oClass instanceof Class ? Reflections.primitiveToWrapper((Class) oClass) : null;
    OpenType openType = val.getValueOpenType();
    String description = val.getDescription();
    if (description == null || description.isEmpty()) {
      description = val.getName();
    }
    if (openType != null) {
      return new OpenMBeanAttributeInfoSupport(val.getName(), description,
            openType, true, val.isWriteable(), valClass == Boolean.class);
    } else {
       return new MBeanAttributeInfo(
            val.getName(),
            oClass.getTypeName(),
            val.getDescription(),
            true, // isReadable
            val.isWriteable(), // isWritable
            valClass == Boolean.class);
    }

  }

  @Override
  public String toString() {
    return "ExportedValuesMBean{" + "exportedValues=" + exportedValues + ", exportedOperations="
            + exportedOperations + ", objectName=" + objectName + ", beanInfo=" + beanInfo + '}';
  }

}
