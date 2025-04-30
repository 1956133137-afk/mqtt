package com.yannuo.dgcanteen.util;

import android.util.Base64;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.spec.KeySpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Random;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESKeySpec;
import javax.crypto.spec.DESedeKeySpec;
import javax.crypto.spec.IvParameterSpec;

import sun.misc.BASE64Decoder;

public class DES3CBCUtil {

    private static final String encryptionMode = "DESede/CBC/PKCS7Padding"; //加密模式
    private static final String encryption = "DESede";  //填充
    private static final String cipher = Constant.CIPHER;   //密码
    private static final String encryptionVector = Constant.ENCRYPTION_VECTOR;  //加密向量

    // 私钥
    private static final String PRIVATE_KEY = "MIICdQIBADANBgkqhkiG9w0BAQEFAASCAl8wggJbAgEAAoGBAIG" +
            "RJ0RqOaaYrem6zmTo/SF2OROMcJwRws/b05kaG0N90ZKFdRucIuiWvCiU4y9LLD6yNaCIyDGH0VubFOGnwzF" +
            "7BqGR4LTJgCHtfYodkE8XA99/P/cT/gi38uoX+UBnjxR2WeJPhHEr59tvVejb93KJQPMnhs7wJnxXYycTmJu" +
            "LAgMBAAECgYAvoMcZfB7jIb7Ua2oRaCAc29ORXw/KHzFIrVs0LYeWILsYLFznIFcovrg+BrUYnn6OMX5LG9z" +
            "TcETCctiTNtMmaBwG6J41GNWdwwJDdTmhjXs/jh6q1Wp3oT4jzlJWDozrwTWnzxFg/zoywntSFd46xzlt0YI" +
            "XxpSQR8e0WxktYQJBAME/MTnp8csABoZ/OGhIS4qbxlVayHS+H8qCOU1mdb/aYDoiqf94LYpebqkCwcerjhz" +
            "02ZX9xwqWTA2TUib8p1ECQQCrpDDi/M+mU2f63qs66usmLCIeJpaD56AeYIm5TdVC7PI6ZOx/FsBxJGkk1b6" +
            "pmrOEAKQ7lFhMoq4UZ2I28hYbAkAqQF/J8s2L/ehvVbeGjW/+0UpO9Tdo1vzqcQiIVMOf++YYL+YNVkBWxYj" +
            "aaSDnQColSJ+ePMtdFDlyqmhG3+zRAkAghHq+hibQ2/xXCthl0Ru7n6DXFXhuhPNQzflJofVFOJ6rcXNcoHL" +
            "U/JDu6Y+1khlwaK60muYfnrJcKznwLu0BAkAKhJcHprKRRCJpT//A169jrbfuX1B6mFcOGXwPzO2s1JYzUlX" +
            "CU4ylOVrLmdOpV+e7OSkrKNihVeIUm+TJt4MK";
    // 公钥
    private static final String PUBLIC_KEY = "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQCBkSdEajmmmK3p" +
            "us5k6P0hdjkTjHCcEcLP29OZGhtDfdGShXUbnCLolrwolOMvSyw+sjWgiMgxh9FbmxThp8MxewahkeC0yYAh" +
            "7X2KHZBPFwPffz/3E/4It/LqF/lAZ48UdlniT4RxK+fbb1Xo2/dyiUDzJ4bO8CZ8V2MnE5ibiwIDAQAB";

    /**
     * 返回离线码过期时间戳
     */
    public static long getTimestamp(String PlainStr) {
        return Long.parseLong(PlainStr.substring(PlainStr.lastIndexOf("@") + 1));
    }

    /**
     * 获取密文串
     *
     * @param src
     * @return
     */
    public static String transDecryption(String src) {
        return decode(src.substring(3, src.indexOf("@")).replace(",", "+"));
    }

    /**
     * 3DES
     */
    private static String decode(String decryption) {
        try {
            DESedeKeySpec spec = new DESedeKeySpec(cipher.getBytes(StandardCharsets.UTF_8));
            SecretKey deskey = SecretKeyFactory.getInstance(encryption).generateSecret(spec);
            Cipher cipher1 = Cipher.getInstance(encryptionMode);
            IvParameterSpec ips = new IvParameterSpec(encryptionVector.getBytes(StandardCharsets.UTF_8));
            cipher1.init(Cipher.DECRYPT_MODE, (Key) deskey, ips);
            byte[] bOut = cipher1.doFinal(Base64.decode(decryption.getBytes(StandardCharsets.UTF_8), Base64.DEFAULT));
            return new String(bOut, StandardCharsets.UTF_8);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    public static String decryptRSA(String result) {
        String res = "";

        try {
            byte[] rsaDeBytes = Base64.decode(PRIVATE_KEY.getBytes(), Base64.NO_WRAP);
            PKCS8EncodedKeySpec rsaDeKeySpec = new PKCS8EncodedKeySpec(rsaDeBytes);
            KeyFactory rsaDeFactory = KeyFactory.getInstance("RSA");
            PrivateKey privateKey = rsaDeFactory.generatePrivate(rsaDeKeySpec);
            Cipher rsaDeCipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            rsaDeCipher.init(Cipher.DECRYPT_MODE, privateKey);
            byte[] rsaDeMsgBytes = rsaDeCipher.doFinal(Base64.decode(result.substring(result.length() - 172), Base64.NO_WRAP));
            String ppk = new String(rsaDeMsgBytes, "utf-8");

            // dse解密
            Cipher deCipher = Cipher.getInstance("DES/ECB/PKCS5Padding");
            KeySpec deKeySpec = new DESKeySpec(ppk.getBytes());
            SecretKeyFactory deDecretKeyFactory = SecretKeyFactory.getInstance("DES");
            SecretKey deSecretKey = deDecretKeyFactory.generateSecret(deKeySpec);
            deCipher.init(Cipher.DECRYPT_MODE, deSecretKey, new SecureRandom());
            byte[] deMsgBytes = deCipher.doFinal(Base64.decode(result.substring(0, result.length() - 172), Base64.NO_WRAP));
            res = new String(deMsgBytes);
        } catch (Exception e) {
            LogUtil.e("decryptRSA", e.getMessage());
        }
        return res;
    }

    public static String encryption(String data) {
        String key = "";
        String encrypString = "";
        while (key.length() < 8) {
            key += String.valueOf(new Random().nextInt(10));
        }

        System.out.println("key:" + key);
        try {
            // des 加密
            Cipher cipher = Cipher.getInstance("DES/ECB/PKCS5Padding");
            SecretKey secretKey = SecretKeyFactory.getInstance("DES").generateSecret(new DESKeySpec(key.getBytes()));
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, new SecureRandom());
            byte[] enMsgBytes = cipher.doFinal(data.getBytes("UTF-8"));

            String desEnMsg = Base64Util.encode(enMsgBytes);
            // rsa 加密
            byte[] rsaEnBytes = new BASE64Decoder().decodeBuffer(PUBLIC_KEY);
            PublicKey Key = KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(rsaEnBytes));
            Cipher rsaEnCipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            rsaEnCipher.init(Cipher.ENCRYPT_MODE, Key);
            byte[] rsaEnMsgBytes = rsaEnCipher.doFinal(key.getBytes("UTF-8"));

            String rsaEnMsg = Base64Util.encode(rsaEnMsgBytes);

            System.out.println("rsaEnMsg:" + rsaEnMsg);
            System.out.println("desEnMsg:" + desEnMsg);
            encrypString = desEnMsg + rsaEnMsg;
            return encrypString;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return encrypString;
    }
}
