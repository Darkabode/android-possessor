package com.zer0.possessor;

import java.util.List;
import java.util.Random;

public class StringUtils
{
    private final static char[] ALLOWED_CHARS = "1234567890abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray();

    public static String randomString(int len, boolean lowerCase)
    {
        final StringBuilder buf = new StringBuilder();
        final Random rand = new Random();
        for (int i = 0; i < len; i++) {
            buf.append(ALLOWED_CHARS[rand.nextInt(ALLOWED_CHARS.length)]);
        }
        String val = buf.toString();
        if (lowerCase) {
            val = val.toLowerCase();
        }
        return val;
    }

    public static String toHexString(long num, int padding)
    {
        String val = Long.toHexString(num);
        int i = padding - val.length();
        for (; i > 0; --i) {
            val = "0" + val;
        }
        return val;
    }

    public static String joinStrings(List<String> list, String sep)
    {
        StringBuilder sb = new StringBuilder();
        String loopSep = "";

        for(String s : list) {
            sb.append(loopSep);
            sb.append(s);
            loopSep = sep;
        }

        return sb.toString();
    }

    public static String getHexFrom(byte[] data)
    {
        final char[] hexDigits = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};
        String hex = "";
        for (byte aData : data) {
            hex += hexDigits[aData & 0x0F];
            hex += hexDigits[(aData >> 4) & 0x0F];
        }

        return hex;
    }
}