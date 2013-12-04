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

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.concurrent.TimeoutException;
import java.util.logging.Logger;
import javax.sql.DataSource;
import org.spf4j.pool.ObjectBorrowException;
import org.spf4j.pool.ObjectCreationException;
import org.spf4j.pool.ObjectPool;
import org.spf4j.pool.impl.ObjectPoolBuilder;

/**
 *
 * @author zoly
 */
public final class PooledDataSource implements DataSource {

    private final ObjectPool<Connection> pool;
    
    
    public PooledDataSource(final int coreSize, final int maxSize,
            final String driverName, final String url, final String user, final String password)
            throws ObjectCreationException {
        ObjectPoolBuilder<Connection, SQLException> builder =
                new ObjectPoolBuilder<Connection, SQLException>(
                maxSize, new JdbcConnectionFactory(driverName, url, user, password));
        pool = builder.build();
    }
    
    @Override
    public Connection getConnection() throws SQLException {
        try {
            return pool.borrowObject();
        } catch (InterruptedException ex) {
            throw new SQLException(ex);
        } catch (TimeoutException ex) {
            throw new SQLException(ex);
        } catch (ObjectBorrowException ex) {
            throw new SQLException(ex);
        } catch (ObjectCreationException ex) {
            throw new SQLException(ex);
        }
    }

    @Override
    public Connection getConnection(final String username, final String password) throws SQLException {
        throw new UnsupportedOperationException("Unsupported operation"); 
    }

    @Override
    public PrintWriter getLogWriter() throws SQLException {
        throw new UnsupportedOperationException("Unsupported operation");
    }

    @Override
    public void setLogWriter(final PrintWriter out) throws SQLException {
        throw new UnsupportedOperationException("Unsupported operation");
    }

    @Override
    public void setLoginTimeout(final int seconds) throws SQLException {
        throw new UnsupportedOperationException("Unsupported operation");
    }

    @Override
    public int getLoginTimeout() throws SQLException {
        throw new UnsupportedOperationException("Unsupported operation");
    }

    @Override
    public <T> T unwrap(final Class<T> iface) throws SQLException {
        if (iface.equals(DataSource.class) || iface.equals(PooledDataSource.class)) {
            return (T) this;
        } else {
            throw new SQLException("Not a wrapper for " + iface);
        }
    }

    @Override
    public boolean isWrapperFor(final Class<?> iface) throws SQLException {
        return iface.equals(DataSource.class) || iface.equals(PooledDataSource.class);
    }

    public Logger getParentLogger() throws SQLFeatureNotSupportedException {
        throw new UnsupportedOperationException("Not supported yet.");
    }
    
}
