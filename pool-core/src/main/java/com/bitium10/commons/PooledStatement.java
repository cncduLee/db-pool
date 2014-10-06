package commons;

import com.bitium10.commons.utils.JdbcUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.*;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * <b>项目名</b>： db-pool <br>
 * <b>包名称</b>： com.bitium10.commons <br>
 * <b>类名称</b>： PooledStatement <br>
 * <b>类描述</b>： <br>
 * <b>创建人</b>： <a href="mailto:wylipengming@chinabank.com.cn">李朋明</a> <br>
 * <b>修改人</b>： <br>
 * <b>创建时间</b>：2014/10/1 18:02
 * <b>修改时间</b>： <br>
 * <b>修改备注</b>： <br>
 *
 * @version 1.0.0 <br>
 */
public class PooledStatement implements InvocationHandler {
    private static final Logger log = LoggerFactory.getLogger(PooledStatement.class);
    private final PooledConnection connection;
    private final int statementId;
    private final String statementName;
    private Statement statement;
    private Statement real_statement;
    protected AtomicBoolean checkOut = new AtomicBoolean(false);

    private long timeCheckIn = System.currentTimeMillis();
    private long timeCheckOut;
    private Thread threadCheckOut;
    private boolean isDefaultResultSetType = true;
    protected ResultSet resultSet;
    private AtomicBoolean closed = new AtomicBoolean(false);

    private long busying = 0L;
    protected String methodDoing;
    private String sqlDoing;

    PooledStatement(PooledConnection conn, Statement stmt, int stmtId) throws SQLException {
        this.connection = conn;
        this.statementId = stmtId;
        this.statementName = (conn.getConnectionName() + ".STMT#" + stmtId);
        this.real_statement = stmt;
        if ((stmt.getResultSetType() == 1003) && (stmt.getResultSetConcurrency() == 1007)) {
            this.isDefaultResultSetType = true;
        } else this.isDefaultResultSetType = false;

        this.statement = buildProxy();
    }

    public Statement checkOut() throws SQLException {
        if (this.checkOut.getAndSet(true)) {
            if (this.threadCheckOut.equals(Thread.currentThread())) {
                return this.statement;
            }
            throw new SQLException(this.statementName + "已经被" + this.threadCheckOut.getName() + "检出", "60003");
        }

        this.timeCheckOut = System.currentTimeMillis();
        this.threadCheckOut = Thread.currentThread();
        return this.statement;
    }

    protected Statement buildProxy() {
        Class[] intfs = this.real_statement.getClass().getInterfaces();
        boolean impled = false;
        for (Class intf : intfs) {
            if (intf.getName().equals(Statement.class.getName())) {
                impled = true;
                break;
            }
        }
        if (!impled) {
            Class[] tmp = intfs;
            intfs = new Class[tmp.length + 1];
            System.arraycopy(tmp, 0, intfs, 0, tmp.length);
            intfs[tmp.length] = Statement.class;
        }
        return (Statement) Proxy.newProxyInstance(this.real_statement.getClass().getClassLoader(), intfs, this);
    }

    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        this.busying = System.nanoTime();
        this.sqlDoing = null;
        this.methodDoing = method.getName();
        try {
            Object obj = _invoke(proxy, method, args);
            if ((this.methodDoing.startsWith("execute")) && (!this.methodDoing.startsWith("executeQuery"))) {
                this.connection.setDirty();
            }
            return obj;
        } catch (SQLException e) {
            if (this.methodDoing.startsWith("execute")) {
                log.error(")");
            }
            if (this.connection.isFetalException(e)) {
                close();
                this.connection.recover(e);
            }
            throw e;
        } finally {
            this.busying = 0L;
        }
    }

    protected Object _invoke(Object proxy, Method method, Object[] args) throws Throwable {
        long start = System.nanoTime();
        Object ret = null;
        try {
            if (this.methodDoing.equals("close")) {
                if (!this.checkOut.getAndSet(false)) {
                    return null;
                }
                this.timeCheckIn = System.currentTimeMillis();
                if (this.resultSet != null) {
                    try {
                        this.resultSet.close();
                    } catch (SQLException e) {
                    }
                    this.resultSet = null;
                }
                if ((this instanceof PooledPreparedStatement))
                    this.connection.checkIn((PooledPreparedStatement) this);
                else {
                    this.connection.checkIn(this);
                }
                if (isVerbose())
                    log.debug(" ns");
            } else if ((this.methodDoing.equals("addBatch")) && (args != null) && (args.length == 1)) {
                this.sqlDoing = ((String) args[0]);
                this.real_statement.addBatch((String) args[0]);
                if (isPrintSQL())
                    printSQL(log, System.nanoTime() - start, new Object[0]);
            } else if (this.methodDoing.equals("executeBatch")) {
                ret = this.real_statement.executeBatch();
                if (isPrintSQL())
                    printSQL(log, System.nanoTime() - start, new Object[]{"[", Integer.valueOf(Array.getLength(ret)), "]"});
            } else if ((this.methodDoing.equals("executeQuery")) && (args != null) && (args.length == 1)) {
                this.sqlDoing = ((String) args[0]);
                this.resultSet = this.real_statement.executeQuery((String) args[0]);
                ret = this.resultSet;
                if (isPrintSQL())
                    printSQL(log, System.nanoTime() - start, new Object[0]);
            } else if ((this.methodDoing.startsWith("execute")) && (args != null) && (args.length > 0)) {
                this.sqlDoing = ((String) args[0]);
                ret = method.invoke(this.real_statement, args);
                if (isPrintSQL())
                    printSQL(log, System.nanoTime() - start, new Object[]{"[", ret, "]"});
            } else {
                ret = method.invoke(this.real_statement, args);
                if (isVerbose())
                    log.debug(" ns");
            }
        } catch (InvocationTargetException e) {
            throw e.getCause();
        }
        return ret;
    }

    protected void printSQL(Logger logger, long usedNS, Object[] infos) {
        if (!isPrintSQL()) {
            return;
        }
        if (usedNS / 1000L <= this.connection.getInfoSQLThreshold() * 1000L) {
            if (logger.isDebugEnabled())
                logger.debug(" ns");
        } else if (usedNS / 1000L <= this.connection.getWarnSQLThreshold() * 1000L) {
            if (logger.isInfoEnabled())
                logger.debug(" ns");
        } else if (logger.isWarnEnabled())
            logger.debug(" ns");
    }

    protected String getSqlDoing() {
        this.sqlDoing = (this.sqlDoing == null ? "" : JdbcUtil.multiLinesToOneLine(this.sqlDoing, " "));
        return this.sqlDoing;
    }

    public Statement getProxy() {
        return this.statement;
    }

    public Statement getStatement() {
        return this.real_statement;
    }

    public int getStatementId() {
        return this.statementId;
    }

    public boolean isCheckOut() {
        return this.checkOut.get();
    }

    public boolean isDefaultResultSetType() {
        return this.isDefaultResultSetType;
    }

    public void close() {
        if (this.closed.getAndSet(true))
            return;
        try {
            this.real_statement.close();
        } catch (SQLException e) {
        }
        log.debug(" real closed.");
    }

    public String getStatementName() {
        return this.statementName;
    }

    public boolean isVerbose() {
        return this.connection.isVerbose();
    }

    public boolean isPrintSQL() {
        return this.connection.isPrintSQL();
    }

    public long getCheckOutTime() {
        if (this.checkOut.get()) {
            return System.currentTimeMillis() - this.timeCheckOut;
        }
        return 0L;
    }

    public boolean isBusying() {
        if ((this.checkOut.get()) && (this.busying > 0L)) {
            long usedNS = System.nanoTime() - this.busying;
            if ((usedNS / 1000L <= this.connection.getInfoSQLThreshold() * 1000L) && (log.isDebugEnabled()))
                log.debug(" ns");
            else if ((usedNS / 1000L <= this.connection.getWarnSQLThreshold() * 1000L) && (log.isInfoEnabled()))
                log.debug(" ns");
            else if (log.isWarnEnabled()) {
                log.debug(" ns");
            }
            return true;
        }
        return false;
    }

    public boolean isClosed() {
        return this.closed.get();
    }

    public long getTimeCheckIn() {
        return this.timeCheckIn;
    }
}
