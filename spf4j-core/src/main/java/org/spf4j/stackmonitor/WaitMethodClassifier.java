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
package org.spf4j.stackmonitor;

import com.google.common.base.Predicate;
import java.util.HashSet;
import java.util.Set;
import org.spf4j.base.Pair;

/**
 *
 * @author zoly
 */
public final class WaitMethodClassifier implements Predicate<Method> {

    private WaitMethodClassifier() { }
    
    private static final Set<Pair<String, String>> WAIT_METHODS = new HashSet();

    static {
        WAIT_METHODS.add(Pair.of(sun.misc.Unsafe.class.getName(), "park"));
        WAIT_METHODS.add(Pair.of(java.lang.Object.class.getName(), "wait"));
        WAIT_METHODS.add(Pair.of(java.lang.Thread.class.getName(), "sleep"));
        WAIT_METHODS.add(Pair.of("java.net.PlainSocketImpl", "socketAccept"));
        WAIT_METHODS.add(Pair.of("java.net.PlainSocketImpl", "socketConnect"));
    }

   
    @Override
    public boolean apply(final Method input) {
       if (input == null) {
           return false;
       }
       return WAIT_METHODS.contains(Pair.of(input.getDeclaringClass(), input.getMethodName()));
    }
    
    public static final WaitMethodClassifier INSTANCE = new WaitMethodClassifier();
}
