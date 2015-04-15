package com.bitium10.commons;

import java.sql.SQLException;

/**
 * <b>项目名</b>： db-pool <br>
 * <b>包名称</b>： com.bitium10.commons <br>
 * <b>类名称</b>： ConnectPool <br>
 * <b>类描述</b>： <br>
 * <b>创建人</b>： <a href="mailto:shouli1990@gmail.com">李朋明</a> <br>
 * <b>修改人</b>： <br>
 * <b>创建时间</b>：2014/10/1 20:31
 * <b>修改时间</b>： <br>
 * <b>修改备注</b>： <br>
 *
 * @version 1.0.0 <br>
 */
public interface ConnectPool {
    public String getPoolName();

    public void setPoolName(String paramString);

    public int getActiveConnectionsCount();

    public int getIdleConnectionsCount();

    public void reloadProperties() throws SQLException;

    public void shutdown();
}
