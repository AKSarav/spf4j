
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
package com.zoltran.memorymonitor;

import com.google.common.base.Strings;

/**
 *
 * @author zoly
 */
public class TestClass {

    public static void testAllocInStaticContext() throws InterruptedException {
        for (int i = 0; i < 1000; i++) {
            System.err.println("TS" + i + Strings.repeat("A", i % 2 * (int) (Math.random() * 10)));
            if (i % 100 == 0) {
                Thread.sleep(500);
            }
        }
    }
    
}
