package com.bitium10.commons;

import java.lang.reflect.Proxy;
import java.sql.CallableStatement;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * <b>项目名</b>： com.bitium10.commons <br>
 * <b>包名称</b>： com.bitium10.commons <br>
 * <b>类名称</b>： PooledCallableStatement <br>
 * <b>类描述</b>： <br>
 * <b>创建人</b>： <a href="mailto:shouli1990@gmail.com">李朋明</a> <br>
 * <b>修改人</b>： <br>
 * <b>创建时间</b>：2014/10/5 21:14
 * <b>修改时间</b>： <br>
 * <b>修改备注</b>： <br>
 *
 * @version 1.0.0 <br>
 */
public class PooledCallableStatement extends PooledPreparedStatement {
    public PooledCallableStatement(PooledConnection conn, CallableStatement stmt, int stmtId, String sql)
            throws SQLException {
        super(conn, stmt, stmtId, sql);
    }

    protected CallableStatement buildProxy() {
        Statement stmt = getStatement();
        Class[] intfs = stmt.getClass().getInterfaces();
        boolean impled = false;
        for (Class intf : intfs) {
            if (intf.getName().equals(CallableStatement.class.getName())) {
                impled = true;
                break;
            }
        }
        if (!impled) {
            Class[] tmp = intfs;
            intfs = new Class[tmp.length + 1];
            System.arraycopy(tmp, 0, intfs, 0, tmp.length);
            intfs[tmp.length] = CallableStatement.class;
        }
        return (CallableStatement) Proxy.newProxyInstance(stmt.getClass().getClassLoader(), intfs, this);
    }
}
