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
package org.spf4j.pool.jdbc;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import org.spf4j.pool.ObjectCreationException;
import org.spf4j.pool.ObjectDisposeException;
import org.spf4j.pool.ObjectPool;

/**
 *
 * @author zoly
 */
public final class JdbcConnectionFactory  implements ObjectPool.Factory<Connection> {

    public JdbcConnectionFactory(final String driverName, final String url,
            final String user, final String password) {
        try {
            Class.forName(driverName);
        } catch (ClassNotFoundException ex) {
            throw new RuntimeException(ex);
        }
        this.url = url;
        this.password = password;
        this.user = user;       
    }
    
    private final String url;
    private final String user;
    private final String password;
    private ObjectPool<Connection> pool;
    
    
    @Override
    public Connection create() throws ObjectCreationException {
        final Connection conn;
        try {
            conn =  DriverManager.getConnection(url, user, password);
        } catch (SQLException ex) {
            throw new ObjectCreationException(ex);
        }
        
        return (Connection) Proxy.newProxyInstance(Thread.currentThread().getContextClassLoader(),
                new Class<?> [] {Connection.class}, new InvocationHandler() {

            private Exception ex;

            
            @Override
            public Object invoke(final Object proxy, final Method method, final Object[] args) throws Throwable {
                if (method.getName().equals("close")) {
                    pool.returnObject((Connection) proxy, ex);
                    ex = null;
                    return null;
                } else {
                    try {
                        return method.invoke(conn, args);
                    } catch (Exception e) {
                        ex = e;
                        throw e;
                    }
                }
            }
        });
        
    }

    @Override
    public void dispose(final Connection object) throws ObjectDisposeException {
        try {
            object.unwrap(Connection.class).close();
        } catch (SQLException ex) {
            throw new ObjectDisposeException(ex);
        }
    }

    @Override
    public Exception validate(final Connection object, final Exception e) {
        try {
            object.isValid(60);
            return null;
        } catch (SQLException ex) {
            return ex;
        }
    }

    @Override
    public void setPool(final ObjectPool<Connection> pool) {
        this.pool = pool;
    }
    
}
