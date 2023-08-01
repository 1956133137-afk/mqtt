package com.yannuo.dgcanteen.util;

import com.yannuo.dgcanteen.dao.CardPay;
import com.yannuo.dgcanteen.dao.OffLineTable;

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

import COM.CCB.EnDecryptAlgorithm.MCipherEncryptor;

public class CanteenEncryptionUtil {

    private final static String CANTEEN_TEST_URL = "http://121.40.54.232:8090/CCBIS/B2CMainPlat_00_ZHST";
    private final static String CCB_IBSVersion = "V6";
    private final static String PT_STYLE = "8";
    private final static String PT_LANGUAGE = "CN";
    private final static String STR_KEY = "MKnzkGMRe08NmPv2TP6YbEzMOdjZzeEG";
//    private final static String STR_KEY = "RReTnEXt6ebGdVfMybRrWU5CC46pJ9Mu";

    /**
     * @param CAMPUS_ID
     * @param TXCODE
     * @param CORP_ID
     * @param param
     * @return
     */
    private static HashMap<String, String> getSamePart(String CAMPUS_ID, String TXCODE, String CORP_ID, String param){
        HashMap<String, String> map = new HashMap<String,String>();
        map.put("CCB_IBSVersion", CCB_IBSVersion);
        map.put("PT_STYLE", PT_STYLE);
        map.put("PT_LANGUAGE",PT_LANGUAGE);
        map.put("CAMPUS_ID", CAMPUS_ID);
        map.put("TXCODE", TXCODE);
        map.put("CORP_ID", CORP_ID);
        map.put("ccbSafeParam", encryption(param));
        return map;
    }

    /**
     * 解析二维码
     * @param ccbBean
     * @return
     */
    public static HashMap<String, String> getAnalysisQr(OffLineTable ccbBean){
        return getSamePart(ccbBean.getCAMPUS_ID(),
                            ccbBean.getTXCODE(),
                            ccbBean.getCORP_ID(),
                     "QR_CODE=" + ccbBean.getQR_CODE());
    }

    /**
     * 扫码支付
     * @param ccbBean
     * @return
     */
    public static HashMap<String, String> getScanToPay(OffLineTable ccbBean){
        StringBuilder param = new StringBuilder();
        param.append("BUSINESS_ID=" + ccbBean.getBUSINESS_ID())
             .append("&VPOS_ID=" + ccbBean.getVPOS_ID())
             .append("&PAYMENT=" + ccbBean.getPAYMENT())
             .append("&ACTUAL_PAYMENT=" + ccbBean.getACTUAL_PAYMENT())
             .append("&COUPON_INFO=" + ccbBean.getCOUPON_INFO())
             .append("&QR_CODE=" + ccbBean.getQR_CODE())
             .append("&CUST_ID=" + ccbBean.getCUST_ID())
             .append("&ORDER_ID=" + ccbBean.getORDER_ID())
             .append("&OFFLINE=" + ccbBean.getOFFLINE())
             .append("&SIGN_TIME=" + ccbBean.getSIGN_TIME());

        LogUtil.e("TEST", param.toString());

        return getSamePart(ccbBean.getCAMPUS_ID(),
                            ccbBean.getTXCODE(),
                            ccbBean.getCORP_ID(),
                            param.toString());
    }

    /**
     * 查询记录
     * @param ccbBean
     * @return
     */
    public static HashMap<String, String> getQueryRecord(OffLineTable ccbBean){
        return getSamePart(ccbBean.getCAMPUS_ID(),
                            ccbBean.getTXCODE(),
                            ccbBean.getCORP_ID(),
                    "ORDER_ID=" + ccbBean.getORDER_ID());
    }

    /**
     * 刷卡支付
     * @param payBean
     * @return
     */
    public static HashMap<String, String> getCardToPay(CardPay payBean){

        StringBuilder param = new StringBuilder();
        param.append("BUSINESS_ID=" + payBean.getBusiness_id())
             .append("&VPOS_ID=" + payBean.getVpos_id())
             .append("&PAYMENT=" + payBean.getPayment())
             .append("&ACTUAL_PAYMENT=" + payBean.getActual_payment())
             .append("&COUPON_INFO=" + payBean.getCoupon_info())
             .append("&OFFLINE=" + payBean.getOffline())
             .append("&SIGN_TIME=" + payBean.getSign_time())
             .append("&CARD_ID=" + payBean.getCard_id())
             .append("&CUST_ID=" + payBean.getCust_id())
             .append("&ORDER_ID=" + payBean.getOrder_id());

        return getSamePart(payBean.getCampus_id(),
                            payBean.getTxcode(),
                            payBean.getCorp_id(),
                            param.toString());
    }

    /**
     * 请求报文加密方法
     * @param param
     * @return
     */
    private static String encryption(String param){

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
