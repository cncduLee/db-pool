package com.bitium10.commons;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

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
    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        return null;
    }

    @Override
    public String getConnectionName() {
        return null;
    }

    @Override
    public boolean isCheckOut() {
        return false;
    }

    @Override
    public boolean isBusying() {
        return false;
    }

    @Override
    public boolean isClosed() {
        return false;
    }

    @Override
    public String getCheckOutThreadName() {
        return null;
    }

    @Override
    public int getCachedStatementsCount() {
        return 0;
    }

    @Override
    public int getCachedPreStatementsCount() {
        return 0;
    }

    @Override
    public String[] getCachedPreStatementsSQLs() {
        return new String[0];
    }

    @Override
    public void doCheck() {

    }

    @Override
    public void close() {

    }
}
