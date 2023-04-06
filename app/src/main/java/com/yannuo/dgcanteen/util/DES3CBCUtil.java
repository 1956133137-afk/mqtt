package com.yannuo.dgcanteen.util;

import android.os.Build;

import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

public class DES3CBCUtil {

    //加密模式
    private static final String encryptionMode = "3DES/CBC/pkcs7padding";
    //填充
    private static final String encryption = "3DES";
    //密码
    private static final String cipher = "123456781234567812345678";
    //加密向量
    private static final String encryptionVector = "00000000";
    //输出
    private static final String str = "base64";
    //字符集
    private static final String str1 = "utf8";


    /**
     *  3DES 解密
     */
    public static String decode(String decryption) throws Exception {
        SecretKey secretKey = new SecretKeySpec(cipher.getBytes(str1),encryption);
        Cipher cipher1 = Cipher.getInstance(encryptionMode);
        cipher1.init(Cipher.ENCRYPT_MODE,secretKey);
        byte[] decryptData = cipher1.doFinal(Base64.getDecoder().decode(decryption));
        return new String(decryptData,str1);
    }

}
