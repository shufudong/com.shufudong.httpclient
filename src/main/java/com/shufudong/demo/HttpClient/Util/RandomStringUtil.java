package com.shufudong.demo.HttpClient.Util;

import java.util.Random;

/** 
* @ClassName:   [中]RandomStringUtil 
* @Description: [中]生成随机字符串的工具类
* @author       [中]ShuFuDong
* @date         [中]2015年9月5日 上午2:26:16 
*/ 
public class RandomStringUtil {

    /**
     * [中]使用Random对象的random方法，以避免同一毫秒返回相同的随机值.
     */
    private static final Random RANDOM = new Random();

    public RandomStringUtil() {
      super();
    }

    /** 
    * [中]创建一个固定长度的随机字符串,字符将从所有字符集之间选择
    * @param count      [中]随机字符串的长度
    * @return           [中]随机字符串
    */
    public static String random(int count) {
        return random(count, false, false);
    }

    /** 
    * [中]创建一个固定长度的随机字符串,字符将从字符集的ASCII值32和126之间选择.
    * @param count      [中]随机字符串的长度
    * @return           [中]随机字符串
    */
    public static String randomAscii(int count) {
        return random(count, 32, 127, false, false);
    }
    
    /** 
    * [中]创建一个固定长度的随机字符串,字符将从字符集[a-zA-Z]之间选择
    * @param count      [中]随机字符串的长度
    * @return           [中]随机字符串
    */
    public static String randomAlphabetic(int count) {
        return random(count, true, false);
    }
    
    /** 
     * [中]创建一个固定长度的随机字符串,字符将从字符集[a-zA-Z0-9]之间选择
     * @param count      [中]随机字符串的长度
     * @return           [中]随机字符串
     */
    public static String randomAlphanumeric(int count) {
        return random(count, true, true);
    }
    
    /** 
     * [中]创建一个固定长度的随机字符串,字符将从字符集[0-9]之间选择
     * @param count      [中]随机字符串的长度
     * @return           [中]随机字符串
     */
    public static String randomNumeric(int count) {
        return random(count, false, true);
    }

    public static String random(int count, boolean letters, boolean numbers) {
        return random(count, 0, 0, letters, numbers);
    }
    
    public static String random(int count, int start, int end, boolean letters, boolean numbers) {
        return random(count, start, end, letters, numbers, null, RANDOM);
    }

    public static String random(int count, int start, int end, boolean letters, boolean numbers, char[] chars) {
        return random(count, start, end, letters, numbers, chars, RANDOM);
    }

    public static String random(int count, int start, int end, boolean letters, boolean numbers,
                                char[] chars, Random random) {
        if (count == 0) {
            return "";
        } else if (count < 0) {
            throw new IllegalArgumentException("Requested random string length " + count + " is less than 0.");
        }
        if ((start == 0) && (end == 0)) {
            end = 'z' + 1;
            start = ' ';
            if (!letters && !numbers) {
                start = 0;
                end = Integer.MAX_VALUE;
            }
        }

        char[] buffer = new char[count];
        int gap = end - start;

        while (count-- != 0) {
            char ch;
            if (chars == null) {
                ch = (char) (random.nextInt(gap) + start);
            } else {
                ch = chars[random.nextInt(gap) + start];
            }
            if ((letters && Character.isLetter(ch))
                || (numbers && Character.isDigit(ch))
                || (!letters && !numbers)) 
            {
                if(ch >= 56320 && ch <= 57343) {
                    if(count == 0) {
                        count++;
                    } else {
                        // low surrogate, insert high surrogate after putting it in
                        buffer[count] = ch;
                        count--;
                        buffer[count] = (char) (55296 + random.nextInt(128));
                    }
                } else if(ch >= 55296 && ch <= 56191) {
                    if(count == 0) {
                        count++;
                    } else {
                        // high surrogate, insert low surrogate before putting it in
                        buffer[count] = (char) (56320 + random.nextInt(128));
                        count--;
                        buffer[count] = ch;
                    }
                } else if(ch >= 56192 && ch <= 56319) {
                    // private high surrogate, no effing clue, so skip it
                    count++;
                } else {
                    buffer[count] = ch;
                }
            } else {
                count++;
            }
        }
        return new String(buffer);
    }

    /** 
    * [中]创建一个固定长度的随机字符串,字符将从参数chars字符串中选择
    * @param count      [中]随机字符串的长度
    * @param chars      [中]字符选取的范围
    * @return           [中]随机字符串 
    */
    public static String random(int count, String chars) {
        if (chars == null) {
            return random(count, 0, 0, false, false, null, RANDOM);
        }
        return random(count, chars.toCharArray());
    }

    /** 
    * [中]创建一个固定长度的随机字符串,字符将从参数chars数组中选择
    * @param count      [中]随机字符串的长度
    * @param chars      [中]字符选取的范围
    * @return           [中]随机字符串 
    */
    public static String random(int count, char[] chars) {
        if (chars == null) {
            return random(count, 0, 0, false, false, null, RANDOM);
        }
        return random(count, 0, chars.length, false, false, chars, RANDOM);
    }
}
