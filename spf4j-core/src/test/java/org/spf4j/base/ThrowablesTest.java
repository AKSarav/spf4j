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

import java.net.SocketTimeoutException;
import java.sql.BatchUpdateException;
import java.util.concurrent.TimeoutException;
import org.junit.Assert;
import org.junit.Test;

/**
 *
 * @author zoly
 */
public final class ThrowablesTest {
    
    /**
     * Test of chain method, of class ExceptionChain.
     */
    @Test
    public void testChain() {
        System.out.println("chain");
        Throwable t = new RuntimeException("", new SocketTimeoutException("Boo timeout"));
        Throwable newRootCause = new TimeoutException("Booo");
        Throwable result = Throwables.chain(t, newRootCause);
        result.printStackTrace();
        Assert.assertEquals(newRootCause, com.google.common.base.Throwables.getRootCause(result));
        Assert.assertEquals(3, com.google.common.base.Throwables.getCausalChain(result).size());
        
    }
    
    @Test
    public void testChain2() {
        System.out.println("chain");
        Throwable t = new RuntimeException("bla1",
                new BatchUpdateException("Sql bla", "ORA-500", 500, new int[] {1, 2}, new RuntimeException("la la")));
        Throwable newRootCause = new TimeoutException("Booo");
        Throwable result = Throwables.chain(t, newRootCause);
        result.printStackTrace();
        Assert.assertArrayEquals(new int[] {1, 2}, ((BatchUpdateException) result.getCause()).getUpdateCounts());
        Assert.assertEquals(newRootCause, com.google.common.base.Throwables.getRootCause(result));
        Assert.assertEquals(4, com.google.common.base.Throwables.getCausalChain(result).size());
        
    }
    
    @Test
    public void testChain3() {
        Exception e = new RuntimeException();
        for (int i = 0; i < 100; i++) {
            e = Throwables.suppress(e, new RuntimeException());
        }
        Assert.assertEquals(200, com.google.common.base.Throwables.getCausalChain(e).size());
    
    }

}
