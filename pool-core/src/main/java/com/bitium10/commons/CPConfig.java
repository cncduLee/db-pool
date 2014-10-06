package commons;

/**
 * <b>项目名</b>： db-pool <br>
 * <b>包名称</b>： com.bitium10.commons <br>
 * <b>类名称</b>： CPConfigBean <br>
 * <b>类描述</b>： <br>
 * <b>创建人</b>： <a href="mailto:wylipengming@chinabank.com.cn">李朋明</a> <br>
 * <b>修改人</b>： <br>
 * <b>创建时间</b>：2014/10/1 18:09
 * <b>修改时间</b>： <br>
 * <b>修改备注</b>： <br>
 *
 * @version 1.0.0 <br>
 */
public interface CPConfig {
    public String getConnUrl();

    public void setConnUrl(String paramString);

    public String getDriver();

    public void setDriver(String paramString);

    public String getUsername();

    public void setUsername(String paramString);

    public void setPassword(String paramString);

    public int getMinConnections();

    public void setMinConnections(int paramInt);

    public int getMaxConnections();

    public void setMaxConnections(int paramInt);

    public boolean isVerbose();

    public void setVerbose(boolean paramBoolean);

    public boolean isPrintSQL();

    public void setPrintSQL(boolean paramBoolean);

    public boolean isCommitOnClose();

    public void setCommitOnClose(boolean paramBoolean);

    public long getIdleTimeoutSec();

    public void setIdleTimeoutSec(long paramLong);

    public long getCheckoutTimeoutMilliSec();

    public void setCheckoutTimeoutMilliSec(long paramLong);

    public int getMaxStatements();

    public void setMaxStatements(int paramInt);

    public int getMaxPreStatements();

    public void setMaxPreStatements(int paramInt);

    public String getCheckStatement();

    public void setCheckStatement(String paramString);

    public boolean isTransactionMode();

    public void setTransactionMode(boolean paramBoolean);

    public int getJmxLevel();

    public void setJmxLevel(int paramInt);

    public boolean isLazyInit();

    public void setLazyInit(boolean paramBoolean);
}
