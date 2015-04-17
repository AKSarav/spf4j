
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

package org.spf4j.jmx;

import java.lang.annotation.Annotation;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nullable;
import javax.management.InstanceAlreadyExistsException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;
import org.spf4j.base.Reflections;
import org.spf4j.base.Strings;

public final class Registry {

    private Registry() { }

    private static final MBeanServer MBEAN_SERVER = ManagementFactory.getPlatformMBeanServer();

    private static final Map<ObjectName, Object> REGISTERED = new HashMap<>();

    public static synchronized Object registerMBean(final ObjectName objectName, final Object mbean) {
        Object replaced = null;
        if (MBEAN_SERVER.isRegistered(objectName)) {
            try {
                replaced = REGISTERED.remove(objectName);
                MBEAN_SERVER.unregisterMBean(objectName);
            } catch (InstanceNotFoundException | MBeanRegistrationException ex) {
                throw new RuntimeException(ex);
            }
        }
        try {
            MBEAN_SERVER.registerMBean(mbean, objectName);
        } catch (InstanceAlreadyExistsException | MBeanRegistrationException | NotCompliantMBeanException ex) {
            throw new RuntimeException(ex);
        }
        REGISTERED.put(objectName, mbean);
        return replaced;
    }

    public static void registerMBean(final String domain, final String name, final Object object) {
        Registry.registerMBean(ExportedValuesMBean.createObjectName(domain, name), object);
    }

    public static Object unregister(final Object object) {
        final Class<? extends Object> aClass = object.getClass();
        return unregister(aClass.getPackage().getName(), aClass.getSimpleName());
    }

    public static Object unregister(final Class<?> object) {
        return unregister(object.getPackage().getName(), object.getSimpleName());
    }

    public static Object unregister(final String packageName, final String mbeanName) {
        ObjectName objectName = ExportedValuesMBean.createObjectName(packageName, mbeanName);
        return unregister(objectName);
    }

    public static synchronized Object unregister(final ObjectName objectName) {
        Object result = null;
        if (MBEAN_SERVER.isRegistered(objectName)) {
            try {
                result = REGISTERED.remove(objectName);
                MBEAN_SERVER.unregisterMBean(objectName);
            } catch (InstanceNotFoundException | MBeanRegistrationException ex) {
                throw new RuntimeException(ex);
            }
        }
        return result;
    }

    public static void export(final Object object) {
        final Class<? extends Object> aClass = object.getClass();
        export(aClass.getPackage().getName(), aClass.getSimpleName(), object);
    }

    public static void export(final Class<?> object) {
        export(object.getPackage().getName(), object.getSimpleName(), object);
    }

    public static synchronized ExportedValuesMBean export(final String packageName, final String mbeanName,
            final Object ... objects) {

        ObjectName objectName = ExportedValuesMBean.createObjectName(packageName, mbeanName);
        ExportedValuesMBean existing = (ExportedValuesMBean) unregister(objectName);
        Map<String, ExportedValueImpl> exportedAttributes = new HashMap<>();
        Map<String, ExportedOperationImpl> exportedOps = new HashMap<>();
        for (Object object : objects) {

            if (object instanceof Class) {
                for (Method method : ((Class<?>) object).getMethods()) {
                    if (Modifier.isStatic(method.getModifiers())) {
                        Annotation [] annotations = method.getAnnotations();
                        for (Annotation annot : annotations) {
                            if (annot.annotationType() == JmxExport.class) {
                                exportMethod(method, null, exportedAttributes, exportedOps, annot);
                            }
                        }
                    }
                }
            } else {
                final Class<? extends Object> oClass = object.getClass();
                for (Method method : oClass.getMethods()) {
                    Annotation [] annotations = method.getAnnotations();
                    for (Annotation annot : annotations) {
                        if (annot.annotationType() == JmxExport.class) {
                            exportMethod(method, object, exportedAttributes, exportedOps, annot);
                        }
                    }
                }
            }

        }
        if (exportedAttributes.isEmpty() && exportedOps.isEmpty()) {
            return null;
        }
        ExportedValue<?> [] values = new ExportedValue[exportedAttributes.size()];
        int i = 0;
        for (ExportedValueImpl expVal : exportedAttributes.values()) {
            if (expVal.isValid()) {
                values[i++] = expVal;
            } else {
                throw new IllegalArgumentException("If setter is exported, getter must be exported as well " + expVal);
            }
        }
        ExportedValuesMBean mbean;
        if (existing == null) {
            mbean = new ExportedValuesMBean(objectName, values,
                        exportedOps.values().toArray(new ExportedOperation [exportedOps.size()]));
        } else {
            mbean = new ExportedValuesMBean(existing, values,
                        exportedOps.values().toArray(new ExportedOperation [exportedOps.size()]));
        }
        Registry.registerMBean(mbean.getObjectName(), mbean);
        return mbean;
    }

    private static void exportMethod(final Method method,
            @Nullable final Object object, final Map<String, ExportedValueImpl> exportedAttributes,
            final Map<String, ExportedOperationImpl> exportedOps, final Annotation annot) {
        method.setAccessible(true); // this is to speed up invocation
        String methodName = method.getName();
        int nrParams = method.getParameterTypes().length;
        if (methodName.startsWith("get") && nrParams == 0) {
            String valueName = methodName.substring("get".length());
            valueName = Strings.withFirstCharLower(valueName);
            addGetter(valueName, exportedAttributes, annot, method, object);
        } else if (methodName.startsWith("is") && nrParams == 0) {
            String valueName = methodName.substring("is".length());
            valueName = Strings.withFirstCharLower(valueName);
            addGetter(valueName, exportedAttributes, annot, method, object);
        } else if (methodName.startsWith("set") && nrParams == 1) {
            addSetter(methodName, exportedAttributes, method, object, annot);
        } else {
            String opName = methodName;
            String nameOverwrite = (String) Reflections.getAnnotationAttribute(annot, "value");
            if (!"".equals(nameOverwrite)) {
                opName = nameOverwrite;
            }
            ExportedOperationImpl existing = exportedOps.put(opName, new ExportedOperationImpl(opName,
                    (String) Reflections.getAnnotationAttribute(annot, "description"), method, object));
            if (existing != null) {
                throw new IllegalArgumentException("exporting operations with same name not supported: " + opName);
            }
        }
    }

    private static void addSetter(final String methodName, final Map<String, ExportedValueImpl> exportedAttributes,
            final Method method, final Object object, final Annotation annot) {
        String customName = (String) Reflections.getAnnotationAttribute(annot, "value");
        String valueName;
        if ("".equals(customName)) {
            valueName = methodName.substring("set".length());
            valueName = Strings.withFirstCharLower(valueName);
        } else {
            valueName = customName;
        }

        ExportedValueImpl existing = exportedAttributes.get(valueName);
        if (existing == null) {
            existing = new ExportedValueImpl(valueName, null,
                    null, method, object, method.getParameterTypes()[0]);
        } else {
            if (existing.getValueClass() != method.getParameterTypes()[0]) {
                throw new IllegalArgumentException(
                        "Getter and setter icorrectly defined " + existing + " " + method);
            }
            existing = existing.withSetter(method);
        }
        exportedAttributes.put(valueName, existing);
    }

    private static void addGetter(final String pvalueName,
            final Map<String, ExportedValueImpl> exported,
            final Annotation annot, final Method method, final Object object) {
        String customName = (String) Reflections.getAnnotationAttribute(annot, "value");
        String valueName = "".equals(customName) ? pvalueName : customName;
        ExportedValueImpl existing = exported.get(valueName);
        if (existing == null) {
            existing = new ExportedValueImpl(
                    valueName, (String) Reflections.getAnnotationAttribute(annot, "description"),
                    method, null, object, method.getReturnType());
        } else {
            if (existing.getValueClass() != method.getReturnType()) {
                throw new IllegalArgumentException(
                        "Getter and setter icorrectly defined " + existing + " " + method);
            }
            existing = existing.withGetter(method,
                    (String) Reflections.getAnnotationAttribute(annot, "description"));
        }
        exported.put(valueName, existing);
    }
}
