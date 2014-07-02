
package org.spf4j.jmx;


import com.google.common.base.Throwables;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;
import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.AttributeNotFoundException;
import javax.management.DynamicMBean;
import javax.management.InvalidAttributeValueException;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanException;
import javax.management.MBeanInfo;
import javax.management.MBeanOperationInfo;
import javax.management.MBeanParameterInfo;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.ReflectionException;
import org.spf4j.base.Reflections;


class ExportedValuesMBean implements DynamicMBean {

  
    private static final Pattern INVALID_CHARS = Pattern.compile("[^a-zA-Z0-9_\\-\\.]");

    private final Map<String, ExportedValue<?>> exportedValues;
    
    private final Map<String, ExportedOperation> exportedOperations;

    private final ObjectName objectName;

    private final MBeanInfo beanInfo;


    ExportedValuesMBean(final String domain, final String name,
            final ExportedValue<?> [] exported, final ExportedOperation [] operations) {
        this.exportedValues = new HashMap<String, ExportedValue<?>>(exported.length);
        for (ExportedValue<?> val : exported) {
            this.exportedValues.put(val.getName(), val);
        }
        this.exportedOperations = new HashMap<String, ExportedOperation>(exported.length);
        for (ExportedOperation op : operations) {
            this.exportedOperations.put(op.getName(), op);
        }
        this.objectName = createObjectName(domain, name);
        this.beanInfo = createBeanInfo();
    }

    /**
     * Returns the object name built from the {@link com.netflix.servo.monitor.MonitorConfig}.
     */
    public ObjectName getObjectName() {
        return objectName;
    }

    /** {@inheritDoc} */
    @Override
    public Object getAttribute(final String name) throws AttributeNotFoundException {
        ExportedValue<?> result = exportedValues.get(name);
        if (result == null) {
            throw new AttributeNotFoundException(name);
        }
        return result.get();
    }

    /** {@inheritDoc} */
    @Override
    public void setAttribute(final Attribute attribute)
            throws InvalidAttributeValueException, MBeanException, AttributeNotFoundException {
        String name = attribute.getName();
        ExportedValue<Object> result = (ExportedValue<Object>) exportedValues.get(name);
        if (result == null) {
            throw new AttributeNotFoundException(name);
        }
        result.set(attribute.getValue());
    }

    /** {@inheritDoc} */
    @Override
    public AttributeList getAttributes(final String[] names) {
        AttributeList list = new AttributeList(names.length);
        for (String name : names) {
            list.add(new Attribute(name, exportedValues.get(name).get()));
        }
        return list;
    }

    /** {@inheritDoc} */
    @Override
    public AttributeList setAttributes(final AttributeList list) {
        AttributeList result = new AttributeList(list.size());
        for (Attribute attr : list.asList()) {
            ExportedValue<Object> eval = (ExportedValue<Object>) exportedValues.get(attr.getName());
            if (eval != null) {
                try {
                    eval.set(attr.getValue());
                    result.add(attr);
                } catch (InvalidAttributeValueException ex) {
                    throw new RuntimeException(ex);
                }
            }
        }
        return result;
    }

    /** {@inheritDoc} */
    @Override
    public Object invoke(final String name, final Object[] args, final String[] sig)
            throws MBeanException, ReflectionException {
        return exportedOperations.get(name).invoke(args);
    }

    /** {@inheritDoc} */
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
            throw Throwables.propagate(e);
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
            MBeanParameterInfo [] paramInfos = op.getParameterInfos();
            operations[i++] = new MBeanOperationInfo(op.getName(), op.getDescription(),
                    paramInfos, op.getReturnType().getName(), MBeanOperationInfo.UNKNOWN);
        }
        return new MBeanInfo(
                this.getClass().getName(),
                "JmxMBean",
                attrs,
                null,  // constructors
                operations,  // operations
                null); // notifications
    }

    private static MBeanAttributeInfo createAttributeInfo(final ExportedValue<?> val) {
        Class<?> valClass = Reflections.primitiveToWrapper(val.getValueClass());
        final String type = Number.class.isAssignableFrom(valClass)
            ? Number.class.getName()
            : String.class.getName();
        return new MBeanAttributeInfo(
            val.getName(),
            type,
            val.getDescription(),
            true,   // isReadable
            val.isWriteable(),  // isWritable
            false); // isIs
    }
}
