package com.bitium10.commons.utils;

import oracle.jdbc.internal.OraclePreparedStatement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.sql.Statement;

/**
 * <b>项目名</b>： db-pool <br>
 * <b>包名称</b>： com.bitium10.commons.utils <br>
 * <b>类名称</b>： OracleUtils <br>
 * <b>类描述</b>： <br>
 * <b>创建人</b>： <a href="mailto:wylipengming@chinabank.com.cn">李朋明</a> <br>
 * <b>修改人</b>： <br>
 * <b>创建时间</b>：2014/10/5 20:31
 * <b>修改时间</b>： <br>
 * <b>修改备注</b>： <br>
 *
 * @version 1.0.0 <br>
 */
public class OracleUtils {
    private static final Logger log = LoggerFactory.getLogger(OracleUtils.class);
    public static final String ORACLE_FREECACHE_PROPERTY_NAME = "oracle.jdbc.FreeMemoryOnEnterImplicitCache";
    public static final String ORACLE_FREECACHE_PROPERTY_VALUE_TRUE = "true";
    public static final String SOCKET_TIMEOUT = "oracle.jdbc.ReadTimeout";
    public static final String SOCKET_TIMEOUT_LOW_VER = "oracle.net.READ_TIMEOUT";
    public static final String CONNECT_TIMEOUT = "oracle.net.CONNECT_TIMEOUT";

    public static void enterImplicitCache(Statement statement) {
        try {
            OraclePreparedStatement oraclePreparedStatement = unwrapInternal(statement);

            if (oraclePreparedStatement != null)
                oraclePreparedStatement.enterImplicitCache();
        } catch (SQLException e) {
            log.warn("database error", e);
        }
    }

    public static void exitImplicitCacheToActive(Statement statement) {
        try {
            OraclePreparedStatement oraclePreparedStatement = unwrapInternal(statement);

            if (oraclePreparedStatement != null)
                oraclePreparedStatement.exitImplicitCacheToActive();
        } catch (SQLException e) {
            log.warn("database error",e);
        }
    }

    public static void exitImplicitCacheToClose(Statement statement) {
        try {
            OraclePreparedStatement oraclePreparedStatement = unwrapInternal(statement);

            if (oraclePreparedStatement != null)
                oraclePreparedStatement.exitImplicitCacheToClose();
        } catch (SQLException e) {
            log.warn("database error",e);
        }
    }

    private static OraclePreparedStatement unwrapInternal(Statement stmt)  throws SQLException {
        if ((stmt instanceof OraclePreparedStatement)) {
            return (OraclePreparedStatement) stmt;
        }
        OraclePreparedStatement unwrapped = (OraclePreparedStatement) stmt.unwrap(OraclePreparedStatement.class);

        if (unwrapped == null) {
            log.error("can not unwrap statement : " + stmt.getClass());
        }

        return unwrapped;
    }
}
