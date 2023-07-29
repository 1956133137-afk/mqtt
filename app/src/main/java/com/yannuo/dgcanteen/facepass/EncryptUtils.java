package com.yannuo.dgcanteen.facepass;

import android.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

/**
 * Created by zhanmian on 2021-08-20 17:02
 */
public class EncryptUtils {

    /**
     * 加密
     *
     * @param key     加密密码
     * @param content 需要加密的内容
     * @return 加密内容
     */
    public static String encrypt(String key, String content) throws Exception {
        SecretKeySpec keySpec = new SecretKeySpec(key.getBytes(), "AES");
        Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, keySpec);
        byte[] result = cipher.doFinal(content.getBytes("UTF-8"));
        return Base64.encodeToString(result, Base64.DEFAULT);
    }

    /**
     * 解密
     *
     * @param key     解密密钥
     * @param content 需要解密的内容
     * @return 解密内容
     */
    public static String decrypt(String key, String content) throws Exception {
        if (content == null) {
            return null;
        }
        SecretKeySpec keySpec = new SecretKeySpec(key.getBytes(), "AES");
        Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, keySpec);
        byte[] decode = Base64.decode(content, Base64.NO_WRAP);
        return new String(cipher.doFinal(decode), "UTF-8");
    }
}
