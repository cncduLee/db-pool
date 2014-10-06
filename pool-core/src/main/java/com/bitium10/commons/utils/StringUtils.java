package commons.utils;

import java.text.DecimalFormat;

/**
 * <b>项目名</b>： db-pool <br>
 * <b>包名称</b>： com.bitium10.commons.utils <br>
 * <b>类名称</b>： StringUtils <br>
 * <b>类描述</b>： <br>
 * <b>创建人</b>： <a href="mailto:wylipengming@chinabank.com.cn">李朋明</a> <br>
 * <b>修改人</b>： <br>
 * <b>创建时间</b>：2014/10/1 20:22
 * <b>修改时间</b>： <br>
 * <b>修改备注</b>： <br>
 *
 * @version 1.0.0 <br>
 */
public class StringUtils extends org.apache.commons.lang.StringUtils {
    public static final String CRLF = System.getProperty("line.separator");

    public static final String centToDollar(String cent)
    {
        return transformNumber(cent, "0.00", 0.01D);
    }

    public static String centToDollarShort(String cent)
    {
        return transformNumber(cent, "###", 0.01D);
    }

    public static String dollarToCent(String dollar)
    {
        return transformNumber(dollar, "###", 100.0D);
    }

    private static String transformNumber(String str, String format, double rate)
    {
        if (isEmpty(str)) {
            return "0";
        }
        return new DecimalFormat(format).format(Double.parseDouble(str) * rate);
    }

    public static String protect(String s)
    {
        if (isEmpty(s)) {
            return s;
        }
        StringBuilder sb = new StringBuilder();
        int len = s.length();
        int clear = len > 6 ? 6 : 0;
        int lastFourIndex = -1;
        if (clear > 0) {
            lastFourIndex = s.indexOf('=') - 4;
            if (lastFourIndex < 0) {
                lastFourIndex = s.indexOf('^') - 4;
            }
            if ((lastFourIndex < 0) && (s.indexOf('^') < 0)) {
                lastFourIndex = s.indexOf('D') - 4;
            }
            if (lastFourIndex < 0) {
                lastFourIndex = len - 4;
            }
        }
        for (int i = 0; i < len; i++) {
            if ((s.charAt(i) == '=') || ((s.charAt(i) == 'D') && (s.indexOf('^') < 0))) {
                clear = 1;
            } else if (s.charAt(i) == '^') {
                lastFourIndex = 0;
                clear = len - i;
            } else if (i == lastFourIndex) {
                clear = 4;
            }
            sb.append(clear-- > 0 ? s.charAt(i) : '*');
        }
        s = sb.toString();
        int charCount = s.replaceAll("[^\\^]", "").length();
        if (charCount == 2) {
            s = s.substring(0, s.lastIndexOf("^") + 1);
            s = rightPad(s, len, '*');
        }
        return s;
    }
}
