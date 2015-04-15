package com.bitium10.commons;

import com.bitium10.commons.log.Logger;
import com.bitium10.commons.utils.OracleUtils;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * <b>项目名</b>： com.bitium10.commons <br>
 * <b>包名称</b>： com.bitium10.commons <br>
 * <b>类名称</b>： OraclePooledPreparedStatement <br>
 * <b>类描述</b>： <br>
 * <b>创建人</b>： <a href="mailto:shouli1990@gmail.com">李朋明</a> <br>
 * <b>修改人</b>： <br>
 * <b>创建时间</b>：2014/10/5 21:13
 * <b>修改时间</b>： <br>
 * <b>修改备注</b>： <br>
 *
 * @version 1.0.0 <br>
 */
public class OraclePooledPreparedStatement extends PooledPreparedStatement {
    private static final Logger log = new Logger();
    private boolean useOracleImplicitCache;

    public OraclePooledPreparedStatement(PooledConnection conn, PreparedStatement stmt, int stmtId, String sql, boolean useOracleImplicitCache)
            throws SQLException {
        super(conn, stmt, stmtId, sql);
        this.useOracleImplicitCache = useOracleImplicitCache;
    }

    public void cleanCache() {
        if (this.useOracleImplicitCache)
            OracleUtils.enterImplicitCache(getStatement());
    }

    public Statement checkOut()
            throws SQLException {
        if (this.useOracleImplicitCache) {
            OracleUtils.exitImplicitCacheToActive(getStatement());
        }
        return super.checkOut();
    }

    public void close() {
        if (this.useOracleImplicitCache) {
            OracleUtils.exitImplicitCacheToClose(getStatement());
        }
        super.close();
    }
}
