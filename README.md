db-pool
===

db-pool

usage
======

db.properties
===

    jdbc.drive=com.mysql.jdbc.Driver
    jdbc.url=jdbc:mysql://localhost:3306/test?useUnicode=true&amp;characterEncoding=utf8
    jdbc.username=root
    jdbc.password=root
    #以上三个为线上测试数据的db
    #最小连接数，【根据需要配置】
    jdbc.min_connections = 1
    #最大连接数，【根据需要配置】
    jdbc.max_connections = 5
    #连接耗尽时，等待可用连接的最长时间(毫秒)，推荐5000毫秒
    jdbc.checkout_timeout = 5000
    #空闲连接的回收时间(秒)，推荐60秒
    jdbc.idle_timeout = 60


spring config
===

    <bean id="sampleDataSource" class="com.bitium10.commons.CPDataSource">
        <property name="propertiesLocation" value="classpath:db.properties" />
    </bean>
    
    <bean id="sampleSqlSessionFactory" class="org.mybatis.spring.SqlSessionFactoryBean">
        <property name="dataSource" ref="sampleDataSource" />
        <property name="configLocation" value="classpath:mybatis-config-sample.xml" />
    </bean>
    
    <bean id="sqlSessionTemplate" class="org.mybatis.spring.SqlSessionTemplate">
        <constructor-arg ref="sampleSqlSessionFactory" />
    </bean>
