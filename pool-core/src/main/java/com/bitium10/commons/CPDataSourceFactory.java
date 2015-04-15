package com.bitium10.commons;

import org.apache.ibatis.datasource.DataSourceFactory;

import javax.sql.DataSource;
import java.util.Map;
import java.util.Properties;

/**
 * <b>项目名</b>： db-pool <br>
 * <b>包名称</b>： com.bitium10.commons <br>
 * <b>类名称</b>： CPDataSourceFactory <br>
 * <b>类描述</b>： <br>
 * <b>创建人</b>： <a href="mailto:shouli1990@gmail.com">李朋明</a> <br>
 * <b>修改人</b>： <br>
 * <b>创建时间</b>：2014/10/1 20:33
 * <b>修改时间</b>： <br>
 * <b>修改备注</b>： <br>
 *
 * @version 1.0.0 <br>
 */
public class CPDataSourceFactory extends CPDataSource implements DataSourceFactory {
    @Override
    public DataSource getDataSource() {
        return this;
    }

    public void initialize(Map map) {
        Properties prop = new Properties();
        prop.putAll(map);

        setProperties(prop);
    }
}
