package com.yannuo.dgcanteen.util;

import android.util.Log;

import com.yannuo.dgcanteen.model.CcbScanPayBean;

import java.io.UnsupportedEncodingException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.ShortBufferException;

import COM.CCB.EnDecryptAlgorithm.MCipherEncryptor;

public class CanteenEncryptionUtil {

    private final static String CANTEEN_TEST_URL = "http://121.40.54.232:8090/CCBIS/B2CMainPlat_00_ZHST";
    private final static String CCB_IBSVersion = "V6";
    private final static String PT_STYLE = "8";
    private final static String PT_LANGUAGE = "CN";
    private final static String STR_KEY = "MKnzkGMRe08NmPv2TP6YbEzMOdjZzeEG";

    public static String requestScanData(CcbScanPayBean bean){
        StringBuilder param = new StringBuilder();
        param.append("BUSINESS_ID=" + bean.getBUSINESS_ID()).append("&VPOS_ID=" + bean.getVPOS_ID()).append("&PAYMENT=" + bean.getPAYMENT())
                .append("&ACTUAL_PAYMENT=" + bean.getACTUAL_PAYMENT()).append("&COUPON_INFO=" + bean.getCOUPON_INFO()).append("&ACC_NOS=" + bean.getACC_NOS())
                .append("&QR_CODE=" + bean.getQR_CODE()).append("&CUST_ID=" + bean.getCUST_ID()).append("&ORDER_ID=" + bean.getORDER_ID())
                .append("&OFFLINE=" + bean.getOFFLINE()).append("&SIGN_TIME=" + bean.getSIGN_TIME());

        bean.setCcbSafeParam(encryption(param.toString()));

        param = new StringBuilder();
        param.append(CANTEEN_TEST_URL).append("? CCB_IBSVersion=" + CCB_IBSVersion).append("&PT_STYLE=" + PT_STYLE).append("&PT_LANGUAGE=" + PT_LANGUAGE)
                .append("&CAMPUS_ID=" + bean.getCAMPUS_ID()).append("&TXCODE=" + bean.getTXCODE()).append("&CORP_ID=" + bean.getCORP_ID())
                .append("&ccbSafeParam=" + bean.getCcbSafeParam());

        return param.toString();
    }

    public static String encryption(String param){

        try{
            //创建加密对象，向构造函数传入密钥
            MCipherEncryptor ccbEncryptor = new MCipherEncryptor(STR_KEY);

            //执行加密
            String ccbSafeParam = ccbEncryptor.doEncrypt(param);

            return ccbSafeParam;
        }catch (InvalidKeyException | NoSuchAlgorithmException | NoSuchPaddingException |
                ShortBufferException | IllegalBlockSizeException | BadPaddingException |
                NoSuchProviderException | InvalidAlgorithmParameterException |
                UnsupportedEncodingException e){
            e.printStackTrace();
        }

        return "";
    }
}
