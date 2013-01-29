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
package com.zoltran.base;

import com.google.common.base.Throwables;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Field;
import java.security.AccessController;
import java.security.PrivilegedAction;
import javax.annotation.ParametersAreNonnullByDefault;

/**
 * Chainable exception class;
 *
 * @author zoly
 */
@ParametersAreNonnullByDefault
public class Exceptions {

    private static final Field field;

    static {
        try {
            field = Throwable.class.getDeclaredField("cause");
        } catch (NoSuchFieldException ex) {
            throw new RuntimeException(ex);
        } catch (SecurityException ex) {
            throw new RuntimeException(ex);
        }
        AccessController.doPrivileged(new PrivilegedAction() {
            @Override
            public Object run() {
                field.setAccessible(true);
                return null; // nothing to return
            }
        });

    }

    private static Throwable chain0(final Throwable t, final Throwable cause) {
        final Throwable rc = Throwables.getRootCause(t);
        try {
            AccessController.doPrivileged(new PrivilegedAction() {
                @Override
                public Object run() {
                    try {
                        field.set(rc, cause);
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
        return t;
    }

    /**
     * This method will clone the exception t and will set a new root cause.
     *
     * @param <T>
     * @param t
     * @param newRootCause
     * @return
     */
    public static <T extends Throwable> T chain(T t, Throwable newRootCause) {
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
            chain0(result, newRootCause);
            return result;
        } catch (ClassNotFoundException ex) {
            throw new RuntimeException(ex);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }
}
