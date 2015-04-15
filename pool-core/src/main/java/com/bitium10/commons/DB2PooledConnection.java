package com.bitium10.commons;

import java.sql.SQLException;

/**
 * <b>项目名</b>： com.bitium10.commons <br>
 * <b>包名称</b>： com.bitium10.commons <br>
 * <b>类名称</b>： DB2PooledConnection <br>
 * <b>类描述</b>： <br>
 * <b>创建人</b>： <a href="mailto:shouli1990@gmail.com">李朋明</a> <br>
 * <b>修改人</b>： <br>
 * <b>创建时间</b>：2014/10/6 8:31
 * <b>修改时间</b>： <br>
 * <b>修改备注</b>： <br>
 *
 * @version 1.0.0 <br>
 */
public class DB2PooledConnection extends PooledConnection {
    DB2PooledConnection(CP pool, int connId)
            throws SQLException {
        super(pool, connId);
    }

    public boolean isFetalException(SQLException sqle) {
        if (super.isFetalException(sqle)) {
            return true;
        }

        int errorCode = sqle.getErrorCode();
        switch (errorCode) {
            case -924:
            case -918:
            case -909:
            case -525:
            case -518:
            case -516:
            case -514:
            case -512:
                return true;
        }

        return false;
    }
}
