package com.yannuo.dgcanteen.greendao.entity;

import org.greenrobot.greendao.annotation.Entity;
import org.greenrobot.greendao.annotation.Id;
import org.greenrobot.greendao.annotation.Generated;

@Entity
public class FacePayTable {
    @Id(autoincrement = true)
    private Long id;
    private String deviceId;    //设备序列号
    private String campusId;    //园区ID
    private String businessId;  //商家id
    private String businessName;    //商家名称
    private String vposId;  //柜台号
    private String payment; //支付金额
    private String actualPayment;   //真实支付金额
    private String offline; //离线订单标识：0：联机支付，1离线补扣
    private String signTime;    //离线订单签单时间
    private String personNumber;    //用户编号
    private String custId;
    private String orderId; //订单Id
    private String faceBase64;  //抓拍人脸base64编码
    private String faceScore;   //相似分数值
    @Generated(hash = 964699)
    public FacePayTable(Long id, String deviceId, String campusId,
            String businessId, String businessName, String vposId, String payment,
            String actualPayment, String offline, String signTime,
            String personNumber, String custId, String orderId, String faceBase64,
            String faceScore) {
        this.id = id;
        this.deviceId = deviceId;
        this.campusId = campusId;
        this.businessId = businessId;
        this.businessName = businessName;
        this.vposId = vposId;
        this.payment = payment;
        this.actualPayment = actualPayment;
        this.offline = offline;
        this.signTime = signTime;
        this.personNumber = personNumber;
        this.custId = custId;
        this.orderId = orderId;
        this.faceBase64 = faceBase64;
        this.faceScore = faceScore;
    }
    @Generated(hash = 225470125)
    public FacePayTable() {
    }
    public Long getId() {
        return this.id;
    }
    public void setId(Long id) {
        this.id = id;
    }
    public String getDeviceId() {
        return this.deviceId;
    }
    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }
    public String getCampusId() {
        return this.campusId;
    }
    public void setCampusId(String campusId) {
        this.campusId = campusId;
    }
    public String getBusinessId() {
        return this.businessId;
    }
    public void setBusinessId(String businessId) {
        this.businessId = businessId;
    }
    public String getBusinessName() {
        return this.businessName;
    }
    public void setBusinessName(String businessName) {
        this.businessName = businessName;
    }
    public String getVposId() {
        return this.vposId;
    }
    public void setVposId(String vposId) {
        this.vposId = vposId;
    }
    public String getPayment() {
        return this.payment;
    }
    public void setPayment(String payment) {
        this.payment = payment;
    }
    public String getActualPayment() {
        return this.actualPayment;
    }
    public void setActualPayment(String actualPayment) {
        this.actualPayment = actualPayment;
    }
    public String getOffline() {
        return this.offline;
    }
    public void setOffline(String offline) {
        this.offline = offline;
    }
    public String getSignTime() {
        return this.signTime;
    }
    public void setSignTime(String signTime) {
        this.signTime = signTime;
    }
    public String getPersonNumber() {
        return this.personNumber;
    }
    public void setPersonNumber(String personNumber) {
        this.personNumber = personNumber;
    }
    public String getCustId() {
        return this.custId;
    }
    public void setCustId(String custId) {
        this.custId = custId;
    }
    public String getFaceBase64() {
        return this.faceBase64;
    }
    public void setFaceBase64(String faceBase64) {
        this.faceBase64 = faceBase64;
    }
    public String getFaceScore() {
        return this.faceScore;
    }
    public void setFaceScore(String faceScore) {
        this.faceScore = faceScore;
    }
    public String getOrderId() {
        return this.orderId;
    }
    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

}
