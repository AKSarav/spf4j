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

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import org.junit.Assert;
import org.junit.Test;

/**
 *
 * @author zoly
 */
public final class ReflectionsTest {


    private static final Class<?>[] params = new Class<?>[]{String.class, Integer.class};

    @Test
    public void testReflections() {
        Method reflect = Reflections.getCompatibleMethod(String.class, "indexOf", params);
        Method fastM = Reflections.getCompatibleMethodCached(String.class, "indexOf", params);
        Assert.assertEquals(reflect, fastM);

        Method method = Reflections.getMethod(String.class, "indexOf", int.class);
        Assert.assertEquals("indexOf", method.getName());
        method = Reflections.getMethod(String.class, "bla", char.class);
        Assert.assertNull(method);

        Constructor cons = Reflections.getConstructor(String.class, byte[].class);
        Assert.assertNotNull(cons);
        cons = Reflections.getConstructor(String.class, Pair.class);
        Assert.assertNull(cons);

    }

    public static String print(Object ... args) {
      return java.util.Arrays.toString(args);
    }

    public static void caca() {
      //nothing
    }

    public interface Printing {
      String print(Object ... args);

      void caca();
    }



    @Test
    public void testImplement() {
      Printing prt = Reflections.implementStatic(Printing.class, ReflectionsTest.class);
      String result = prt.print("a", 3);
      System.out.println(result);
      Assert.assertEquals("[a, 3]", result);
    }

}
