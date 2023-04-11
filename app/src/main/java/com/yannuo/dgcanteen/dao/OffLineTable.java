package com.yannuo.dgcanteen.dao;

import org.greenrobot.greendao.annotation.Entity;
import org.greenrobot.greendao.annotation.Id;
import org.greenrobot.greendao.annotation.NotNull;
import org.greenrobot.greendao.annotation.Generated;

@Entity
public class OffLineTable {

    @Id(autoincrement = true)
    private Long id;
    @NotNull
    private String CAMPUS_ID;       //园区ID
    @NotNull
    private String CORP_ID;         //合作方ID
    @NotNull
    private String TXCODE;           //交易码 PAY003
    @NotNull
    private String ccbSafeParam;     //加密参数串
    @NotNull
    private String BUSINESS_ID;      //商家编号
    @NotNull
    private String VPOS_ID;          //柜台编号
    @NotNull
    private String PAYMENT;          //支付金额
    @NotNull
    private String ACTUAL_PAYMENT;   //实际支付金额
    private String COUPON_INFO;      //优惠信息描述
    private String ACC_NOS;          //用户可使用的账户
    @NotNull
    private String QR_CODE;          //付款码 -> 离线码
    private String CUST_ID;          //用户ID
    @NotNull
    private String ORDER_ID;         //订单编号
    @NotNull
    private String OFFLINE;          //离线标识   0 , 1
    @NotNull
    private String SIGN_TIME;        //离线签单时间 yyyyMMddHHmmss

    @NotNull
    private String decryptionCode;   //解密码

    private String corDishId;       //关联菜品ID

    @Generated(hash = 1989026892)
    public OffLineTable(Long id, @NotNull String CAMPUS_ID, @NotNull String CORP_ID,
            @NotNull String TXCODE, @NotNull String ccbSafeParam,
            @NotNull String BUSINESS_ID, @NotNull String VPOS_ID,
            @NotNull String PAYMENT, @NotNull String ACTUAL_PAYMENT,
            String COUPON_INFO, String ACC_NOS, @NotNull String QR_CODE,
            String CUST_ID, @NotNull String ORDER_ID, @NotNull String OFFLINE,
            @NotNull String SIGN_TIME, @NotNull String decryptionCode,
            String corDishId) {
        this.id = id;
        this.CAMPUS_ID = CAMPUS_ID;
        this.CORP_ID = CORP_ID;
        this.TXCODE = TXCODE;
        this.ccbSafeParam = ccbSafeParam;
        this.BUSINESS_ID = BUSINESS_ID;
        this.VPOS_ID = VPOS_ID;
        this.PAYMENT = PAYMENT;
        this.ACTUAL_PAYMENT = ACTUAL_PAYMENT;
        this.COUPON_INFO = COUPON_INFO;
        this.ACC_NOS = ACC_NOS;
        this.QR_CODE = QR_CODE;
        this.CUST_ID = CUST_ID;
        this.ORDER_ID = ORDER_ID;
        this.OFFLINE = OFFLINE;
        this.SIGN_TIME = SIGN_TIME;
        this.decryptionCode = decryptionCode;
        this.corDishId = corDishId;
    }

    @Generated(hash = 609387495)
    public OffLineTable() {
    }

    public Long getId() {
        return this.id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCAMPUS_ID() {
        return this.CAMPUS_ID;
    }

    public void setCAMPUS_ID(String CAMPUS_ID) {
        this.CAMPUS_ID = CAMPUS_ID;
    }

    public String getCORP_ID() {
        return this.CORP_ID;
    }

    public void setCORP_ID(String CORP_ID) {
        this.CORP_ID = CORP_ID;
    }

    public String getTXCODE() {
        return this.TXCODE;
    }

    public void setTXCODE(String TXCODE) {
        this.TXCODE = TXCODE;
    }

    public String getCcbSafeParam() {
        return this.ccbSafeParam;
    }

    public void setCcbSafeParam(String ccbSafeParam) {
        this.ccbSafeParam = ccbSafeParam;
    }

    public String getBUSINESS_ID() {
        return this.BUSINESS_ID;
    }

    public void setBUSINESS_ID(String BUSINESS_ID) {
        this.BUSINESS_ID = BUSINESS_ID;
    }

    public String getVPOS_ID() {
        return this.VPOS_ID;
    }

    public void setVPOS_ID(String VPOS_ID) {
        this.VPOS_ID = VPOS_ID;
    }

    public String getPAYMENT() {
        return this.PAYMENT;
    }

    public void setPAYMENT(String PAYMENT) {
        this.PAYMENT = PAYMENT;
    }

    public String getACTUAL_PAYMENT() {
        return this.ACTUAL_PAYMENT;
    }

    public void setACTUAL_PAYMENT(String ACTUAL_PAYMENT) {
        this.ACTUAL_PAYMENT = ACTUAL_PAYMENT;
    }

    public String getCOUPON_INFO() {
        return this.COUPON_INFO;
    }

    public void setCOUPON_INFO(String COUPON_INFO) {
        this.COUPON_INFO = COUPON_INFO;
    }

    public String getACC_NOS() {
        return this.ACC_NOS;
    }

    public void setACC_NOS(String ACC_NOS) {
        this.ACC_NOS = ACC_NOS;
    }

    public String getQR_CODE() {
        return this.QR_CODE;
    }

    public void setQR_CODE(String QR_CODE) {
        this.QR_CODE = QR_CODE;
    }

    public String getCUST_ID() {
        return this.CUST_ID;
    }

    public void setCUST_ID(String CUST_ID) {
        this.CUST_ID = CUST_ID;
    }

    public String getORDER_ID() {
        return this.ORDER_ID;
    }

    public void setORDER_ID(String ORDER_ID) {
        this.ORDER_ID = ORDER_ID;
    }

    public String getOFFLINE() {
        return this.OFFLINE;
    }

    public void setOFFLINE(String OFFLINE) {
        this.OFFLINE = OFFLINE;
    }

    public String getSIGN_TIME() {
        return this.SIGN_TIME;
    }

    public void setSIGN_TIME(String SIGN_TIME) {
        this.SIGN_TIME = SIGN_TIME;
    }

    public String getDecryptionCode() {
        return this.decryptionCode;
    }

    public void setDecryptionCode(String decryptionCode) {
        this.decryptionCode = decryptionCode;
    }

    public String getCorDishId() {
        return this.corDishId;
    }

    public void setCorDishId(String corDishId) {
        this.corDishId = corDishId;
    }

}
