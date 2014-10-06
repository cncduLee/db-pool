package commons.utils;

/**
 * <b>项目名</b>： com.bitium10.commons <br>
 * <b>包名称</b>： com.bitium10.commons.log <br>
 * <b>类名称</b>： Formatter <br>
 * <b>类描述</b>： <br>
 * <b>创建人</b>： <a href="mailto:wylipengming@chinabank.com.cn">李朋明</a> <br>
 * <b>修改人</b>： <br>
 * <b>创建时间</b>：2014/10/5 21:34
 * <b>修改时间</b>： <br>
 * <b>修改备注</b>： <br>
 *
 * @version 1.0.0 <br>
 */
public class Formatter {
    public static String fmtNumR(long num) {
        return ralign(String.valueOf(num), 8, ' ');
    }

    public static String fmtNumL(long num) {
        return lalign(String.valueOf(num), 8, ' ');
    }

    public static String fmtStrR(String str, int len) {
        if (str.getBytes().length >= len) {
            return str;
        }
        return ralign(str, len, ' ');
    }

    public static String fmtStrL(String str, int len) {
        if (str.getBytes().length >= len) {
            return str;
        }
        return lalign(str, len);
    }

    public static String ltrim(String str, int len) {
        byte[] bstr = str.getBytes();
        if (bstr.length <= len) {
            return str;
        }
        byte[] obs = new byte[len];
        int offset = bstr.length - len;
        System.arraycopy(bstr, offset, obs, 0, len);
        return new String(obs);
    }

    public static String rtrim(String str, int len) {
        byte[] bstr = str.getBytes();
        if (bstr.length <= len) {
            return str;
        }
        byte[] obs = new byte[len];
        System.arraycopy(bstr, 0, obs, 0, len);
        return new String(obs);
    }

    public static String ralign(String str, int len) {
        return ralign(str, len, '0');
    }

    public static String lalign(String str, int len) {
        return lalign(str, len, ' ');
    }

    public static String ralign(String str, int len, char fill) {
        return align(str, len, fill, false);
    }

    public static String lalign(String str, int len, char fill) {
        return align(str, len, fill, true);
    }

    public static String align(String str, int len, char fill, boolean leftAlign) {
        if (str == null) {
            str = "";
        }
        byte[] buf = new byte[len];
        byte[] bstr = str.getBytes();
        int str_len = bstr.length;
        int fill_len = len - str_len;
        if (leftAlign) {
            if (fill_len > 0)
                System.arraycopy(bstr, 0, buf, 0, str_len);
            else {
                System.arraycopy(bstr, 0, buf, 0, len);
            }
        } else if (fill_len > 0)
            System.arraycopy(bstr, 0, buf, fill_len, str_len);
        else {
            System.arraycopy(bstr, -fill_len, buf, 0, len);
        }

        for (int i = 0; i < fill_len; i++) {
            if (leftAlign)
                buf[(i + str_len)] = ((byte) fill);
            else {
                buf[i] = ((byte) fill);
            }
        }
        return new String(buf);
    }

    public static String trim(String str) {
        return str == null ? "" : str.trim();
    }

    public static String formatNS(long ns) {
        String src = String.valueOf(ns);
        int len = src.length();
        int count = len / 3;
        int first = len % 3;
        if ((count < 1) || ((count == 1) && (first == 0))) {
            return src;
        }
        if (first == 0) {
            first = 3;
            count--;
        }
        StringBuilder sb = new StringBuilder(len + count);
        for (int i = 0; i < len; i++) {
            sb.append(src.charAt(i));
            if (i + 1 == first)
                sb.append(',');
            else if ((i > first) && ((i + 1 - first) % 3 == 0) && (i + 1 < len)) {
                sb.append(',');
            }
        }
        String fmt = sb.toString();

        return fmt;
    }
}
