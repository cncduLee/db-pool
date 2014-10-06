package com.bitium10.commons.cp.test;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.annotation.Resource;

import static org.junit.Assert.*;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath*:/spring.xml"})
public class MySQLTestTest {

    @Resource
    private MySQLTest mySQLTest;

    @Test
    public void testCount() throws Exception {
        int count = mySQLTest.count();
        System.out.println("count:"+count);
        Assert.assertEquals(2,count);
    }
}