package com.bitium10.commons;

import com.bitium10.commons.log.Logger;
import com.bitium10.commons.log.LoggerFactory;
import com.bitium10.commons.utils.Formatter;
import com.bitium10.commons.utils.JMXUtil;
import com.bitium10.commons.utils.JdbcUtil;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.*;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

/**
 * <b>项目名</b>： db-pool <br>
 * <b>包名称</b>： com.bitium10.commons <br>
 * <b>类名称</b>： PooledConnection <br>
 * <b>类描述</b>： <br>
 * <b>创建人</b>： <a href="mailto:wylipengming@chinabank.com.cn">李朋明</a> <br>
 * <b>修改人</b>： <br>
 * <b>创建时间</b>：2014/10/1 18:00
 * <b>修改时间</b>： <br>
 * <b>修改备注</b>： <br>
 *
 * @version 1.0.0 <br>
 */
public class PooledConnection implements InvocationHandler, PooledConnectionMBean {
    private static final Logger log = LoggerFactory.getLogger(PooledConnection.class);

    private AtomicInteger statementNo = new AtomicInteger(0);
    private final CP connectionPool;
    private final int connectionId;
    private final String connectionName;
    private Connection connection;
    private Connection real_connection;
    private AtomicBoolean checkOut = new AtomicBoolean(false);

    private long timeCheckIn = System.currentTimeMillis();
    private long timeCheckOut;
    private Thread threadCheckOut;
    private AtomicInteger validStatementNum = new AtomicInteger(0);
    private LinkedBlockingQueue<PooledStatement> idleStatementsPool;
    private ConcurrentHashMap<Integer, PooledStatement> activeStatementsPool;
    private LinkedHashMap validPreStatementsPool;
    private AtomicBoolean closed = new AtomicBoolean(true);

    private final ReentrantLock operLock = new ReentrantLock();

    private boolean autoCommit = false;

    private boolean dirty = false;


    PooledConnection(CP pool, int connId) throws SQLException {
        this.connectionPool = pool;
        this.connectionId = connId;
        this.connectionName = (pool.getPoolName() + "#" + connId);
        this.idleStatementsPool = new LinkedBlockingQueue(this.connectionPool.getConfig().getMaxStatements());
        this.activeStatementsPool = new ConcurrentHashMap(this.connectionPool.getConfig().getMaxStatements());
        this.validPreStatementsPool = new LinkedHashMap(
            this.connectionPool.getConfig().getMaxPreStatements() + 1, 0.75F,
            true) {
            private static final long serialVersionUID = -5350521942562100031L;

            @Override
            protected boolean removeEldestEntry(Map.Entry eldest) {
                if (size() > PooledConnection.this.connectionPool.getConfig().getMaxPreStatements()) {
                    PooledPreparedStatement ppstmt = (PooledPreparedStatement)eldest.getValue();
                    if (ppstmt.isCheckOut()) {
                        return false;
                    }
                    ppstmt.close();
                    return true;
                }
                return false;
            }
        };

            makeRealConnection();
            this.connection = buildProxy();
            if (pool.getConfig().getJmxLevel() > 1)
                JMXUtil.register(getClass().getPackage().getName() + ":type=pool-" + pool.getPoolName() + ",name=" + getConnectionName(), this);
    }

    private void makeRealConnection() throws SQLException {
        if (!this.closed.get()) {
            return;
        }
        long start = System.nanoTime();

        this.real_connection = DriverManager.getConnection(this.connectionPool.getConfig().getConnUrl(), this.connectionPool.getConfig().getConnectionProperties());

        this.autoCommit = this.real_connection.getAutoCommit();
        log.info(new Object[]{this.connectionName, " make new connection to ", this.connectionPool.getConfig().getConnUrl(), " use ", Formatter.formatNS(System.nanoTime() - start), " ns"});
        this.closed.set(false);
    }

    public boolean recover(SQLException sqle) {
        if (isFetalException(sqle)) {
            close();
            try {
                makeRealConnection();

                log.info(new Object[]{"recover ok from exception: ", sqle});

                return true;
            } catch (Exception e) {
                log.info("recover fail");
                log.info(e);
            }
        }
        return false;
    }

    public boolean isFetalException(SQLException sqle) {
        String sqls = sqle.getSQLState();
        if ((sqls == null) || (sqls.startsWith("08"))) {
            return true;
        }

        char firstChar = sqls.charAt(0);
        if ((firstChar >= '5') && (firstChar <= '9')) {
            return true;
        }
        return false;
    }

    public Connection getConnection() {
        return this.real_connection;
    }

    public void lock() throws InterruptedException {
        this.operLock.lockInterruptibly();
    }

    public void unlock() {
        this.operLock.unlock();
    }

    public Connection checkOut(boolean autoCommit) throws SQLException {
        try {
            lock();
        } catch (InterruptedException e) {
            throw new SQLException("waiting for a free available connection be interrupted", "08001");
        }
        try {
            if (this.closed.get()) {
                makeRealConnection();
            }

            if (autoCommit != this.autoCommit) {
                this.connection.setAutoCommit(autoCommit);
            }

            if (this.checkOut.getAndSet(true)) {
                throw new SQLException("connection of " + this.connectionName + " had be checkout! ", "08001");
            }
            this.timeCheckOut = System.currentTimeMillis();
            this.threadCheckOut = Thread.currentThread();

            this.autoCommit = this.real_connection.getAutoCommit();
            return this.connection;
        } finally {
            unlock();
        }
    }

    private Connection buildProxy() {
        Class[] intfs = this.real_connection.getClass().getInterfaces();
        boolean impled = false;
        for (Class intf : intfs) {
            if (intf.getName().equals(Connection.class.getName())) {
                impled = true;
                break;
            }
        }
        if (!impled) {
            Class[] tmp = intfs;
            intfs = new Class[tmp.length + 1];
            System.arraycopy(tmp, 0, intfs, 0, tmp.length);
            intfs[tmp.length] = Connection.class;
        }
        return (Connection) Proxy.newProxyInstance(this.real_connection.getClass().getClassLoader(), intfs, this);
    }

    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        String mname = method.getName();
        if ((this.closed.get()) && (!mname.equals("close")))
            makeRealConnection();
        try {
            return _invoke(proxy, method, args);
        } catch (SQLException e) {
            if (recover(e)) {
                if (this.autoCommit) {
                    return _invoke(proxy, method, args);
                }
            }

            throw e;
        }
    }

    private Object _invoke(Object proxy, Method method, Object[] args) throws Throwable {
        long invokeStart = System.nanoTime();
        Object ret = null;
        String mname = method.getName();
        try {
            if (mname.equals("close")) {
                checkIn();
                if (isVerbose())
                    log.debug(new Object[]{this.connectionName, ".close() use ", Formatter.formatNS(System.nanoTime() - invokeStart), " ns"});
            } else if (mname.equals("createStatement")) {
                ret = createStatement(method, args);
                if (isVerbose())
                    log.all(new Object[]{this.connectionName, ".", mname, "(...) use ", Formatter.formatNS(System.nanoTime() - invokeStart), " ns"});
            } else if (mname.equals("prepareStatement")) {
                ret = prepareStatement(method, args);
                if (isVerbose())
                    log.all(new Object[]{this.connectionName, ".", mname, "(...) use ", Formatter.formatNS(System.nanoTime() - invokeStart), " ns"});
            } else if (mname.equals("prepareCall")) {
                ret = prepareCall(method, args);
                if (isVerbose())
                    log.all(new Object[]{this.connectionName, ".", mname, "(...) use ", Formatter.formatNS(System.nanoTime() - invokeStart), " ns"});
            } else if ((mname.equals("commit")) || (mname.equals("rollback"))) {
                ret = method.invoke(this.real_connection, args);
                this.dirty = false;
                if (isVerbose())
                    log.debug(new Object[]{this.connectionName, ".", mname, "() use ", Formatter.formatNS(System.nanoTime() - invokeStart), " ns"});
            } else if ((mname.equals("setAutoCommit")) && (args.length == 1)) {
                ret = method.invoke(this.real_connection, args);
                this.autoCommit = this.real_connection.getAutoCommit();
                if (isVerbose())
                    log.debug(new Object[]{this.connectionName, ".", mname, "(", args[0], ") use ", Formatter.formatNS(System.nanoTime() - invokeStart), " ns"});
            } else {
                ret = method.invoke(this.real_connection, args);
                if (isVerbose())
                    log.all(new Object[]{this.connectionName, ".", mname, "(...) use ", Formatter.formatNS(System.nanoTime() - invokeStart), " ns"});
            }
        } catch (InvocationTargetException ite) {
            throw ite.getCause();
        }
        return ret;
    }

    protected void checkIn()
            throws SQLException {
        if (!this.checkOut.getAndSet(false)) {
            return;
        }
        this.timeCheckIn = System.currentTimeMillis();

        if ((!this.autoCommit) && (this.dirty)) {
            try {
                if (this.connectionPool.getConfig().isCommitOnClose()) {
                    this.real_connection.commit();
                    if (isVerbose())
                        log.debug(new Object[]{this.connectionName, ".commit() on close"});
                } else {
                    this.real_connection.rollback();
                    if (isVerbose())
                        log.debug(new Object[]{this.connectionName, ".rollback() on close"});
                }
            } catch (SQLException e) {
                log.warn(e);
            }
            this.dirty = false;
        }

        for (Object obj : this.activeStatementsPool.entrySet().toArray()) {
            Map.Entry entry = (Map.Entry) obj;
            PooledStatement pstmt = (PooledStatement) entry.getValue();
            pstmt.getProxy().close();
            if (isVerbose()) {
                log.debug(new Object[]{pstmt.getStatementName(), " force to close."});
            }
        }
        this.connectionPool.checkIn(this);
    }

    public Statement createStatement(Method method, Object[] args) throws Throwable {
        PooledStatement pstmt = null;
        if ((args == null) || (args.length == 0)) {
            pstmt = (PooledStatement) this.idleStatementsPool.poll();
        } else return (Statement) method.invoke(this.real_connection, args);

        if (pstmt == null) {
            if (this.validStatementNum.incrementAndGet() <= this.connectionPool.getConfig().getMaxStatements()) {
                long invokeStart = System.nanoTime();
                Statement stmt = (Statement) method.invoke(this.real_connection, args);
                if (this.connectionPool.getConfig().getQueryTimeout() > 0) {
                    stmt.setQueryTimeout(this.connectionPool.getConfig().getQueryTimeout());
                }
                pstmt = new PooledStatement(this, stmt, this.statementNo.getAndIncrement());
                if (isVerbose())
                    log.info(new Object[]{this.connectionName, " * createStatement(...)[", Integer.valueOf(this.validStatementNum.get()), "], use ", Formatter.formatNS(System.nanoTime() - invokeStart), " ns"});
            } else {
                this.validStatementNum.decrementAndGet();
            }
        }
        if (pstmt == null) {
            throw new SQLException("statements of " + this.connectionName + " exceed max value[" + this.connectionPool.getConfig().getMaxStatements() + "]", "60000");
        }
        Statement stmt = pstmt.checkOut();
        this.activeStatementsPool.put(Integer.valueOf(pstmt.getStatementId()), pstmt);
        return stmt;
    }

    public PreparedStatement prepareStatement(Method method, Object[] args) throws Throwable {
        PooledPreparedStatement ppstmt = null;
        synchronized (this.validPreStatementsPool) {
            if (args.length == 1) {
                ppstmt = (PooledPreparedStatement) this.validPreStatementsPool.get(args[0]);
            } else return (PreparedStatement) method.invoke(this.real_connection, args);

            if (ppstmt == null) {
                long invokeStart = System.nanoTime();
                PreparedStatement pstmt = (PreparedStatement) method.invoke(this.real_connection, args);
                if (this.connectionPool.getConfig().getQueryTimeout() > 0) {
                    pstmt.setQueryTimeout(this.connectionPool.getConfig().getQueryTimeout());
                }
                ppstmt = getPooledPreparedStatement(pstmt, this.statementNo.getAndIncrement(), (String) args[0]);
                if (ppstmt.isDefaultResultSetType()) {
                    this.validPreStatementsPool.put((String) args[0], ppstmt);
                    if (isVerbose()) {
                        log.info(new Object[]{this.connectionName, " * prepareStatement(", args[0], ")[", Integer.valueOf(this.validPreStatementsPool.size()), "], use ", Formatter.formatNS(System.nanoTime() - invokeStart), " ns"});
                    }
                }
            }
        }
        PreparedStatement pstmt = (PreparedStatement) ppstmt.checkOut();
        this.activeStatementsPool.put(Integer.valueOf(ppstmt.getStatementId()), ppstmt);
        return pstmt;
    }

    protected PooledPreparedStatement getPooledPreparedStatement(PreparedStatement stmt, int stmtId, String sql)
            throws SQLException {
        return new PooledPreparedStatement(this, stmt, stmtId, sql);
    }

    public CallableStatement prepareCall(Method method, Object[] args) throws Throwable {
        PooledCallableStatement pcstmt = null;
        synchronized (this.validPreStatementsPool) {
            if (args.length == 1)
                pcstmt = (PooledCallableStatement) this.validPreStatementsPool.get(args[0]);
            else {
                return (CallableStatement) method.invoke(this.real_connection, args);
            }
            if (pcstmt == null) {
                long invokeStart = System.nanoTime();
                CallableStatement cstmt = (CallableStatement) method.invoke(this.real_connection, args);
                if (this.connectionPool.getConfig().getQueryTimeout() > 0) {
                    cstmt.setQueryTimeout(this.connectionPool.getConfig().getQueryTimeout());
                }
                pcstmt = new PooledCallableStatement(this, cstmt, this.statementNo.getAndIncrement(), (String) args[0]);
                if (pcstmt.isDefaultResultSetType()) {
                    this.validPreStatementsPool.put((String) args[0], pcstmt);
                    if (isVerbose()) {
                        log.info(new Object[]{this.connectionName, " * prepareCall(", args[0], ")[", Integer.valueOf(this.validPreStatementsPool.size()), "], use ", Formatter.formatNS(System.nanoTime() - invokeStart), " ns"});
                    }
                }
            }
        }
        CallableStatement pstmt = (CallableStatement) pcstmt.checkOut();
        this.activeStatementsPool.put(Integer.valueOf(pcstmt.getStatementId()), pcstmt);
        return pstmt;
    }

    public void checkIn(PooledStatement pstmt) {
        this.activeStatementsPool.remove(Integer.valueOf(pstmt.getStatementId()));
        if (pstmt.isDefaultResultSetType()) {
            if (!pstmt.isClosed())
                this.idleStatementsPool.offer(pstmt);
        } else {
            pstmt.close();
            this.validStatementNum.decrementAndGet();
        }
    }

    public void checkIn(PooledPreparedStatement ppstmt) {
        this.activeStatementsPool.remove(Integer.valueOf(ppstmt.getStatementId()));
        if (ppstmt.isDefaultResultSetType())
            ppstmt.cleanCache();
        else
            ppstmt.close();
    }

    public long getCheckOutTime() {
        if (this.checkOut.get()) {
            return System.currentTimeMillis() - this.timeCheckOut;
        }
        return 0L;
    }

    public boolean isBusying() {
        boolean b = false;
        for (Map.Entry e : this.activeStatementsPool.entrySet()) {
            if (((PooledStatement) e.getValue()).isBusying()) {
                b = true;
            }
        }
        return b;
    }

    public void doCheck() {
        Statement stmt = null;
        ResultSet rs = null;
        String checkStmt = this.connectionPool.getConfig().getCheckStatement();
        if (checkStmt == null) {
            log.info("check statement is NULL, skip connection check...");
            return;
        }
        try {
            if (this.closed.get()) {
                makeRealConnection();
            }
            stmt = this.connection.createStatement();

            rs = stmt.executeQuery(checkStmt);
            rs.next();
        } catch (Exception e) {
            log.warn(e);
        } finally {
            JdbcUtil.closeQuietly(rs);
            JdbcUtil.closeQuietly(stmt);
        }
    }

    private void commitOnClose() throws SQLException {
        if (this.connectionPool.getConfig().isCommitOnClose()) {
            this.real_connection.commit();
            log.warn(new Object[]{this.connectionName, ".commit() on close"});
        } else {
            this.real_connection.rollback();
            log.warn(new Object[]{this.connectionName, ".rollback() on close"});
        }
    }

    public void close() {
        if (this.closed.getAndSet(true)) {
            return;
        }
        this.validStatementNum.set(0);
        this.idleStatementsPool.clear();
        this.activeStatementsPool.clear();
        this.validPreStatementsPool.clear();
        if ((!this.autoCommit) && (this.dirty)) {
            try {
                commitOnClose();
            } catch (SQLException e) {
                log.error(new Object[]{this.connectionName, ".commitOnClose() error: ", e});
            }
            this.dirty = false;
        }
        try {
            try {
                this.real_connection.close();
            } catch (SQLException e) {
                log.error(new Object[]{"close real_connection[", this.connectionName, "] error: ", e});
                try {
                    this.real_connection.rollback();
                } catch (SQLException ignr) {
                }
                this.real_connection.close();
            }
            log.info(new Object[]{this.connectionName, " real closed."});
        } catch (SQLException e) {
            log.error(new Object[]{this.connectionName, " real_connection close error: ", e});
            this.connectionPool.offerUnclosedConnection(this.real_connection, this.connectionName);
        }
        if (this.connectionPool.getConfig().getJmxLevel() > 1)
            JMXUtil.unregister(getClass().getPackage().getName() + ":type=pool-" + this.connectionPool.getPoolName() + ",name=" + getConnectionName());
    }

    public int getConnectionId() {
        return this.connectionId;
    }

    public CP getConnectionPool() {
        return this.connectionPool;
    }

    public String getConnectionName() {
        return this.connectionName;
    }

    public boolean isCheckOut() {
        return this.checkOut.get();
    }

    public boolean isVerbose() {
        return this.connectionPool.getConfig().isVerbose();
    }

    public boolean isPrintSQL() {
        return this.connectionPool.getConfig().isPrintSQL();
    }

    public long getTimeCheckIn() {
        return this.timeCheckIn;
    }

    public Thread getThreadCheckOut() {
        return this.threadCheckOut;
    }

    public void setDirty() {
        this.dirty = true;
    }

    public boolean isClosed() {
        return this.closed.get();
    }

    public int getCachedStatementsCount() {
        return this.validStatementNum.get();
    }

    public int getCachedPreStatementsCount() {
        return this.validPreStatementsPool.size();
    }

    public String[] getCachedPreStatementsSQLs() {
        return (String[]) this.validPreStatementsPool.keySet().toArray(new String[this.validPreStatementsPool.size()]);
    }

    public String getCheckOutThreadName() {
        if (isCheckOut()) {
            return getThreadCheckOut().getName();
        }
        return "";
    }

    public long getInfoSQLThreshold() {
        return this.connectionPool.getConfig().getInfoSQLThreshold();
    }

    public long getWarnSQLThreshold() {
        return this.connectionPool.getConfig().getWarnSQLThreshold();
    }
}