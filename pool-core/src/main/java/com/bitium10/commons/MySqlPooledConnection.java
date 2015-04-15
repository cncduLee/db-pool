package com.bitium10.commons;

import java.sql.SQLException;

/**
 * <b>项目名</b>： com.bitium10.commons <br>
 * <b>包名称</b>： com.bitium10.commons <br>
 * <b>类名称</b>： MySqlPooledConnection <br>
 * <b>类描述</b>： <br>
 * <b>创建人</b>： <a href="mailto:shouli1990@gmail.com">李朋明</a> <br>
 * <b>修改人</b>： <br>
 * <b>创建时间</b>：2014/10/5 21:11
 * <b>修改时间</b>： <br>
 * <b>修改备注</b>： <br>
 *
 * @version 1.0.0 <br>
 */
public class MySqlPooledConnection  extends PooledConnection
{
    public static final String CONNECT_TIMEOUT = "connectTimeout";
    public static final String SOCKET_TIMEOUT = "socketTimeout";

    MySqlPooledConnection(CP pool, int connId)
            throws SQLException
    {
        super(pool, connId);
    }

    public boolean isFetalException(SQLException sqle)
    {
        if (super.isFetalException(sqle)) {
            return true;
        }
        String sqlState = sqle.getSQLState();
        if ((sqlState == null) || (sqlState.equals("40001")))
        {
            return true;
        }

        int errorCode = sqle.getErrorCode();
        switch (errorCode)
        {
            case 1004:
            case 1005:
            case 1015:
            case 1021:
            case 1037:
            case 1038:
            case 1040:
            case 1041:
            case 1042:
            case 1043:
            case 1045:
            case 1047:
            case 1081:
            case 1129:
            case 1130:
                return true;
        }

        if ((errorCode >= -10000) && (errorCode <= -9000)) {
            return true;
        }

        String message = sqle.getMessage();
        if ((message != null) && (message.length() > 0)) {
            String errorText = message.toUpperCase();

            if (((errorCode == 0) && (errorText.indexOf("COMMUNICATIONS LINK FAILURE") > -1)) || (errorText.indexOf("COULD NOT CREATE CONNECTION") > -1) || (errorText.indexOf("NO DATASOURCE") > -1) || (errorText.indexOf("NO ALIVE DATASOURCE") > -1))
            {
                return true;
            }
        }
        return false;
    }
}
