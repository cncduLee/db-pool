package com.bitium10.commons.log;

/**
 * <b>项目名</b>： com.bitium10.commons <br>
 * <b>包名称</b>： com.bitium10.commons.log <br>
 * <b>类名称</b>： Logger <br>
 * <b>类描述</b>： <br>
 * <b>创建人</b>： <a href="mailto:wylipengming@chinabank.com.cn">李朋明</a> <br>
 * <b>修改人</b>： <br>
 * <b>创建时间</b>：2014/10/5 21:36
 * <b>修改时间</b>： <br>
 * <b>修改备注</b>： <br>
 *
 * @version 1.0.0 <br>
 */
public class Logger extends LoggerBase {
    public Logger() {
        super(Logger.class);
    }

    public Logger(String cls) {
        super(cls);
    }

    public Logger(Class cls) {
        super(cls);
    }
}
