package commons.utils;

import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.Closeable;
import java.io.IOException;
import java.sql.Driver;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * <b>项目名</b>： db-pool <br>
 * <b>包名称</b>： com.bitium10.commons.utils <br>
 * <b>类名称</b>： JdbcUtil <br>
 * <b>类描述</b>： <br>
 * <b>创建人</b>： <a href="mailto:wylipengming@chinabank.com.cn">李朋明</a> <br>
 * <b>修改人</b>： <br>
 * <b>创建时间</b>：2014/10/1 18:19
 * <b>修改时间</b>： <br>
 * <b>修改备注</b>： <br>
 *
 * @version 1.0.0 <br>
 */
public class JdbcUtil {
    private static final Logger LOGGER = LoggerFactory.getLogger(JdbcUtil.class);

    public static final String CRLF = System.getProperty("line.separator");

    private static byte[] pwdAesRawKey = "f[j@R#?]qM(#}rI$".getBytes();
    private static byte[] IV = pwdAesRawKey;

    public static Driver createDriver(String driverClassName) throws SQLException
    {
        Class clazz = null;
        try
        {
            ClassLoader contextLoader = Thread.currentThread().getContextClassLoader();
            if (contextLoader != null) {
                clazz = contextLoader.loadClass(driverClassName);
            }
        }
        catch (ClassNotFoundException e)
        {
        }
        if (clazz == null) {
            try {
                clazz = Class.forName(driverClassName);
            } catch (ClassNotFoundException e) {
                throw new SQLException(e.getMessage(), e);
            }
        }
        try
        {
            return (Driver)clazz.newInstance();
        } catch (IllegalAccessException e) {
            throw new SQLException(e.getMessage(), e);
        } catch (InstantiationException e) {
            throw new SQLException(e.getMessage(), e);
        }
    }

    public static void closeQuietly(Closeable closeable) {
        try {
            if (closeable != null)
                closeable.close();
        }
        catch (IOException ioe)
        {
        }
    }

    public static void closeQuietly(ResultSet rs)
    {
        if (rs != null)
            try {
                rs.close();
            } catch (SQLException e) {
                LOGGER.warn("E close(ResultSet)...", e);
            }
    }

    public static void closeQuietly(Statement stmt)
    {
        if (stmt != null)
            try {
                stmt.close();
            } catch (SQLException e) {
                LOGGER.warn("E close(Statement)...", e);
            }
    }

    public static String decodePassword(String secretBase64)
            throws Exception
    {
        SecretKeySpec key = new SecretKeySpec(pwdAesRawKey, "AES");
        IvParameterSpec iv = new IvParameterSpec(IV);
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(2, key, iv);
        byte[] decode = cipher.doFinal(Base64.decodeBase64(secretBase64));
        return new String(decode, "UTF-8");
    }

    public static String multiLinesToOneLine(String lines, String replacement)
    {
        if (replacement == null) {
            replacement = "";
        }
        String str1 = replace(lines, "\r\n", replacement);
        String str2 = replace(str1, "\r", replacement);
        String str3 = replace(str2, "\n", replacement);
        return str3;
    }

    public static String replace(String text, String searchString, String replacement)
    {
        return replace(text, searchString, replacement, -1);
    }

    public static String replace(String text, String searchString, String replacement, int max) {
        if ((isEmpty(text)) || (isEmpty(searchString)) || (replacement == null) || (max == 0)) {
            return text;
        }
        int start = 0;
        int end = text.indexOf(searchString, start);
        if (end == -1) {
            return text;
        }
        int replLength = searchString.length();
        int increase = replacement.length() - replLength;
        increase = increase < 0 ? 0 : increase;
        increase *= (max > 64 ? 64 : max < 0 ? 16 : max);
        StringBuilder buf = new StringBuilder(text.length() + increase);
        while (end != -1) {
            buf.append(text.substring(start, end)).append(replacement);
            start = end + replLength;
            max--; if (max == 0) {
                break;
            }
            end = text.indexOf(searchString, start);
        }
        buf.append(text.substring(start));
        return buf.toString();
    }

    public static boolean isEmpty(CharSequence cs) {
        return (cs == null) || (cs.length() == 0);
    }
}
