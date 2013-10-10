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
package org.spf4j.pool.impl;

import org.spf4j.pool.ObjectCreationException;
import org.spf4j.pool.ObjectDisposeException;
import org.spf4j.pool.ObjectPool;
import java.io.IOException;

/**
 *
 * @author zoly
 */
public final class ExpensiveTestObjectFactory implements ObjectPool.Factory<ExpensiveTestObject> {

    private final long maxIdleMillis;
    private final int nrUsesToFailAfter;
    private final long  minOperationMillis;
    private final long maxOperationMillis;

    public ExpensiveTestObjectFactory(final long maxIdleMillis, final int nrUsesToFailAfter,
            final long minOperationMillis, final long maxOperationMillis) {
        this.maxIdleMillis = maxIdleMillis;
        this.nrUsesToFailAfter = nrUsesToFailAfter;
        this.minOperationMillis = minOperationMillis;
        this.maxOperationMillis = maxOperationMillis;
    }

    public ExpensiveTestObjectFactory() {
        this(100, 10, 1, 20);
    }

    
    
    @Override
    public ExpensiveTestObject create() throws ObjectCreationException {
        return new ExpensiveTestObject(maxIdleMillis, nrUsesToFailAfter, minOperationMillis, maxOperationMillis);
    }

    @Override
    public void dispose(final ExpensiveTestObject object) throws ObjectDisposeException {
        try {
            object.close();
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public Exception validate(final ExpensiveTestObject object, final Exception e) {
       if (e != null && e instanceof IOException) {
           return e;
       } else {
            try {
                object.testObject();
                return null;
            } catch (IOException ex) {
                return ex;
            }
       }
    }

    @Override
    public void setPool(final ObjectPool<ExpensiveTestObject> pool) {
    }
    
 
}
