package com.bitium10.commons;

import com.bitium10.commons.impl.CP;
import com.bitium10.commons.impl.CPConfigImpl;

import javax.naming.Context;
import javax.naming.Name;
import javax.naming.RefAddr;
import javax.naming.Reference;
import javax.naming.spi.ObjectFactory;
import javax.sql.DataSource;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Properties;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Logger;

/**
 * <b>项目名</b>： db-pool <br>
 * <b>包名称</b>： com.bitium10.commons <br>
 * <b>类名称</b>： CPDataSource <br>
 * <b>类描述</b>： <br>
 * <b>创建人</b>： <a href="mailto:wylipengming@chinabank.com.cn">李朋明</a> <br>
 * <b>修改人</b>： <br>
 * <b>创建时间</b>：2014/10/1 20:27
 * <b>修改时间</b>： <br>
 * <b>修改备注</b>： <br>
 *
 * @version 1.0.0 <br>
 */
public class CPDataSource extends CPConfigImpl implements DataSource, ObjectFactory {
    private ReadWriteLock rwl = new ReentrantReadWriteLock();

    private CP pool = null;
    private PrintWriter logWriter = null;

    public PrintWriter getLogWriter() throws SQLException {
        return this.logWriter;
    }

    public void setLogWriter(PrintWriter out) throws SQLException {
        this.logWriter = out;
    }

    @Override
    public Logger getParentLogger() throws SQLFeatureNotSupportedException {
        return null;
    }

    public <T> T unwrap(Class<T> iface)
            throws SQLException {
        return null;
    }

    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        return false;
    }

    public Object getObjectInstance(Object object, Name name, Context nameCtx, Hashtable<?, ?> environment) throws Exception {
        Reference ref = (Reference) object;
        Enumeration addrs = ref.getAll();
        Properties props = new Properties();
        while (addrs.hasMoreElements()) {
            RefAddr addr = (RefAddr) addrs.nextElement();
            if ((addr.getType().equals("driverClassName")) || (addr.getType().equals("driver"))) {
                Class.forName((String) addr.getContent());
            } else props.put(addr.getType(), addr.getContent());
        }

        CPDataSource ds = new CPDataSource();
        ds.setProperties(props);
        return ds;
    }

    public Connection getConnection() throws SQLException {
        if (this.pool == null) {
            maybeInit();
        }
        return this.pool.getConnection();
    }

    public Connection getConnection(String username, String password) throws SQLException {
        throw new UnsupportedOperationException("getConnection(username, password) is unsupported.");
    }

    private void maybeInit() throws SQLException {
        try {
            this.rwl.readLock().lock();
            if (this.pool == null) {
                this.rwl.readLock().unlock();
                this.rwl.writeLock().lock();
                try {
                    if (this.pool == null) {
                        this.pool = new CP(this);
                    }
                } finally {
                    this.rwl.readLock().lock();
                }
            }
        } finally {
            this.rwl.readLock().unlock();
        }
    }
}