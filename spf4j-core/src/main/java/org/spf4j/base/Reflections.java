package org.spf4j.base;

import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nonnull;
import org.spf4j.concurrent.UnboundedRacyLoadingCache;

public final class Reflections {

    private Reflections() {
    }

    private static final Map<Class<?>, Class<?>> PRIMITIVE_MAP = new HashMap<Class<?>, Class<?>>(8);

    static {
        PRIMITIVE_MAP.put(boolean.class, Boolean.class);
        PRIMITIVE_MAP.put(byte.class, Byte.class);
        PRIMITIVE_MAP.put(char.class, Character.class);
        PRIMITIVE_MAP.put(short.class, Short.class);
        PRIMITIVE_MAP.put(int.class, Integer.class);
        PRIMITIVE_MAP.put(long.class, Long.class);
        PRIMITIVE_MAP.put(float.class, Float.class);
        PRIMITIVE_MAP.put(double.class, Double.class);
    }

    public static Method getCompatibleMethod(final Class<?> c,
            final String methodName,
            final Class<?>... paramTypes) {
        Method[] methods = c.getMethods();
        for (Method m : methods) {

            if (!m.getName().equals(methodName)) {
                continue;
            }

            Class<?>[] actualTypes = m.getParameterTypes();
            if (actualTypes.length > paramTypes.length) {
                continue;
            }

            boolean found = true;
            int last = actualTypes.length - 1;
            for (int j = 0; j < actualTypes.length; j++) {
                final Class<?> actType = actualTypes[j];
                final Class<?> paramType = paramTypes[j];
                found = canAssign(actType, paramType);
                if (!found && j == last) {
                    if (actType.isArray()) {
                        boolean matchvararg = true;
                        Class<?> varargType = actType.getComponentType();
                        for (int k = j; k < paramTypes.length; k++) {
                            if (!canAssign(varargType, paramTypes[k])) {
                                matchvararg = false;
                                break;
                            }
                        }
                        found = matchvararg;
                    }
                } else if (j == last && actualTypes.length < paramTypes.length) {
                    found = false;
                }
                if (!found) {
                    break;
                }
            }

            if (found) {
                return m;
            }
        }
        return null;
    }
    
    
    public static Object invoke(final Method m, final Object object, final Object [] parameters)
            throws IllegalAccessException, InvocationTargetException {
        int np = parameters.length;
        if (np > 0) {
            Class<?> [] actTypes = m.getParameterTypes();
            Class<?> lastParamClass =  actTypes[actTypes.length - 1];
            if (Reflections.canAssign(parameters[np - 1].getClass(), lastParamClass)) {
                return m.invoke(object, parameters);
            } else if (lastParamClass.isArray()) {
                int lidx = actTypes.length - 1;
                int l = np - lidx;
                Object array = Array.newInstance(lastParamClass.getComponentType(), l);
                for (int k = 0; k < l; k++) {
                    Array.set(array, k, parameters[lidx + k]);
                }
                Object [] newParams  = new Object [actTypes.length];
                System.arraycopy(parameters, 0, newParams, 0, lidx);
                newParams[lidx] = array;
                return m.invoke(object, newParams);
            } else {
                throw new IllegalStateException();
            }
        } else {
            return m.invoke(object);
        }
    }
    
    

    public static boolean canAssign(final Class<?> to, final Class<?> from) {
        boolean found = true;
        if (!to.isAssignableFrom(from)) {
            if (to.isPrimitive()) {
                Class<?> boxed = PRIMITIVE_MAP.get(to);
                found = boxed.equals(from);
            } else if (from.isPrimitive()) {
                found = PRIMITIVE_MAP.get(from).equals(to);
            } else {
                found = false;
            }
        }
        return found;
    }

    static final class MethodDesc {

        @Nonnull
        private final Class<?> clasz;
        @Nonnull
        private final String name;
        @Nonnull
        private final Class<?>[] paramTypes;

        public MethodDesc(final Class<?> clasz, final String name, final Class<?>[] paramTypes) {
            this.clasz = clasz;
            this.name = name;
            this.paramTypes = paramTypes;
        }

        public Class<?> getClasz() {
            return clasz;
        }

        public String getName() {
            return name;
        }

        public Class<?>[] getParamTypes() {
            return paramTypes;
        }

        @Override
        public int hashCode() {
            return 47 * this.clasz.hashCode() + this.name.hashCode();
        }

        @Override
        public boolean equals(final Object obj) {
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final MethodDesc other = (MethodDesc) obj;
            if (!this.clasz.equals(other.clasz)) {
                return false;
            }
            if (!this.name.equals(other.name)) {
                return false;
            }
            return (Arrays.deepEquals(this.paramTypes, other.paramTypes));
        }

        @Override
        public String toString() {
            return "MethodDesc{" + "clasz=" + clasz + ","
                    + " name=" + name + ", paramTypes=" + Arrays.toString(paramTypes) + '}';
        }

    }

    private static final LoadingCache<MethodDesc, Method> CACHE_FAST
            = new UnboundedRacyLoadingCache<MethodDesc, Method>(64,
                    new CacheLoader<MethodDesc, Method>() {
                        @Override
                        public Method load(final MethodDesc k) throws Exception {
                            final Method m = getCompatibleMethod(k.getClasz(), k.getName(), k.getParamTypes());
                            AccessController.doPrivileged(new PrivilegedAction() {
                                @Override
                                public Object run() {
                                    m.setAccessible(true);
                                    return null; // nothing to return
                                }
                            });
                            m.setAccessible(true);
                            return m;
                        }
                    });

    public static Method getCompatibleMethodCached(final Class<?> c,
            final String methodName,
            final Class<?>... paramTypes) {
        return CACHE_FAST.getUnchecked(new MethodDesc(c, methodName, paramTypes));
    }

}
