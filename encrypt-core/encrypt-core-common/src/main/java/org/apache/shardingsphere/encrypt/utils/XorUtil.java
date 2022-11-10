package org.apache.shardingsphere.encrypt.utils;

import java.util.Base64;

/**
 * XOR util.
 *
 * @author binyu.wu
 * @date 2022/11/7 16:28
 */
public class XorUtil {

    private final static byte[] ENCRYPT_VAL = {
            56, 68, 54, 52, 65, 53, 69, 48,
            52, 56, 57, 57, 69, 56, 56, 69,
            68, 48, 70, 55, 70, 50, 49, 51,
            65, 52, 68, 69, 54, 65, 53, 43
    };

    /**
     * 加密
     *
     * @param str
     * @return
     */
    public static String encrypt(String str) {
        byte[] bytes = str.getBytes();
        for (int i = 0; i < bytes.length; i++) {
            bytes[i] = (byte) (bytes[i] ^ ENCRYPT_VAL[i % ENCRYPT_VAL.length]);
        }
        return new String(Base64.getEncoder().encode(bytes));
    }

    /**
     * 解密
     *
     * @param str
     * @return
     */
    public static String decrypt(String str) {
        byte[] bytes = Base64.getDecoder().decode(str);
        for (int i = 0; i < bytes.length; i++) {
            bytes[i] = (byte) (bytes[i] ^ ENCRYPT_VAL[i % ENCRYPT_VAL.length]);
        }
        return new String(bytes);
    }

}
