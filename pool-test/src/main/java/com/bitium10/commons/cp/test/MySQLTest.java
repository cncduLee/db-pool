package com.bitium10.commons.cp.test;

import com.bitium10.commons.log.Logger;
import org.mybatis.spring.support.SqlSessionDaoSupport;
import org.springframework.stereotype.Repository;

/**
 * <b>项目名</b>： com.bitium10.commons <br>
 * <b>包名称</b>： com.bitium10.commons.cp.test <br>
 * <b>类名称</b>： MySQLTest <br>
 * <b>类描述</b>： <br>
 * <b>创建人</b>： <a href="mailto:wylipengming@chinabank.com.cn">李朋明</a> <br>
 * <b>修改人</b>： <br>
 * <b>创建时间</b>：2014/10/6 8:51
 * <b>修改时间</b>： <br>
 * <b>修改备注</b>： <br>
 *
 * @version 1.0.0 <br>
 */
@Repository("mySQLTest")
public class MySQLTest extends SqlSessionDaoSupport {
    public static final Logger logger = new Logger();

    public int count(){
        return this.getSqlSession().selectOne("TestMapper.findOne");
    }

}
