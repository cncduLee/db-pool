package com.bitium10.commons;

import com.bitium10.commons.utils.JdbcUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.*;

/**
 * <b>项目名</b>： com.bitium10.commons <br>
 * <b>包名称</b>： com.bitium10.commons <br>
 * <b>类名称</b>： PooledPreparedStatement <br>
 * <b>类描述</b>： <br>
 * <b>创建人</b>： <a href="mailto:wylipengming@chinabank.com.cn">李朋明</a> <br>
 * <b>修改人</b>： <br>
 * <b>创建时间</b>：2014/10/5 21:14
 * <b>修改时间</b>： <br>
 * <b>修改备注</b>： <br>
 *
 * @version 1.0.0 <br>
 */
public class PooledPreparedStatement extends PooledStatement {

    private static final Logger log = LoggerFactory.getLogger(PooledPreparedStatement.class);
    private PreparedStatement pstmt;
    private PreparedStatement real_pstmt;
    private final String sql;
    private final Object[] paras;
    private String toString;

    PooledPreparedStatement(PooledConnection conn, PreparedStatement stmt, int stmtId, String sql)
            throws SQLException {
        super(conn, stmt, stmtId);
        this.real_pstmt = ((PreparedStatement) getStatement());
        this.sql = (sql == null ? "" : JdbcUtil.multiLinesToOneLine(sql, " "));
        this.paras = new Object[getQMCount()];
    }

    public void cleanCache() {
    }

    protected PreparedStatement buildProxy() {
        Statement stmt = getStatement();
        Class[] intfs = stmt.getClass().getInterfaces();
        boolean impled = false;
        for (Class intf : intfs) {
            if (intf.getName().equals(PreparedStatement.class.getName())) {
                impled = true;
                break;
            }
        }
        if (!impled) {
            Class[] tmp = intfs;
            intfs = new Class[tmp.length + 1];
            System.arraycopy(tmp, 0, intfs, 0, tmp.length);
            intfs[tmp.length] = PreparedStatement.class;
        }
        this.pstmt = ((PreparedStatement) Proxy.newProxyInstance(stmt.getClass().getClassLoader(), intfs, this));
        return this.pstmt;
    }

    protected String getSqlDoing() {
        return toString();
    }

    protected Object _invoke(Object proxy, Method method, Object[] args) throws Throwable {
        long start = System.nanoTime();
        Object ret = null;
        this.toString = null;
        try {
            if ((this.methodDoing.equals("addBatch")) && ((args == null) || (args.length == 0))) {
                this.real_pstmt.addBatch();
                if (isPrintSQL())
                    printSQL(log, System.nanoTime() - start, new Object[0]);
            } else if ((this.methodDoing.equals("execute")) && ((args == null) || (args.length == 0))) {
                ret = Boolean.valueOf(this.real_pstmt.execute());
                if (isPrintSQL())
                    printSQL(log, System.nanoTime() - start, new Object[]{"[", ret, "]"});
            } else if ((this.methodDoing.equals("executeQuery")) && ((args == null) || (args.length == 0))) {
                this.resultSet = this.real_pstmt.executeQuery();
                ret = this.resultSet;
                if (isPrintSQL())
                    printSQL(log, System.nanoTime() - start, new Object[0]);
            } else if ((this.methodDoing.equals("executeUpdate")) && ((args == null) || (args.length == 0))) {
                ret = Integer.valueOf(this.real_pstmt.executeUpdate());
                if (isPrintSQL())
                    printSQL(log, System.nanoTime() - start, new Object[]{"[", ret, "]"});
            } else if ((this.methodDoing.startsWith("set")) && (args.length == 2) && ((args[0] instanceof Integer))) {
                int idx = ((Integer) args[0]).intValue();
                if (args[1] == null) {
                    args[1] = "";
                }
                if (this.paras.length >= idx) {
                    this.paras[(idx - 1)] = args[1];
                }
                ret = method.invoke(this.real_pstmt, args);
            } else if ((this.methodDoing.equals("toString")) && ((args == null) || (args.length == 0))) {
                ret = toString();
            } else {
                ret = super._invoke(proxy, method, args);
            }
        } catch (InvocationTargetException e) {
            throw e.getCause();
        }
        return ret;
    }

    private int getQMCount() {
        int c = 0;
        int idx = 0;
        while (true) {
            idx = this.sql.indexOf("?", idx + 1);
            if (idx == -1) {
                break;
            }
            c++;
        }
        return c;
    }

    public String toString() {
        if (this.toString != null) {
            return this.toString;
        }
        if (this.paras == null) {
            return this.sql;
        }
        StringBuilder sb = new StringBuilder(this.sql.length() + this.paras.length * 16);
        int idx = 0;
        for (int i = 0; i < this.paras.length; i++) {
            Object p = this.paras[i];
            int idxNext = this.sql.indexOf("?", idx);
            if (p == null) {
                idx = idxNext + 1;
            } else {
                if (idxNext < 0) {
                    break;
                }
                sb.append(this.sql.substring(idx, idxNext));
                if (((p instanceof String)) || ((p instanceof Time)) || ((p instanceof Timestamp)) || ((p instanceof Date)))
                    sb.append('\'').append(p).append('\'');
                else {
                    sb.append(p);
                }
                idx = idxNext + 1;
            }
        }
        sb.append(this.sql.substring(idx));
        this.toString = sb.toString();
        return this.toString;
    }
}
