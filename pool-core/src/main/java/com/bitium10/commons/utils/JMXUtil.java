package com.bitium10.commons.utils;

import javax.management.*;
import java.lang.management.ManagementFactory;

/**
 * <b>项目名</b>： com.bitium10.commons <br>
 * <b>包名称</b>： com.bitium10.commons.utils <br>
 * <b>类名称</b>： JMXUtil <br>
 * <b>类描述</b>： <br>
 * <b>创建人</b>： <a href="mailto:wylipengming@chinabank.com.cn">李朋明</a> <br>
 * <b>修改人</b>： <br>
 * <b>创建时间</b>：2014/10/5 21:48
 * <b>修改时间</b>： <br>
 * <b>修改备注</b>： <br>
 *
 * @version 1.0.0 <br>
 */
public class JMXUtil {
    public static ObjectName register(String name, Object mbean) {
        try {
            ObjectName objectName = new ObjectName(name);

            MBeanServer mbeanServer = ManagementFactory.getPlatformMBeanServer();
            try {
                mbeanServer.registerMBean(mbean, objectName);
            } catch (InstanceAlreadyExistsException ex) {
                mbeanServer.unregisterMBean(objectName);
                mbeanServer.registerMBean(mbean, objectName);
            }

            return objectName;
        } catch (JMException e) {
            throw new IllegalArgumentException(name, e);
        }
    }

    public static void unregister(String name) {
        try {
            MBeanServer mbeanServer = ManagementFactory.getPlatformMBeanServer();

            mbeanServer.unregisterMBean(new ObjectName(name));
        } catch (InstanceNotFoundException e) {
        } catch (JMException e) {
            throw new IllegalArgumentException(name, e);
        }
    }
}
