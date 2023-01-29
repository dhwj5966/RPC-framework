package github.starry.utils;


public class StringUtil {
    public static void main(String[] args) {
        boolean asdas_ = StringUtil.isBlank(" ");
        System.out.println(asdas_);

    }

    public static boolean isBlank(String s) {
        if (s == null || s.length() == 0) {
            return true;
        }
        //只要有一个不是空的字符。
        for (int i = 0; i < s.length(); ++i) {
            if (!Character.isWhitespace(s.charAt(i))) {
                return false;
            }
        }
        return true;
    }
}
