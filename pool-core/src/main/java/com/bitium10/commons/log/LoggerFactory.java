package com.bitium10.commons.log;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * <b>项目名</b>： com.bitium10.commons <br>
 * <b>包名称</b>： com.bitium10.commons.log <br>
 * <b>类名称</b>： LoggerFactory <br>
 * <b>类描述</b>： <br>
 * <b>创建人</b>： <a href="mailto:shouli1990@gmail.com">李朋明</a> <br>
 * <b>修改人</b>： <br>
 * <b>创建时间</b>：2014/10/5 21:35
 * <b>修改时间</b>： <br>
 * <b>修改备注</b>： <br>
 *
 * @version 1.0.0 <br>
 */
public class LoggerFactory {
    private static final ConcurrentMap<String, Logger> LOGGERS = new ConcurrentHashMap();

    public static Logger getLogger() {
        return new Logger(LoggerFactory.class);
    }

    public static Logger getLogger(String className) {
        Logger logger = (Logger)LOGGERS.get(className);
        if (logger == null) {
            LOGGERS.putIfAbsent(className, new Logger(className));
            logger = (Logger)LOGGERS.get(className);
        }
        return logger;
    }

    public static Logger getLogger(Class clazz) {
        return getLogger(clazz.getName());
    }
}
