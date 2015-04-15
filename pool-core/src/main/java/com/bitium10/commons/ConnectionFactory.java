package com.bitium10.commons;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * <b>项目名</b>： com.bitium10.commons <br>
 * <b>包名称</b>： com.bitium10.commons <br>
 * <b>类名称</b>： ConnectionFactory <br>
 * <b>类描述</b>： <br>
 * <b>创建人</b>： <a href="mailto:shouli1990@gmail.com">李朋明</a> <br>
 * <b>修改人</b>： <br>
 * <b>创建时间</b>：2014/10/5 21:53
 * <b>修改时间</b>： <br>
 * <b>修改备注</b>： <br>
 *
 * @version 1.0.0 <br>
 */
public class ConnectionFactory {
    private static Map<String, CP> poolCache = new HashMap();

    private static ReadWriteLock rwl = new ReentrantReadWriteLock();

    public static CP getCPInstance() throws SQLException {
        return getCPInstance("jdbc");
    }

    public static CP getCPInstance(String jdbc) throws SQLException {
        CP cp = (CP) poolCache.get(jdbc);
        if (cp == null) {
            cp = maybeInit(jdbc);
        }
        return cp;
    }

    private static CP maybeInit(String jdbc) throws SQLException {
        rwl.readLock().lock();
        CP cp = (CP) poolCache.get(jdbc);
        try {
            if (cp == null) {
                rwl.readLock().unlock();
                rwl.writeLock().lock();
                cp = (CP) poolCache.get(jdbc);
                try {
                    if (cp == null) {
                        cp = new CP(jdbc);
                        poolCache.put(jdbc, cp);
                    }
                } finally {
                    rwl.readLock().lock();
                }
            }
        } finally {
            rwl.readLock().unlock();
        }

        return cp;
    }

    public static Connection getConnection() throws SQLException {
        return getConnection("jdbc");
    }

    @Deprecated
    public static Connection getConnection(Connection conn) throws SQLException {
        return getConnection(conn, false);
    }

    @Deprecated
    public static Connection getConnection(Connection conn, boolean autoCommit) throws SQLException {
        conn.setAutoCommit(autoCommit);
        return conn;
    }

    public static Connection getConnection(String jdbc) throws SQLException {
        CP cp = getCPInstance(jdbc);
        return cp.getConnection();
    }

    public static Connection getConnection(boolean autoCommit) throws SQLException {
        return getConnection("jdbc", autoCommit);
    }

    public static Connection getConnection(String jdbc, boolean autoCommit) throws SQLException {
        CP cp = getCPInstance(jdbc);
        return cp.getConnection(autoCommit);
    }

    public static void shutdown() {
        shutdown("jdbc");
    }

    public static synchronized void shutdown(String jdbc) {
        CP cp = remove(jdbc);
        if (cp != null)
            cp.shutdown();
    }

    public static CP remove(String jdbc) {
        return (CP) poolCache.remove(jdbc);
    }
}