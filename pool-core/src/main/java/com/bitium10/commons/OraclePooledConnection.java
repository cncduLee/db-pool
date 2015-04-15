package com.bitium10.commons;

import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * <b>项目名</b>： com.bitium10.commons <br>
 * <b>包名称</b>： com.bitium10.commons <br>
 * <b>类名称</b>： OralcePooledConnection <br>
 * <b>类描述</b>： <br>
 * <b>创建人</b>： <a href="mailto:shouli1990@gmail.com">李朋明</a> <br>
 * <b>修改人</b>： <br>
 * <b>创建时间</b>：2014/10/5 21:12
 * <b>修改时间</b>： <br>
 * <b>修改备注</b>： <br>
 *
 * @version 1.0.0 <br>
 */
public class OraclePooledConnection extends PooledConnection {
    public OraclePooledConnection(CP pool, int connId)
            throws SQLException {
        super(pool, connId);
    }

    public boolean isFetalException(SQLException sqle) {
        if (super.isFetalException(sqle)) {
            return true;
        }
        int error_code = Math.abs(sqle.getErrorCode());

        switch (error_code) {
            case 28:
            case 600:
            case 1012:
            case 1014:
            case 1033:
            case 1034:
            case 1035:
            case 1089:
            case 1090:
            case 1092:
            case 1094:
            case 2396:
            case 3106:
            case 3111:
            case 3113:
            case 3114:
            case 3134:
            case 3135:
            case 3136:
            case 3138:
            case 3142:
            case 3143:
            case 3144:
            case 3145:
            case 3149:
            case 6801:
            case 6802:
            case 6805:
            case 9918:
            case 9920:
            case 9921:
            case 17001:
            case 17002:
            case 17008:
            case 17024:
            case 17089:
            case 17401:
            case 17409:
            case 17410:
            case 17416:
            case 17438:
            case 17442:
            case 25407:
            case 25408:
            case 25409:
            case 25425:
            case 29276:
            case 30676:
                return true;
        }
        if ((error_code >= 12100) && (error_code <= 12299)) {
            return true;
        }

        String error_text = sqle.getMessage().toUpperCase();

        if (((error_code < 20000) || (error_code >= 21000)) && (
                (error_text.indexOf("SOCKET") > -1) || (error_text.indexOf("套接字") > -1) || (error_text.indexOf("CONNECTION HAS ALREADY BEEN CLOSED") > -1) || (error_text.indexOf("BROKEN PIPE") > -1) || (error_text.indexOf("管道已结束") > -1))) {
            return true;
        }

        return false;
    }

    protected PooledPreparedStatement getPooledPreparedStatement(PreparedStatement stmt, int stmtId, String sql) throws SQLException {
        return new OraclePooledPreparedStatement(this, stmt, stmtId, sql, super.getConnectionPool().getConfig().isUseOracleImplicitPSCache());
    }
}
