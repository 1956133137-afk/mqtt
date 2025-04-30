package com.yannuo.dgcanteen.util;

import android.util.Log;

import com.yannuo.dgcanteen.model.PayForUI;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.util.HashMap;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.ShortBufferException;

import COM.CCB.EnDecryptAlgorithm.MCipherDecryptor;
import COM.CCB.EnDecryptAlgorithm.MCipherEncryptor;

public class CanteenEncryptionUtil {

    private final static String CANTEEN_TEST_URL = "http://124.127.94.58:28880/CCBIS/B2CMainPlat_00_ZHST";
    private final static String CCB_IBSVersion = "V6";
    private final static String PT_STYLE = "8";
    private final static String PT_LANGUAGE = "CN";
    private final static String STR_KEY = Constant.STR_KEY;
//    private final static String STR_KEY = "MKnzkGMRe08NmPv2TP6YbEzMOdjZzeEG"; //测试
//    private final static String STR_KEY = "RReTnEXt6ebGdVfMybRrWU5CC46pJ9Mu"; //生产

    /**
     * @param CAMPUS_ID
     * @param TXCODE
     * @param CORP_ID
     * @param param
     * @return
     */
    private static HashMap<String, String> getSamePart(String CAMPUS_ID, String TXCODE, String CORP_ID, String param) {
        HashMap<String, String> map = new HashMap<String, String>();
        map.put("CCB_IBSVersion", CCB_IBSVersion);
        map.put("PT_STYLE", PT_STYLE);
        map.put("PT_LANGUAGE", PT_LANGUAGE);
        map.put("CAMPUS_ID", CAMPUS_ID);
        map.put("TXCODE", TXCODE);
        map.put("CORP_ID", CORP_ID);
        map.put("ccbSafeParam", encryption(param));
        return map;
    }

    /**
     * 解析二维码
     *
     * @return
     */

    public static HashMap<String, String> getAnalysisQr(PayForUI payForUI, String TXCODE) {
        return getSamePart(payForUI.getCampusId(), TXCODE, payForUI.getCorpId(), "QR_CODE=" + payForUI.getPayContent());
    }

    public static HashMap<String, String> getAnalysisQr(String campusId, String TXCODE, String corpId, String qrCode) {
        return getSamePart(campusId, TXCODE, corpId, "QR_CODE=" + qrCode);
    }

    /**
     * 解析核销码
     *
     * @param code
     * @return
     */
    public static HashMap<String, String> getAnalysisCode(String code) {
        HashMap<String, String> ccbParam = new HashMap<>();
        try {
            MCipherDecryptor ccbDecryptor = new MCipherDecryptor(STR_KEY);
            StringBuilder ccbSafeParam = new StringBuilder(ccbDecryptor.doDecrypt(code));
            String sn = Utils.getSN();
            String cardId = "";
            ccbSafeParam.append("&DEVICE_ID=" + sn)
                    .append("&CARD_ID=" + cardId);
            Log.d("TAG", "getAnalysisCode:" + ccbSafeParam);
            ccbParam = storeInfo(String.valueOf(ccbSafeParam));
        } catch (InvalidKeyException | NoSuchAlgorithmException | NoSuchPaddingException |
                 ShortBufferException | IllegalBlockSizeException | BadPaddingException |
                 NoSuchProviderException | InvalidAlgorithmParameterException |
                 IOException e) {
            e.printStackTrace();
        }
        return ccbParam;
    }

    /**
     * 请求报文加密方法
     *
     * @param param
     * @return
     */
    public static String encryption(String param) {

        try {
            Log.d("TAG", "encryption: " + param);
            //创建加密对象，向构造函数传入密钥
            MCipherEncryptor ccbEncryptor = new MCipherEncryptor(STR_KEY);

            //执行加密
            String ccbSafeParam = ccbEncryptor.doEncrypt(param);

            return ccbSafeParam;
        } catch (InvalidKeyException | NoSuchAlgorithmException | NoSuchPaddingException |
                 ShortBufferException | IllegalBlockSizeException | BadPaddingException |
                 NoSuchProviderException | InvalidAlgorithmParameterException |
                 UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        return "";
    }

    /**
     * 请求报文解密方法
     * @param param
     */
    public static String decryption(String param){
        try {
            //创建加密对象，向构造函数传入密钥
            MCipherDecryptor ccbEncryptor = new MCipherDecryptor(STR_KEY);
            //执行加密
            String ccbSafeParam = ccbEncryptor.doDecrypt(param);
            return ccbSafeParam;
        } catch (InvalidKeyException | NoSuchAlgorithmException | NoSuchPaddingException |
                 ShortBufferException | IllegalBlockSizeException | BadPaddingException |
                 NoSuchProviderException | InvalidAlgorithmParameterException |
                 UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return "";
    }

    //将字符串拆分为键值对，同时存储到HashMap中
    private static HashMap<String, String> storeInfo(String ccbSafeParam) {
        String[] pairs = ccbSafeParam.split("&");
        HashMap<String, String> map = new HashMap<>();
        for (String pair : pairs) {
            String[] keyValue = pair.split("=");
            if (keyValue.length == 2) {
                map.put(keyValue[0], keyValue[1]);
            }
        }
        return map;
    }
}
