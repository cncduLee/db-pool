package commons.log;

import com.bitium10.commons.utils.Formatter;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.concurrent.TimeUnit;

/**
 * <b>项目名</b>： com.bitium10.commons <br>
 * <b>包名称</b>： com.bitium10.commons.log <br>
 * <b>类名称</b>： LoggerBase <br>
 * <b>类描述</b>： <br>
 * <b>创建人</b>： <a href="mailto:wylipengming@chinabank.com.cn">李朋明</a> <br>
 * <b>修改人</b>： <br>
 * <b>创建时间</b>：2014/10/5 21:33
 * <b>修改时间</b>： <br>
 * <b>修改备注</b>： <br>
 * 包级别的私有
 * @version 1.0.0 <br>
 */
class LoggerBase {
    private static final String NA = "UnknowClass";
    private static final org.slf4j.Logger ACCESS_LOGGER = org.slf4j.LoggerFactory.getLogger(System.getProperty("logger.access.name", "ACCESS"));
    private static final org.slf4j.Logger PERF_LOGGER = org.slf4j.LoggerFactory.getLogger(System.getProperty("logger.performance.name", "PERFORMANCE"));

    private static ThreadLocal<Long> tlAccessStart = new ThreadLocal();
    private static final String NULL = "NULL";
    public static final String LINE_SEP = System.getProperty("line.separator");
    public static final int LINE_SEP_LEN = LINE_SEP.length();
    private final org.slf4j.Logger delegate;

    public LoggerBase(String cls)
    {
        this.delegate = org.slf4j.LoggerFactory.getLogger(cls);
    }

    public LoggerBase(Class clazz)
    {
        if ((clazz == LoggerFactory.class) || (clazz == Logger.class))
        {
            String CLASSNAME = new StringBuilder().append(clazz.getName()).append(".").toString();
            String cls = getCallingClassName(CLASSNAME);
            this.delegate = org.slf4j.LoggerFactory.getLogger(cls);
        } else {
            this.delegate = org.slf4j.LoggerFactory.getLogger(clazz);
        }
    }

    public boolean isTraceEnabled() {
        return this.delegate.isTraceEnabled();
    }

    public boolean isDebugEnabled() {
        return this.delegate.isDebugEnabled();
    }

    public boolean isInfoEnabled() {
        return this.delegate.isInfoEnabled();
    }

    public boolean isWarnEnabled() {
        return this.delegate.isWarnEnabled();
    }

    public boolean isErrorEnabled() {
        return this.delegate.isErrorEnabled();
    }

    public static void accessStart()
    {
        tlAccessStart.set(Long.valueOf(System.nanoTime()));
    }

    public static void access(Object[] args)
    {
        if (args == null) {
            args = new Object[] { "NULL" };
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < args.length; i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(unull(args[i]));
        }

        Long startNS = (Long)tlAccessStart.get();
        if (startNS != null) {
            sb.append(',').append(Formatter.formatNS(System.nanoTime() - startNS.longValue())).append("ns");
            tlAccessStart.remove();
        }
        ACCESS_LOGGER.info(sb.toString());
    }

    private static Object unull(Object obj) {
        return obj == null ? "NULL" : obj;
    }

    private static String concat(Object[] objects) {
        StringBuilder sb = new StringBuilder();
        concat(sb, objects);
        return sb.toString();
    }

    private static void concat(StringBuilder sb, Object[] objects) {
        for (int i = 0; (i < objects.length) && (
                (i + 1 != objects.length) || (!(objects[i] instanceof Throwable))); i++)
        {
            if ((objects[i] != null) && (objects[i].getClass().isArray()))
                concat(sb, (Object[])objects[i]);
            else
                sb.append(unull(objects[i]));
        }
    }

    private String logf(String format, Object[] args)
    {
        StringBuilder sb = new StringBuilder();
        java.util.Formatter formatter = new java.util.Formatter(sb);
        formatter.format(format, args);
        return sb.toString();
    }

    public void allf(String format, Object[] args) {
        if (this.delegate.isTraceEnabled())
            all(logf(format, args));
    }

    public void all(Object[] objects)
    {
        if (this.delegate.isTraceEnabled())
            if ((objects[(objects.length - 1)] instanceof Throwable))
                this.delegate.trace(concat(objects), (Throwable)objects[(objects.length - 1)]);
            else
                all(concat(objects));
    }

    public void all(Object obj)
    {
        if (!this.delegate.isTraceEnabled()) {
            return;
        }
        if ((obj instanceof Throwable))
            this.delegate.trace("StackTrace:", (Throwable)obj);
        else
            this.delegate.trace(obj == null ? "NULL" : obj.toString());
    }

    public void debugf(String format, Object[] args)
    {
        if (this.delegate.isDebugEnabled())
            debug(logf(format, args));
    }

    public void debug(Object[] objects)
    {
        if (this.delegate.isDebugEnabled())
            if ((objects[(objects.length - 1)] instanceof Throwable))
                this.delegate.debug(concat(objects), (Throwable)objects[(objects.length - 1)]);
            else
                debug(concat(objects));
    }

    public void debug(Object obj)
    {
        if (!this.delegate.isDebugEnabled()) {
            return;
        }
        if ((obj instanceof Throwable))
            this.delegate.debug("StackTrace:", (Throwable)obj);
        else
            this.delegate.debug(obj == null ? "NULL" : obj.toString());
    }

    public void infof(String format, Object[] args)
    {
        if (this.delegate.isInfoEnabled())
            info(logf(format, args));
    }

    public void info(Object[] objects)
    {
        if (this.delegate.isInfoEnabled())
            if ((objects[(objects.length - 1)] instanceof Throwable))
                this.delegate.info(concat(objects), (Throwable)objects[(objects.length - 1)]);
            else
                info(concat(objects));
    }

    public void info(Object obj)
    {
        if (!this.delegate.isInfoEnabled()) {
            return;
        }
        if ((obj instanceof Throwable))
            this.delegate.info("StackTrace:", (Throwable)obj);
        else
            this.delegate.info(obj == null ? "NULL" : obj.toString());
    }

    public void warnf(String format, Object[] args)
    {
        if (this.delegate.isWarnEnabled())
            warn(logf(format, args));
    }

    public void warn(Object[] objects)
    {
        if (this.delegate.isWarnEnabled())
            if ((objects[(objects.length - 1)] instanceof Throwable))
                this.delegate.warn(concat(objects), (Throwable)objects[(objects.length - 1)]);
            else
                warn(concat(objects));
    }

    public void warn(Object obj)
    {
        if (!this.delegate.isWarnEnabled()) {
            return;
        }
        if ((obj instanceof Throwable))
            this.delegate.warn("StackTrace:", (Throwable)obj);
        else
            this.delegate.warn(obj == null ? "NULL" : obj.toString());
    }

    public void errorf(String format, Object[] args)
    {
        if (this.delegate.isErrorEnabled())
            error(logf(format, args));
    }

    public void error(Object[] objects)
    {
        if (this.delegate.isErrorEnabled())
            if ((objects[(objects.length - 1)] instanceof Throwable))
                this.delegate.error(concat(objects), (Throwable)objects[(objects.length - 1)]);
            else
                error(concat(objects));
    }

    public void error(Object obj)
    {
        if (!this.delegate.isErrorEnabled()) {
            return;
        }
        if ((obj instanceof Throwable))
            this.delegate.error("StackTrace:", (Throwable)obj);
        else
            this.delegate.error(obj == null ? "NULL" : obj.toString());
    }

    private String getCallingClassName(String CLASSNAME)
    {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);

        new Throwable().printStackTrace(pw);
        String stack = sw.toString();

        int ibegin = stack.lastIndexOf(CLASSNAME);
        if (ibegin == -1) {
            return "UnknowClass";
        }

        ibegin = stack.indexOf(LINE_SEP, ibegin);
        if (ibegin == -1) {
            return "UnknowClass";
        }
        ibegin += LINE_SEP_LEN;

        int iend = stack.indexOf(LINE_SEP, ibegin);
        if (iend == -1) {
            return "UnknowClass";
        }

        ibegin = stack.lastIndexOf("at ", iend);
        if (ibegin == -1) {
            return "UnknowClass";
        }

        ibegin += 3;

        String fullInfo = stack.substring(ibegin, iend);

        iend = fullInfo.lastIndexOf('(');
        if (iend == -1) {
            return "UnknowClass";
        }
        iend = fullInfo.lastIndexOf('.', iend);

        if (iend == -1) {
            return "UnknowClass";
        }
        return fullInfo.substring(0, iend);
    }

    private static String getShortName(TimeUnit unit)
    {
        if (unit == TimeUnit.NANOSECONDS)
            return "ns";
        if (unit == TimeUnit.MICROSECONDS)
            return "micro";
        if (unit == TimeUnit.MILLISECONDS)
            return "ms";
        if (unit == TimeUnit.SECONDS)
            return "sec";
        if (unit == TimeUnit.MINUTES) {
            return "min";
        }
        return "...";
    }

    public static final void timeSpent(String info, long startTime, TimeUnit unit, long threshold, long delta)
    {
        long now;
        if (unit == TimeUnit.NANOSECONDS)
            now = System.nanoTime();
        else {
            now = unit.convert(System.currentTimeMillis(), TimeUnit.MILLISECONDS);
        }
        long spent = now - startTime;
        if (spent >= threshold)
            if (delta > 0L) {
                int multiple = (int)((spent - threshold) / delta);
                if (multiple == 0)
                    PERF_LOGGER.debug(concat(new Object[] { info, " spent ", Long.valueOf(spent), getShortName(unit), ", but expect in ", Long.valueOf(threshold), getShortName(unit) }));
                else if (multiple == 1)
                    PERF_LOGGER.info(concat(new Object[] { info, " spent ", Long.valueOf(spent), getShortName(unit), ", but expect in ", Long.valueOf(threshold), getShortName(unit) }));
                else
                    PERF_LOGGER.warn(concat(new Object[] { info, " spent ", Long.valueOf(spent), getShortName(unit), ", but expect in ", Long.valueOf(threshold), getShortName(unit) }));
            }
            else {
                PERF_LOGGER.debug(concat(new Object[] { info, " spent ", Long.valueOf(spent), getShortName(unit), ", but expect in ", Long.valueOf(threshold), getShortName(unit) }));
            }
    }

    public static final void timeSpent(String info, long startTime, TimeUnit unit, long threshold)
    {
        timeSpent(info, startTime, unit, threshold, threshold);
    }

    public static final void timeSpentNan(String info, long startTime, long threshold)
    {
        timeSpent(info, startTime, TimeUnit.NANOSECONDS, threshold, threshold);
    }

    public static final void timeSpentNan(String info, long startTime, long threshold, long delta)
    {
        timeSpent(info, startTime, TimeUnit.NANOSECONDS, threshold, delta);
    }

    public static final void timeSpentMillSec(String info, long startTime, long threshold)
    {
        timeSpent(info, startTime, TimeUnit.MILLISECONDS, threshold, threshold);
    }

    public static final void timeSpentMillSec(String info, long startTime, long threshold, long delta)
    {
        timeSpent(info, startTime, TimeUnit.MILLISECONDS, threshold, delta);
    }
}
