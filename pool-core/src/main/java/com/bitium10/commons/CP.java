package com.bitium10.commons;

import com.bitium10.commons.log.Logger;
import com.bitium10.commons.utils.Formatter;
import com.bitium10.commons.utils.JMXUtil;
import com.bitium10.commons.utils.JdbcUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.Map;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * <b>项目名</b>： db-pool <br>
 * <b>包名称</b>： com.bitium10.commons.impl <br>
 * <b>类名称</b>： CP <br>
 * <b>类描述</b>： <br>
 * <b>创建人</b>： <a href="mailto:wylipengming@chinabank.com.cn">李朋明</a> <br>
 * <b>修改人</b>： <br>
 * <b>创建时间</b>：2014/10/2 9:21
 * <b>修改时间</b>： <br>
 * <b>修改备注</b>： <br>
 *
 * @version 1.0.0 <br>
 */
public class CP implements ConnectPool {
    private static final Logger log = new Logger();

    private static final String[] classPaths = System.getProperty("java.class.path", "classes").split(System.getProperty("path.separator", ";"));

    private static final AtomicInteger POOL_ID = new AtomicInteger(0);
    private final int poolId;
    private String poolName;
    private final CPConfigImpl config;
    private final AtomicInteger connectionNo = new AtomicInteger(0);

    private final AtomicInteger validConnectionNum = new AtomicInteger(0);

    private final Map<Integer, PooledConnection> validConnectionsPool = new ConcurrentHashMap();

    private final LinkedStack<Integer> idleConnectionsId = new LinkedStack();

    private AtomicBoolean shutdown = new AtomicBoolean(false);

    private AtomicBoolean inited = new AtomicBoolean(false);
    private Thread monitor;
    private boolean configFromProperties = false;

    private BlockingQueue<NamedConnection> unclosedConnections = new LinkedBlockingQueue();

    public CP(String poolName) throws SQLException {
        this.config = new CPConfigImpl();
        this.config.setProperties(loadProperties(poolName));
        this.poolId = POOL_ID.getAndIncrement();
        this.poolName = poolName;
        this.configFromProperties = true;
        initPool();
    }

    public CP(CPConfigImpl config) throws SQLException {
        this.config = config;
        this.poolId = POOL_ID.getAndIncrement();
        this.poolName = ("UCP#" + this.poolId);
        this.configFromProperties = false;
        initPool();
    }

    private void initPool()
            throws SQLException {
        if ((this.config == null) || (this.config.getConnUrl() == null)) {
            throw new SQLException("jdbc.url cannot be NULL");
        }
        if (this.config.getDriver() != null) {
            try {
                Class.forName(this.config.getDriver());
                log.info(new Object[]{"load ", this.config.getDriver(), " ok"});
            } catch (ClassNotFoundException e) {
                throw new SQLException(e.toString(), e);
            }
        }
        boolean isOracle10 = (this.config.isOracle()) && (DriverManager.getDriver(this.config.getConnUrl()).getMajorVersion() == 10);
        if ((isOracle10) && (this.config.isUseOracleImplicitPSCache()))
            this.config.getConnectionProperties().setProperty("oracle.jdbc.FreeMemoryOnEnterImplicitCache", "true");
        else {
            this.config.getConnectionProperties().remove("oracle.jdbc.FreeMemoryOnEnterImplicitCache");
        }

        if (this.config.getCheckoutTimeoutMilliSec() > 0L) {
            if (this.config.isOracle())
                this.config.getConnectionProperties().setProperty("oracle.net.CONNECT_TIMEOUT", String.valueOf(this.config.getCheckoutTimeoutMilliSec()));
            else if (this.config.isMySQL()) {
                this.config.getConnectionProperties().setProperty("connectTimeout", String.valueOf(this.config.getCheckoutTimeoutMilliSec()));
            }
        }

        if (this.config.getQueryTimeout() > 0) {
            if (this.config.isOracle()) {
                this.config.getConnectionProperties().setProperty("oracle.jdbc.ReadTimeout", String.valueOf(this.config.getQueryTimeout() * 1000));
                this.config.getConnectionProperties().setProperty("oracle.net.READ_TIMEOUT", String.valueOf(this.config.getQueryTimeout() * 1000));
            } else if (this.config.isMySQL()) {
                this.config.getConnectionProperties().setProperty("socketTimeout", String.valueOf(this.config.getQueryTimeout() * 1000));
            }
        }
        if (this.inited.getAndSet(true)) {
            return;
        }
        if (!this.config.isLazyInit()) {
            if (this.validConnectionNum.get() < this.config.getMinConnections()) {
                newConnection(false);
            }
        }

        this.monitor = new CPMonitor();
        this.monitor.setName("CPM:" + this.poolName);
        this.monitor.setDaemon(true);
        this.monitor.start();

        if (this.config.getJmxLevel() > 0) {
            JMXUtil.register(getClass().getPackage().getName() + ":type=pool-" + this.poolName, this);
            JMXUtil.register(getClass().getPackage().getName() + ":type=pool-" + this.poolName + ",name=config", this.config);
        }
    }

    public boolean isShutdown() {
        return this.shutdown.get();
    }

    public void shutdown() {
        if (this.shutdown.getAndSet(true)) {
            return;
        }
        ConnectionFactory.remove(this.poolName);
        if (this.monitor != null) {
            this.monitor.interrupt();
            try {
                this.monitor.join();
            } catch (InterruptedException e) {
            }
        }
        for (int i = 0; i < 10; i++) {
            Integer[] connIds = this.idleConnectionsId.toArray();
            for (Integer connId : connIds) {
                PooledConnection pc = (PooledConnection) this.validConnectionsPool.get(connId);
                try {
                    pc.lock();
                } catch (InterruptedException e) {
                    continue;
                }
                try {
                    if (pc.isCheckOut()) {
                        pc.unlock();
                        break;
                    }
                    if (!closeConnection(pc)) {
                        pc.unlock();
                        break;
                    }
                } finally {
                    pc.unlock();
                }

            }
            if (this.validConnectionNum.get() <= 0) {
                break;
            }
            logVerboseInfo(true);
            this.idleConnectionsId.awaitNotEmpty(1L, TimeUnit.SECONDS);
        }
        for (Map.Entry e : this.validConnectionsPool.entrySet()) {
            PooledConnection pc = (PooledConnection) e.getValue();
            if (pc.isCheckOut()) {
                log.info(new Object[]{"force closing ... ", pc.getConnectionName(), " checkout by ", pc.getThreadCheckOut().getName(), " for " + pc.getCheckOutTime() + " ms[", pc.isBusying() ? "BUSYING" : "IDLE", "]"});
            }

            pc.close();
            this.validConnectionNum.decrementAndGet();
        }
        this.validConnectionsPool.clear();
        JMXUtil.unregister(getClass().getPackage().getName() + ":type=pool-" + this.poolName);
        JMXUtil.unregister(getClass().getPackage().getName() + ":type=pool-" + this.poolName + ",name=config");
    }

    public void reloadProperties() {
        if (this.configFromProperties) {
            Properties prop = loadProperties(this.poolName);
            this.config.setProperties(prop);
        }
    }

    private Properties loadProperties(String propfile) {
        Properties prop = new Properties();
        File pfile = null;
        for (int i = 0; i <= classPaths.length; i++) {
            if (i == classPaths.length)
                pfile = new File("./" + propfile + ".properties");
            else {
                pfile = new File(classPaths[i] + "/" + propfile + ".properties");
            }
            if (pfile.exists()) {
                break;
            }
        }
        if ((pfile != null) && (pfile.exists())) {
            FileInputStream fis = null;
            try {
                fis = new FileInputStream(pfile);
                prop.load(fis);
            } catch (FileNotFoundException e) {
            } catch (IOException e) {
                log.warn(e);
            } finally {
                JdbcUtil.closeQuietly(fis);
            }
        } else {
            ResourceBundle rb = ResourceBundle.getBundle(propfile);
            for (String property : CPConfigImpl.PROPERTIES) {
                if (rb.containsKey(property)) {
                    prop.setProperty(property, rb.getString(property));
                }
            }
        }
        return prop;
    }

    public Connection getConnection()
            throws SQLException {
        return getConnection(!this.config.isTransactionMode());
    }

    public Connection getConnection(boolean autoCommit)
            throws SQLException {
        if (this.shutdown.get()) {
            throw new SQLException("connection pool is shutdown", "08001");
        }
        long start = System.nanoTime();
        Integer connId = (Integer) this.idleConnectionsId.pop();
        if ((connId == null) && (!this.config.isLazyInit())) {
            connId = newConnection(true);
        }
        if (connId == null) {
            if (this.validConnectionNum.get() >= this.config.getMaxConnections())
                log.info(new Object[]{"connections of ", this.poolName, " to ", this.config.getConnUrl(), " exhausted, wait ", Long.valueOf(this.config.getCheckoutTimeoutMilliSec()), " ms for idle connection"});
            try {
                connId = (Integer) this.idleConnectionsId.pop(this.config.getCheckoutTimeoutMilliSec(), TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                log.info(e);
            }
        }
        if (connId == null) {
            logVerboseInfo(true);
            throw new SQLException("Timed out waiting for a free available connection of " + this.poolName + " to " + this.config.getConnUrl(), "08001");
        }
        PooledConnection pconn = (PooledConnection) this.validConnectionsPool.get(connId);
        try {
            Connection conn = pconn.checkOut(autoCommit);
            if (this.config.isVerbose()) {
                log.debug(new Object[]{pconn.getConnectionName(), ".getConnection(", Boolean.valueOf(autoCommit), "), use ", Formatter.formatNS(System.nanoTime() - start), " ns"});
            }
            return conn;
        } catch (SQLException e) {
            checkIn(pconn);
            throw e;
        }
    }

    void checkIn(PooledConnection pconn) {
        int connId = pconn.getConnectionId();
        this.idleConnectionsId.push(Integer.valueOf(connId));
    }

    public String getPoolName() {
        return this.poolName;
    }

    private Integer newConnection(boolean directReturn)
            throws SQLException {
        if (this.validConnectionNum.incrementAndGet() <= this.config.getMaxConnections()) {
            Integer connId = Integer.valueOf(this.connectionNo.getAndIncrement());
            try {
                PooledConnection pconn = null;
                if (this.config.isOracle()) {
                    pconn = new OraclePooledConnection(this, connId.intValue());
                } else {
                    if (this.config.isMySQL()) {
                        pconn = new MySqlPooledConnection(this, connId.intValue());
                    } else {
                        if (this.config.isDB2())
                            pconn = new DB2PooledConnection(this, connId.intValue());
                        else
                            pconn = new PooledConnection(this, connId.intValue());
                    }
                }
                this.validConnectionsPool.put(connId, pconn);
                if (directReturn) {
                    return connId;
                }
                this.idleConnectionsId.push(connId);

                if (this.config.isVerbose())
                    log.info(new Object[]{this.poolName, " +)", Integer.valueOf(this.validConnectionNum.get()), " connections to ", this.config.getConnUrl()});
            } catch (SQLException e) {
                this.validConnectionNum.decrementAndGet();
                throw e;
            }
        } else {
            this.validConnectionNum.decrementAndGet();
        }
        return null;
    }

    private boolean closeConnection(PooledConnection pc) {
        if (!this.idleConnectionsId.removeStackBottom(Integer.valueOf(pc.getConnectionId()))) {
            return false;
        }
        this.validConnectionNum.decrementAndGet();
        this.validConnectionsPool.remove(Integer.valueOf(pc.getConnectionId()));
        pc.close();
        if (this.config.isVerbose()) {
            log.info(new Object[]{this.poolName, " -)", Integer.valueOf(this.validConnectionNum.get()), " connections to ", this.config.getConnUrl()});
        }
        return true;
    }

    private void logVerboseInfo(boolean verbose) {
        int i = 0;
        for (Map.Entry e : this.validConnectionsPool.entrySet()) {
            PooledConnection pc = (PooledConnection) e.getValue();
            if (pc.isCheckOut()) {
                i++;
                long usedMS = pc.getCheckOutTime();
                if ((usedMS <= pc.getInfoSQLThreshold()) && (log.isDebugEnabled()))
                    log.debug(new Object[]{pc.getConnectionName(), " checkout by ", pc.getThreadCheckOut().getName(), " for " + usedMS + " ms[", pc.isBusying() ? "BUSYING" : "IDLE", "]"});
                else if ((usedMS <= pc.getWarnSQLThreshold()) && (log.isInfoEnabled()))
                    log.info(new Object[]{pc.getConnectionName(), " checkout by ", pc.getThreadCheckOut().getName(), " for " + usedMS + " ms[", pc.isBusying() ? "BUSYING" : "IDLE", "]"});
                else if (log.isWarnEnabled()) {
                    log.warn(new Object[]{pc.getConnectionName(), " checkout by ", pc.getThreadCheckOut().getName(), " for " + usedMS + " ms[", pc.isBusying() ? "BUSYING" : "IDLE", "]"});
                }
            }
        }
        if (verbose)
            log.info(new Object[]{this.poolName, ": checkout:", Integer.valueOf(i), "/connected:", Integer.valueOf(this.validConnectionNum.get()), "/max:", Integer.valueOf(this.config.getMaxConnections())});
        else
            log.debug(new Object[]{this.poolName, ": checkout:", Integer.valueOf(i), "/connected:", Integer.valueOf(this.validConnectionNum.get()), "/max:", Integer.valueOf(this.config.getMaxConnections())});
    }

    void offerUnclosedConnection(Connection connection, String connectionName) {
        this.unclosedConnections.offer(new NamedConnection(connection, connectionName));
    }

    void closeUnclosedConnection() {
        int c = this.unclosedConnections.size();
        for (int i = 0; i < c; i++) {
            NamedConnection namedConnection = (NamedConnection) this.unclosedConnections.poll();
            if (namedConnection == null)
                break;
            try {
                namedConnection.retryCloseCount += 1;
                try {
                    namedConnection.connection.close();
                } catch (SQLException e) {
                    try {
                        namedConnection.connection.rollback();
                    } catch (SQLException ignr) {
                    }
                    namedConnection.connection.close();
                }
                log.info(new Object[]{namedConnection.connectionName, " finally be closed!"});
            } catch (SQLException e) {
                log.error(new Object[]{"closeUnclosedConnection(", namedConnection.connectionName, ")[", Integer.valueOf(namedConnection.retryCloseCount), "] error: ", e.toString()});
                if (namedConnection.retryCloseCount < 10)
                    this.unclosedConnections.offer(namedConnection);
            }
        }
    }

    public int getActiveConnectionsCount() {
        return this.validConnectionNum.get();
    }

    public int getIdleConnectionsCount() {
        return this.idleConnectionsId.size();
    }

    public CPConfigImpl getConfig() {
        return this.config;
    }

    public int getPoolId() {
        return this.poolId;
    }

    public void setPoolName(String poolName) {
        if (!this.configFromProperties) {
            this.poolName = poolName;
            if (this.monitor != null)
                this.monitor.setName("CPM:" + poolName);
        }
    }

    private static class LinkedStack<E> {
        private LinkedList<E> stack;
        private final ReentrantLock operLock = new ReentrantLock();
        private final Condition notEmpty = this.operLock.newCondition();
        private final Condition requireMore = this.operLock.newCondition();

        public LinkedStack() {
            this.stack = new LinkedList();
        }

        public int size() {
            return this.stack.size();
        }

        public boolean removeStackBottom(E e) {
            this.operLock.lock();
            try {
                if (this.stack.size() == 0) {
                    return false;
                }
                Object x = this.stack.getFirst();
                boolean bool2;
                if (x.equals(e)) {
                    this.stack.removeFirst();
                    return true;
                }
                return false;
            } finally {
                this.operLock.unlock();
            }
        }

        public void push(E e) {
            int c = -1;
            this.operLock.lock();
            try {
                c = this.stack.size();
                this.stack.addLast(e);
                if (c == 0)
                    this.notEmpty.signal();
            } finally {
                this.operLock.unlock();
            }
        }

        public E pop() {
            this.operLock.lock();
            try {
                Object localObject1;
                if (0 == this.stack.size()) {
                    return null;
                }
                return this.stack.removeLast();
            } finally {
                this.operLock.unlock();
            }
        }

        public long awaitRequireMore(long timeout, TimeUnit unit)
                throws InterruptedException {
            this.operLock.lockInterruptibly();
            try {
                return this.requireMore.awaitNanos(unit.toNanos(timeout));
            } catch (InterruptedException e) {
                this.requireMore.signal();
                throw e;
            } finally {
                this.operLock.unlock();
            }
        }

        public boolean awaitNotEmpty(long timeout, TimeUnit unit) {
            try {
                this.operLock.lockInterruptibly();
            } catch (InterruptedException e) {
                return true;
            }
            try {
                return this.notEmpty.await(timeout, unit);
            } catch (InterruptedException e) {
                return true;
            } finally {
                this.operLock.unlock();
            }
        }

        public E pop(long timeout, TimeUnit unit) throws InterruptedException, SQLException {
            long nanos = unit.toNanos(timeout);
            this.operLock.lockInterruptibly();
            try {
                while (true) {
                    Object x;
                    if (this.stack.size() > 0) {
                        x = this.stack.removeLast();
                        if (this.stack.size() > 0) {
                            this.notEmpty.signal();
                        }
                        return (E) x;
                    }

                    this.requireMore.signal();

                    if (nanos <= 0L)
                        return null;
                    try {
                        nanos = this.notEmpty.awaitNanos(nanos);
                    } catch (InterruptedException ie) {
                        this.notEmpty.signal();
                        throw ie;
                    }
                }
            } finally {
                this.operLock.unlock();
            }
        }

        public Integer[] toArray() {
            this.operLock.lock();
            try {
                Integer[] ret = new Integer[this.stack.size()];
                return (Integer[]) this.stack.toArray(ret);
            } finally {
                this.operLock.unlock();
            }
        }
    }

    private class CPMonitor extends Thread {
        private ExecutorService executorService = Executors.newSingleThreadExecutor();

        private CPMonitor() {
        }

        private long idleConnectionCheckOrClose()
                throws InterruptedException {
            long timeToNextCheck = CP.this.config.getIdleTimeoutMilliSec();
            Integer[] connIds = CP.this.idleConnectionsId.toArray();
            for (Integer connId : connIds) {
                PooledConnection pc = (PooledConnection) CP.this.validConnectionsPool.get(connId);
                pc.lock();
                try {
                    if (pc.isCheckOut()) {
                        pc.unlock();
                        break;
                    }
                    timeToNextCheck = pc.getTimeCheckIn() + CP.this.config.getIdleTimeoutMilliSec() - System.currentTimeMillis();
                    if (timeToNextCheck <= 0L) {
                        timeToNextCheck = CP.this.config.getIdleTimeoutMilliSec();
                        if (CP.this.validConnectionNum.get() > CP.this.config.getMinConnections()) {
                            if (!CP.this.closeConnection(pc)) {
                                pc.unlock();
                                break;
                            }
                        } else {
                            asyncCheckConnection(pc);
                        }

                    } else {
                        pc.unlock();
                        break;
                    }
                } finally {
                    pc.unlock();
                }

            }
            return timeToNextCheck;
        }

        private void asyncCheckConnection(final PooledConnection pooledConnection) {
            Future future = this.executorService.submit(new Runnable() {
                public void run() {
                    pooledConnection.doCheck();
                }
            });
            try {
                future.get(CP.this.config.getIdleTimeoutMilliSec(), TimeUnit.MILLISECONDS);
            } catch (Exception e) {
                CP.log.warn(new Object[]{"get connection: ", pooledConnection.getConnectionName(), " check result error: ", e});
                pooledConnection.close();
            }
        }

        private void newMoreConnections(long waitTime)
                throws InterruptedException {
            try {
                while (CP.this.validConnectionNum.get() < CP.this.config.getMinConnections())
                    CP.this.newConnection(false);
            } catch (SQLException e) {
                CP.log.warn(e);
            }
            long nanos = TimeUnit.MILLISECONDS.toNanos(waitTime);
            long timeout = System.nanoTime() + nanos;
            while (timeout - System.nanoTime() > 0L) {
                nanos = CP.this.idleConnectionsId.awaitRequireMore(timeout - System.nanoTime(), TimeUnit.NANOSECONDS);
                if (nanos <= 0L) break;
                try {
                    CP.this.newConnection(false);
                } catch (SQLException e) {
                    CP.log.warn(e);
                }
            }
        }

        public void run() {
            CP.log.info(new Object[]{getName(), " start!"});
            long idleTimeout = CP.this.config.getIdleTimeoutMilliSec();
            while (!CP.this.shutdown.get()) {
                try {
                    newMoreConnections(idleTimeout);

                    CP.this.closeUnclosedConnection();

                    idleTimeout = idleConnectionCheckOrClose();

                    CP.this.logVerboseInfo(CP.this.config.isVerbose());
                } catch (InterruptedException e) {
                    if (CP.this.shutdown.get())
                        break;
                } catch (Exception e) {
                    idleTimeout = CP.this.config.getIdleTimeoutMilliSec();
                    CP.log.warn(e);
                } catch (Throwable t) {
                    idleTimeout = CP.this.config.getIdleTimeoutMilliSec();
                    CP.log.error(t);
                }
            }
            this.executorService.shutdown();
            try {
                if (!this.executorService.awaitTermination(1L, TimeUnit.SECONDS)) {
                    this.executorService.shutdownNow();
                    this.executorService.awaitTermination(1L, TimeUnit.SECONDS);
                }
            } catch (InterruptedException ignr) {
            }

            CP.log.info(new Object[]{getName(), " quit!"});
        }
    }

    private class NamedConnection {
        final Connection connection;
        final String connectionName;
        int retryCloseCount = 0;

        private NamedConnection(Connection connection, String connectionName) {
            this.connection = connection;
            this.connectionName = connectionName;
        }
    }
}