package com.bitium10.commons;

import com.bitium10.commons.utils.JdbcUtil;
import com.bitium10.commons.utils.StringUtils;
import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.io.InputStream;
import java.sql.DriverManager;
import java.util.Properties;

/**
 * <b>项目名</b>： db-pool <br>
 * <b>包名称</b>： com.bitium10.commons.impl <br>
 * <b>类名称</b>： CPConfigBeanImpl <br>
 * <b>类描述</b>： <br>
 * <b>创建人</b>： <a href="mailto:shouli1990@gmail.com">李朋明</a> <br>
 * <b>修改人</b>： <br>
 * <b>创建时间</b>：2014/10/1 18:11
 * <b>修改时间</b>： <br>
 * <b>修改备注</b>： <br>
 *
 * @version 1.0.0 <br>
 */
public class CPConfigImpl implements CPConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger(CPConfigImpl.class);
    private String connUrl;
    private String driver;
    private String username;
    private String password;
    private int minConnNum = 0;

    private int maxConnNum = 10;

    private int maxStmtNum = 100;

    private int maxPreStmtNum = 10;

    private long maxIdleMilliSec = 300000L;

    private long checkOutTimeout = 10000L;

    private boolean commitOnClose = false;

    private boolean verbose = false;

    private boolean printSQL = true;
    private String checkStatement;
    private int jmxLevel = 0;

    private boolean transactionMode = false;

    private boolean lazyInit = false;

    private long infoSQLThreshold = 10L;

    private long warnSQLThreshold = 100L;

    private boolean isOracle = false;

    private boolean isMySQL = false;

    private boolean isDB2 = false;

    private boolean useOracleImplicitPSCache = true;

    private Properties connectionProperties = new Properties();

    private int queryTimeout = 60;
    private static final String PATTERN_COMMONS_CHARS = "[ -~]+";
    public static final String[] PROPERTIES = {"jdbc.driver", "jdbc.url", "jdbc.username", "jdbc.password", "jdbc.check_statement", "jdbc.verbose", "jdbc.printSQL", "jdbc.commit_on_close", "jdbc.transaction_mode", "jdbc.lazy_init", "jdbc.min_connections", "jdbc.max_connections", "jdbc.max_statements", "jdbc.max_prestatements", "jdbc.idle_timeout", "jdbc.checkout_timeout", "jdbc.jmx_level", "jdbc.infoSQL", "jdbc.warnSQL", "jdbc.use_implicit_ps_cache", "jdbc.connection_info", "jdbc.query_timeout"};

    public int getLoginTimeout() {
        return DriverManager.getLoginTimeout();
    }

    public void setLoginTimeout(int loginTimeout) {
        DriverManager.setLoginTimeout(loginTimeout);
    }

    public int getQueryTimeout() {
        return this.queryTimeout;
    }

    public void setQueryTimeout(int queryTimeout) {
        this.queryTimeout = queryTimeout;
    }

    public Properties getConnectionProperties() {
        return this.connectionProperties;
    }

    public void setConnectionInfo(String connectionInfo) {
        if ((connectionInfo == null) || (connectionInfo.trim().length() == 0)) {
            return;
        }

        String[] entries = connectionInfo.split("&");
        for (int i = 0; i < entries.length; i++) {
            String entry = entries[i];
            if (entry.length() > 0) {
                int index = entry.indexOf(61);
                if (index > 0) {
                    String name = entry.substring(0, index);
                    String value = entry.substring(index + 1);
                    this.connectionProperties.setProperty(name, value);
                } else {
                    this.connectionProperties.setProperty(entry, "");
                }
            }
        }
    }

    public String getConnUrl() {
        return this.connUrl;
    }

    public void setConnUrl(String connUrl) {
        this.connUrl = connUrl;
        if ((this.connUrl != null) && (this.connUrl.trim().length() != 0)) {
            String[] buf = this.connUrl.split(":");
            if (buf.length < 2) {
                return;
            }
            String dbf = buf[1];
            if (dbf.compareToIgnoreCase("oracle") == 0)
                this.isOracle = true;
            else if (dbf.compareToIgnoreCase("mysql") == 0)
                this.isMySQL = true;
            else if (dbf.compareToIgnoreCase("db2") == 0) {
                this.isDB2 = true;
            }
            if ((this.checkStatement == null) || (this.checkStatement.trim().length() == 0)) {
                if (this.isDB2)
                    this.checkStatement = "values(current timestamp)";
                else if (this.isOracle)
                    this.checkStatement = "select systimestamp from dual";
                else if (this.isMySQL) {
                    this.checkStatement = "select now()";
                }
            }
            if ((this.driver == null) || (this.driver.trim().length() == 0)) {
                if (this.isOracle)
                    this.driver = "oracle.jdbc.driver.OracleDriver";
                else if (this.isMySQL)
                    this.driver = "com.mysql.jdbc.Driver";
            }
        }
    }

    public String getUrl() {
        return getConnUrl();
    }

    public void setUrl(String url) {
        setConnUrl(url);
    }

    public long getWarnSQLThreshold() {
        return this.warnSQLThreshold;
    }

    public void setWarnSQLThreshold(long warnSQLThreshold) {
        this.warnSQLThreshold = warnSQLThreshold;
    }

    public long getInfoSQLThreshold() {
        return this.infoSQLThreshold;
    }

    public void setInfoSQLThreshold(long infoSQLThreshold) {
        this.infoSQLThreshold = infoSQLThreshold;
    }

    public String getDriver() {
        return this.driver;
    }

    public void setDriver(String driver) {
        this.driver = driver;
    }

    public String getUsername() {
        return this.username;
    }

    public void setUsername(String username) {
        this.username = username;
        if ((username != null) && (username.trim().length() > 0))
            this.connectionProperties.setProperty("user", username);
    }

    String getPassword() {
        return this.password;
    }

    public void setPassword(String password) {
        if (StringUtils.isEmpty(password)) {
            return;
        }
        this.password = password;
        boolean isBase64 = Base64.isBase64(password);
        if (isBase64) {
            try {
                byte[] base64DecodedPwd = Base64.decodeBase64(password);
                boolean isBlock16 = base64DecodedPwd.length % 16 == 0;
                if ((isBlock16) && (!new String(base64DecodedPwd, "UTF-8").matches("[ -~]+"))) {
                    String plainPwd = StringUtils.trimToEmpty(JdbcUtil.decodePassword(password));
                    if (plainPwd.matches("[ -~]+"))
                        this.password = plainPwd;
                }
            } catch (Exception e) {
                LOGGER.info("使用明文密码: ", e.getMessage());
            }
        }
        this.connectionProperties.setProperty("password", this.password);
    }

    public int getMinConnections() {
        return this.minConnNum;
    }

    public void setMinConnections(int minConnections) {
        this.minConnNum = minConnections;
    }

    public int getMaxConnections() {
        return this.maxConnNum;
    }

    public void setMaxConnections(int maxConnections) {
        this.maxConnNum = maxConnections;
    }

    public boolean isVerbose() {
        return this.verbose;
    }

    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }

    public boolean isPrintSQL() {
        return this.printSQL;
    }

    public void setPrintSQL(boolean printSQL) {
        this.printSQL = printSQL;
    }

    public boolean isCommitOnClose() {
        return this.commitOnClose;
    }

    public void setCommitOnClose(boolean commitOnClose) {
        this.commitOnClose = commitOnClose;
    }

    public long getIdleTimeoutSec() {
        return this.maxIdleMilliSec / 1000L;
    }

    public long getIdleTimeoutMilliSec() {
        return this.maxIdleMilliSec;
    }

    public void setIdleTimeoutSec(long idleTimeoutSec) {
        this.maxIdleMilliSec = (idleTimeoutSec * 1000L);
    }

    public long getCheckoutTimeoutMilliSec() {
        return this.checkOutTimeout;
    }

    public void setCheckoutTimeoutMilliSec(long checkoutTimeoutMilliSec) {
        this.checkOutTimeout = checkoutTimeoutMilliSec;
    }

    public int getMaxStatements() {
        return this.maxStmtNum;
    }

    public void setMaxStatements(int maxStatements) {
        this.maxStmtNum = maxStatements;
    }

    public int getMaxPreStatements() {
        return this.maxPreStmtNum;
    }

    public void setMaxPreStatements(int maxPreStatements) {
        this.maxPreStmtNum = maxPreStatements;
    }

    public String getCheckStatement() {
        return this.checkStatement;
    }

    public void setCheckStatement(String checkStatement) {
        this.checkStatement = checkStatement;
    }

    public boolean isTransactionMode() {
        return this.transactionMode;
    }

    public void setTransactionMode(boolean transactionMode) {
        this.transactionMode = transactionMode;
    }

    public int getJmxLevel() {
        return this.jmxLevel;
    }

    public void setJmxLevel(int jmxLevel) {
        this.jmxLevel = jmxLevel;
    }

    public boolean isLazyInit() {
        return this.lazyInit;
    }

    public void setLazyInit(boolean lazyInit) {
        this.lazyInit = lazyInit;
    }

    public boolean isMySQL() {
        return this.isMySQL;
    }

    public boolean isOracle() {
        return this.isOracle;
    }

    public boolean isDB2() {
        return this.isDB2;
    }

    public boolean isUseOracleImplicitPSCache() {
        return this.useOracleImplicitPSCache;
    }

    public void setUseOracleImplicitPSCache(boolean useOracleImplicitPSCache) {
        this.useOracleImplicitPSCache = useOracleImplicitPSCache;
    }

    public void setPropertiesLocation(Resource configLocation) throws IOException {
        InputStream is = configLocation.getInputStream();
        Properties prop = new Properties();
        prop.load(is);
        setProperties(prop);
    }

    public void setProperties(Properties prop) {
        setConnUrl(prop.getProperty("jdbc.url"));
        setUsername(prop.getProperty("jdbc.username", null));
        setPassword(prop.getProperty("jdbc.password", null));
        this.driver = prop.getProperty("jdbc.driver", this.driver);
        this.verbose = Boolean.valueOf(prop.getProperty("jdbc.verbose", String.valueOf(this.verbose))).booleanValue();
        this.printSQL = Boolean.valueOf(prop.getProperty("jdbc.printSQL", String.valueOf(this.printSQL))).booleanValue();
        this.commitOnClose = Boolean.valueOf(prop.getProperty("jdbc.commit_on_close", String.valueOf(this.commitOnClose))).booleanValue();
        this.minConnNum = Integer.parseInt(prop.getProperty("jdbc.min_connections", String.valueOf(this.minConnNum)));
        this.maxConnNum = Integer.parseInt(prop.getProperty("jdbc.max_connections", String.valueOf(this.maxConnNum)));
        this.maxIdleMilliSec = (Long.parseLong(prop.getProperty("jdbc.idle_timeout", String.valueOf(this.maxIdleMilliSec / 1000L))) * 1000L);
        this.checkOutTimeout = Long.parseLong(prop.getProperty("jdbc.checkout_timeout", String.valueOf(this.checkOutTimeout)));
        this.checkStatement = prop.getProperty("jdbc.check_statement", this.checkStatement);
        this.maxStmtNum = Integer.parseInt(prop.getProperty("jdbc.max_statements", String.valueOf(this.maxStmtNum)));
        this.maxPreStmtNum = Integer.parseInt(prop.getProperty("jdbc.max_prestatements", String.valueOf(this.maxPreStmtNum)));
        this.jmxLevel = Integer.parseInt(prop.getProperty("jdbc.jmx_level", String.valueOf(this.jmxLevel)));
        this.transactionMode = Boolean.parseBoolean(prop.getProperty("jdbc.transaction_mode", String.valueOf(this.transactionMode)));
        this.lazyInit = Boolean.parseBoolean(prop.getProperty("jdbc.lazy_init", String.valueOf(this.lazyInit)));
        this.infoSQLThreshold = Long.parseLong(prop.getProperty("jdbc.infoSQL", String.valueOf(this.infoSQLThreshold)));
        this.warnSQLThreshold = Long.parseLong(prop.getProperty("jdbc.warnSQL", String.valueOf(this.warnSQLThreshold)));
        this.useOracleImplicitPSCache = Boolean.valueOf(prop.getProperty("jdbc.use_implicit_ps_cache", String.valueOf(this.useOracleImplicitPSCache))).booleanValue();

        setQueryTimeout(Integer.parseInt(prop.getProperty("jdbc.query_timeout", String.valueOf(this.queryTimeout))));
        setConnectionInfo(prop.getProperty("jdbc.connection_info"));
    }

    static {
        DriverManager.setLoginTimeout(10);
    }
}