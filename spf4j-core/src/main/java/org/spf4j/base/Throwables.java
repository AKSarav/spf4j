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
package org.spf4j.base;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * utility class for throwables.
 *
 * @author zoly
 */
@ParametersAreNonnullByDefault
public final class Throwables {

    private Throwables() {
    }

    private static final Logger LOG = LoggerFactory.getLogger(Throwables.class);

    private static final int MAX_THROWABLE_CHAIN
            = Integer.parseInt(System.getProperty("throwables.max.chain", "200"));

    private static final Field CAUSE_FIELD;

    private static final Method ADD_SUPPRESSED;
    
    private static final Method GET_SUPPRESSED;

    static {
        CAUSE_FIELD = AccessController.doPrivileged(new PrivilegedAction<Field>() {
            @Override
            public Field run() {
                Field causeField;
                try {
                    causeField = Throwable.class.getDeclaredField("cause");
                } catch (NoSuchFieldException ex) {
                    throw new RuntimeException(ex);
                } catch (SecurityException ex) {
                    throw new RuntimeException(ex);
                }
                causeField.setAccessible(true);
                return causeField;
            }
        });

        ADD_SUPPRESSED = AccessController.doPrivileged(new PrivilegedAction<Method>() {
            @Override
            public Method run() {
                Method m;
                try {
                    m = Throwable.class.getDeclaredMethod("addSuppressed", Throwable.class);
                } catch (NoSuchMethodException ex) {
                    LOG.info("Throwables do not support suppression", ex);
                    m = null;
                } catch (SecurityException ex) {
                    throw new RuntimeException(ex);
                }
                if (m != null) {
                    m.setAccessible(true);
                }
                return m;
            }
        });
        
        GET_SUPPRESSED = AccessController.doPrivileged(new PrivilegedAction<Method>() {
            @Override
            public Method run() {
                Method m;
                try {
                    m = Throwable.class.getDeclaredMethod("getSuppressed");
                } catch (NoSuchMethodException ex) {
                    LOG.info("Throwables do not support suppression", ex);
                    m = null;
                } catch (SecurityException ex) {
                    throw new RuntimeException(ex);
                }
                if (m != null) {
                    m.setAccessible(true);
                }
                return m;
            }
        });

    }

    private static void chain0(final Throwable t, final Throwable cause) {
        final Throwable rc = com.google.common.base.Throwables.getRootCause(t);
        setCause(rc, cause);
    }

    private static void setCause(final Throwable rc, @Nullable final Throwable cause) {
        try {
            AccessController.doPrivileged(new PrivilegedAction() {
                @Override
                public Object run() {
                    try {
                        CAUSE_FIELD.set(rc, cause);
                    } catch (IllegalArgumentException ex) {
                        throw new RuntimeException(ex);
                    } catch (IllegalAccessException ex) {
                        throw new RuntimeException(ex);
                    }
                    return null; // nothing to return
                }
            });

        } catch (IllegalArgumentException ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * This method will clone the exception t and will set a new root cause.
     *
     * @param <T>
     * @param t
     * @param newRootCause
     * @return
     */
    public static <T extends Throwable> T chain(final T t, final Throwable newRootCause) {
        int chainedExNr = com.google.common.base.Throwables.getCausalChain(t).size();
        if (chainedExNr >= MAX_THROWABLE_CHAIN) {
            LOG.warn("Trimming exception", newRootCause);
            return t;
        }
        List<Throwable> newRootCauseChain = com.google.common.base.Throwables.getCausalChain(newRootCause);
        int newChainIdx = 0;
        if (chainedExNr + newRootCauseChain.size() > MAX_THROWABLE_CHAIN) {
            newChainIdx = newRootCauseChain.size() - (MAX_THROWABLE_CHAIN - chainedExNr);
            LOG.warn("Trimming exception at {} ", newChainIdx, newRootCause);
        }
        T result = clone(t);
        chain0(result, newRootCauseChain.get(newChainIdx));
        return result;
  
    }

    public static <T extends Serializable> T clone(final T t) {
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ObjectOutputStream out = new ObjectOutputStream(bos);
            try {
                out.writeObject(t);
            } finally {
                out.close();
            }
            T result;
            ObjectInputStream in = new ObjectInputStream(
                    new ByteArrayInputStream(bos.toByteArray()));
            try {
                result = (T) in.readObject();
            } finally {
                in.close();
            }
            return result;
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Functionality equivalent for java 1.7 Throwable.addSuppressed.
     *
     * @param <T>
     * @param t
     * @param suppressed
     * @return
     */
    public static <T extends Throwable> T suppress(final T t, final Throwable suppressed) {
        if (ADD_SUPPRESSED != null) {
            try {
                ADD_SUPPRESSED.invoke(t, suppressed);
                return t;
            } catch (IllegalAccessException ex) {
                throw new RuntimeException(ex);
            } catch (IllegalArgumentException ex) {
                throw new RuntimeException(ex);
            } catch (InvocationTargetException ex) {
                throw new RuntimeException(ex);
            }
        } else {
            if (suppressed == null) {
                throw new IllegalArgumentException("Cannot suppress null exception");
            }
            if (t == suppressed) {
                throw new IllegalArgumentException("Self suppression not permitted");
            }
            return chain(t, new SuppressedThrowable(suppressed));
        }
    }
    
    public static Throwable [] getSuppressed(final Throwable t) {
        if (GET_SUPPRESSED != null) {
            try {
                return (Throwable []) GET_SUPPRESSED.invoke(t);
            } catch (IllegalAccessException ex) {
                throw new RuntimeException(ex);
            } catch (IllegalArgumentException ex) {
                throw new RuntimeException(ex);
            } catch (InvocationTargetException ex) {
                throw new RuntimeException(ex);
            }
        } else {
            List<Throwable> chain = com.google.common.base.Throwables.getCausalChain(clone(t));
            List<Throwable> result = new ArrayList<Throwable>();
            Throwable prev = null;
            for (Throwable comp : chain) {
                if (comp instanceof SuppressedThrowable) {
                    if (prev != null) {
                        setCause(prev, null);
                    }
                    result.add(comp.getCause());
                }
                prev = comp;
            }
            return result.toArray(new Throwable [result.size()]);
        }
    }
    

}
