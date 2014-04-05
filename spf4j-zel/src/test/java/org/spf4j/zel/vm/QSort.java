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
package org.spf4j.zel.vm;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import java.io.IOException;
import java.util.Random;
import org.junit.Test;

/**
 *
 * @author zoly
 */
public final class QSort {

    @Test
    public void test() throws CompileException, ZExecutionException, InterruptedException, IOException {
        String qsort = Resources.toString(Resources.getResource(QSort.class, "sort.zel"),
                Charsets.US_ASCII);
        Program p = Program.compile(qsort);
        System.out.println(p);
        p.execute();
    }
    
    @Test
    public void testSort() throws CompileException, ZExecutionException, InterruptedException, IOException {
       String qsort = Resources.toString(Resources.getResource(QSort.class, "sortFunc.zel"),
                Charsets.US_ASCII);
        Program p = Program.compile(qsort, "x");
        Integer [] testArray = new Integer [100000];
        Random random = new Random();
        for (int i = 0; i < testArray.length; i++) {
            testArray[i] = random.nextInt();
        }
        
        for (int i = 0; i < 3; i++) {
            long startTime = System.currentTimeMillis();
            p.execute(new Object [] {testArray.clone()});
            System.out.println("Parallel exec time = " + (System.currentTimeMillis() - startTime));
        }
        
        for (int i = 0; i < 3; i++) {
            long startTime = System.currentTimeMillis();
            p.executeSingleThreaded(new Object [] {testArray.clone()});
            System.out.println("ST exec time = " + (System.currentTimeMillis() - startTime));
        }
    }
    
}
