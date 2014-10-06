package commons;

/**
 * <b>项目名</b>： db-pool <br>
 * <b>包名称</b>： com.bitium10.commons <br>
 * <b>类名称</b>： PooledConnectionMBean <br>
 * <b>类描述</b>： <br>
 * <b>创建人</b>： <a href="mailto:wylipengming@chinabank.com.cn">李朋明</a> <br>
 * <b>修改人</b>： <br>
 * <b>创建时间</b>：2014/10/1 18:00
 * <b>修改时间</b>： <br>
 * <b>修改备注</b>： <br>
 *
 * @version 1.0.0 <br>
 */
public  interface PooledConnectionMBean {
    public String getConnectionName();

    public boolean isCheckOut();

    public boolean isBusying();

    public boolean isClosed();

    public String getCheckOutThreadName();

    public int getCachedStatementsCount();

    public int getCachedPreStatementsCount();

    public String[] getCachedPreStatementsSQLs();

    public void doCheck();

    public void close();
}
