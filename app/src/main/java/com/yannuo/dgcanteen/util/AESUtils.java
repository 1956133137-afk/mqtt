package com.yannuo.dgcanteen.util;

import android.util.Base64;

import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;


/**
 * Created by zhanmian on 2021-04-17 10:43
 */
public class AESUtils {

    /**
     * 加密
     *
     * @param key     加密密码
     * @param content 需要加密的内容
     * @return 加密内容
     */
    public static String encrypt(String key, String content) throws Exception{
        SecretKeySpec keySpec = new SecretKeySpec(key.getBytes(), "AES");
        Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, keySpec);
        byte[] result = cipher.doFinal(content.getBytes(StandardCharsets.UTF_8));
        return Base64.encodeToString(result,Base64.NO_WRAP);
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
//        byte[] result = cipher.doFinal(android.util.Base64.decode(content, android.util.Base64.DEFAULT));
        byte[] result = cipher.doFinal(Base64.decode(content, Base64.NO_WRAP));
        return new String(result, StandardCharsets.UTF_8);
    }

    public static String decryptMD5(String input) throws Exception{
        // 创建MD5加密对象
        MessageDigest md = MessageDigest.getInstance("MD5");
        // 执行加密操作
        byte[] messageDigest = md.digest(input.getBytes());
        // 将字节数组转换为16进制字符串
        StringBuilder hexString = new StringBuilder();
        for (byte b : messageDigest) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        // 返回加密后的字符串
        return hexString.toString();
    }


    public static String encryptDataRSA(byte[] data , String key){
        try {
//            java.util.Base64.getDecoder().decode(key);
            byte[] decode = Base64.decode(key, Base64.NO_WRAP);
//            LogUtil.i("RSAUtils","decode: "+ new String(decode));
            PublicKey rsa = KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(decode));
            Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            cipher.init(Cipher.ENCRYPT_MODE,rsa);
            byte[] encryptedBytes = cipher.doFinal(data);
            return Base64.encodeToString(encryptedBytes,Base64.NO_WRAP);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }
}
