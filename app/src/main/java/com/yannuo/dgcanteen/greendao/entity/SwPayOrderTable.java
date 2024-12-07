package com.yannuo.dgcanteen.greendao.entity;

import org.greenrobot.greendao.annotation.Entity;
import org.greenrobot.greendao.annotation.Id;
import org.greenrobot.greendao.annotation.ToMany;
import org.greenrobot.greendao.annotation.Unique;

import java.util.List;
import org.greenrobot.greendao.annotation.Generated;
import org.greenrobot.greendao.DaoException;
import com.yannuo.dgcanteen.greendao.dao.DaoSession;
import com.yannuo.dgcanteen.greendao.dao.SwPayOrderTableDao;
import com.yannuo.dgcanteen.greendao.dao.AccListTableDao;

/**
 * @dsc 简介
 * @Author LiWeiZhong
 * @Date 2024/11/28 14:37
 * @Version 1.0
 */
@Entity
public class SwPayOrderTable {

    @Id(autoincrement = true)
    private Long id;

    private String result;          //订单结果  Y-成功 N-失败
    private String tranResult;      //支付结果: 1：待支付，2：支付失败，3：支付成功
    private String businessId;      //商家编号
    private String businessName;    //商家名称
    private String campusId;        //园区Id
    private String corpId;          //合作方Id
    private String vposId;          //柜台编号
    private String deviceId;        //设备序列号
    private String custId;          //用户唯一标识
    private String username;        //用户姓名
    private String accNo;           //支付账号
    private String accBal;          //账户金额
    private String accType;         //账户类型: 01-现金账户，02-餐补账户1,03-餐补账户2，04-餐补账户3，05-餐补账户4，06-餐补账户5
    //    private String accList;         //账户类型名称
    @Unique
    private String orderId;         //订单号
    private String traceId;         //交易流水号
    private String payType;         //支付类型  1-刷脸 2-扫码 3-刷卡
    private String payContent;      //支付内容  payType: 1-刷脸 2-二维码 3-卡号
    private String payment;         //订单金额
    private String actualPayment;   //实际支付金额
    private String payTime;         //支付时间 yyyy-MM-dd HH:mm:ss
    private String payDate;         //支付日期
    private String offline;         //离线标记 0在线 1离线
    private Integer flag = 0;       //上传标记 0未上传 1已上传

    @ToMany(referencedJoinProperty = "accId")
    private List<AccListTable> accList;

    //sw
    private String actualMealName;      //实际支付餐别

    private int breakfastTime;    //早餐次数
    private int lunchTime;        //午餐次数
    private int dinnerTime;       //晚餐次数
    private int supperTime;       //夜宵次数

    private String standardMealName;      //使用餐标的餐别
    private String standardName; //使用的餐标名称
    private String standardNum;  //每次使用餐标耗次
    private int ruleRestTime; //剩余使用次数
    private float rulePrice;    //餐标单价
    private String subsidyMoney; //补贴

    /** Used to resolve relations */
    @Generated(hash = 2040040024)
    private transient DaoSession daoSession;

    /** Used for active entity operations. */
    @Generated(hash = 17827381)
    private transient SwPayOrderTableDao myDao;
    @Generated(hash = 2071516297)
    public SwPayOrderTable(Long id, String result, String tranResult, String businessId,
            String businessName, String campusId, String corpId, String vposId, String deviceId,
            String custId, String username, String accNo, String accBal, String accType, String orderId,
            String traceId, String payType, String payContent, String payment, String actualPayment,
            String payTime, String payDate, String offline, Integer flag, String actualMealName,
            int breakfastTime, int lunchTime, int dinnerTime, int supperTime, String standardMealName,
            String standardName, String standardNum, int ruleRestTime, float rulePrice,
            String subsidyMoney) {
        this.id = id;
        this.result = result;
        this.tranResult = tranResult;
        this.businessId = businessId;
        this.businessName = businessName;
        this.campusId = campusId;
        this.corpId = corpId;
        this.vposId = vposId;
        this.deviceId = deviceId;
        this.custId = custId;
        this.username = username;
        this.accNo = accNo;
        this.accBal = accBal;
        this.accType = accType;
        this.orderId = orderId;
        this.traceId = traceId;
        this.payType = payType;
        this.payContent = payContent;
        this.payment = payment;
        this.actualPayment = actualPayment;
        this.payTime = payTime;
        this.payDate = payDate;
        this.offline = offline;
        this.flag = flag;
        this.actualMealName = actualMealName;
        this.breakfastTime = breakfastTime;
        this.lunchTime = lunchTime;
        this.dinnerTime = dinnerTime;
        this.supperTime = supperTime;
        this.standardMealName = standardMealName;
        this.standardName = standardName;
        this.standardNum = standardNum;
        this.ruleRestTime = ruleRestTime;
        this.rulePrice = rulePrice;
        this.subsidyMoney = subsidyMoney;
    }
    @Generated(hash = 598791769)
    public SwPayOrderTable() {
    }
    public Long getId() {
        return this.id;
    }
    public void setId(Long id) {
        this.id = id;
    }
    public String getResult() {
        return this.result;
    }
    public void setResult(String result) {
        this.result = result;
    }
    public String getTranResult() {
        return this.tranResult;
    }
    public void setTranResult(String tranResult) {
        this.tranResult = tranResult;
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
    public String getCampusId() {
        return this.campusId;
    }
    public void setCampusId(String campusId) {
        this.campusId = campusId;
    }
    public String getCorpId() {
        return this.corpId;
    }
    public void setCorpId(String corpId) {
        this.corpId = corpId;
    }
    public String getVposId() {
        return this.vposId;
    }
    public void setVposId(String vposId) {
        this.vposId = vposId;
    }
    public String getDeviceId() {
        return this.deviceId;
    }
    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }
    public String getCustId() {
        return this.custId;
    }
    public void setCustId(String custId) {
        this.custId = custId;
    }
    public String getUsername() {
        return this.username;
    }
    public void setUsername(String username) {
        this.username = username;
    }
    public String getAccNo() {
        return this.accNo;
    }
    public void setAccNo(String accNo) {
        this.accNo = accNo;
    }
    public String getAccBal() {
        return this.accBal;
    }
    public void setAccBal(String accBal) {
        this.accBal = accBal;
    }
    public String getAccType() {
        return this.accType;
    }
    public void setAccType(String accType) {
        this.accType = accType;
    }
    public String getOrderId() {
        return this.orderId;
    }
    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }
    public String getTraceId() {
        return this.traceId;
    }
    public void setTraceId(String traceId) {
        this.traceId = traceId;
    }
    public String getPayType() {
        return this.payType;
    }
    public void setPayType(String payType) {
        this.payType = payType;
    }
    public String getPayContent() {
        return this.payContent;
    }
    public void setPayContent(String payContent) {
        this.payContent = payContent;
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
    public String getPayTime() {
        return this.payTime;
    }
    public void setPayTime(String payTime) {
        this.payTime = payTime;
    }
    public String getPayDate() {
        return this.payDate;
    }
    public void setPayDate(String payDate) {
        this.payDate = payDate;
    }
    public String getOffline() {
        return this.offline;
    }
    public void setOffline(String offline) {
        this.offline = offline;
    }
    public Integer getFlag() {
        return this.flag;
    }
    public void setFlag(Integer flag) {
        this.flag = flag;
    }
    public String getActualMealName() {
        return this.actualMealName;
    }
    public void setActualMealName(String actualMealName) {
        this.actualMealName = actualMealName;
    }
    public int getBreakfastTime() {
        return this.breakfastTime;
    }
    public void setBreakfastTime(int breakfastTime) {
        this.breakfastTime = breakfastTime;
    }
    public int getLunchTime() {
        return this.lunchTime;
    }
    public void setLunchTime(int lunchTime) {
        this.lunchTime = lunchTime;
    }
    public int getDinnerTime() {
        return this.dinnerTime;
    }
    public void setDinnerTime(int dinnerTime) {
        this.dinnerTime = dinnerTime;
    }
    public int getSupperTime() {
        return this.supperTime;
    }
    public void setSupperTime(int supperTime) {
        this.supperTime = supperTime;
    }
    public String getStandardMealName() {
        return this.standardMealName;
    }
    public void setStandardMealName(String standardMealName) {
        this.standardMealName = standardMealName;
    }
    public String getStandardName() {
        return this.standardName;
    }
    public void setStandardName(String standardName) {
        this.standardName = standardName;
    }
    public String getStandardNum() {
        return this.standardNum;
    }
    public void setStandardNum(String standardNum) {
        this.standardNum = standardNum;
    }
    public int getRuleRestTime() {
        return this.ruleRestTime;
    }
    public void setRuleRestTime(int ruleRestTime) {
        this.ruleRestTime = ruleRestTime;
    }
    public float getRulePrice() {
        return this.rulePrice;
    }
    public void setRulePrice(float rulePrice) {
        this.rulePrice = rulePrice;
    }
    public String getSubsidyMoney() {
        return this.subsidyMoney;
    }
    public void setSubsidyMoney(String subsidyMoney) {
        this.subsidyMoney = subsidyMoney;
    }
    /**
     * To-many relationship, resolved on first access (and after reset).
     * Changes to to-many relations are not persisted, make changes to the target entity.
     */
    @Generated(hash = 1272759050)
    public List<AccListTable> getAccList() {
        if (accList == null) {
            final DaoSession daoSession = this.daoSession;
            if (daoSession == null) {
                throw new DaoException("Entity is detached from DAO context");
            }
            AccListTableDao targetDao = daoSession.getAccListTableDao();
            List<AccListTable> accListNew = targetDao._querySwPayOrderTable_AccList(id);
            synchronized (this) {
                if (accList == null) {
                    accList = accListNew;
                }
            }
        }
        return accList;
    }
    /** Resets a to-many relationship, making the next get call to query for a fresh result. */
    @Generated(hash = 1306375034)
    public synchronized void resetAccList() {
        accList = null;
    }
    /**
     * Convenient call for {@link org.greenrobot.greendao.AbstractDao#delete(Object)}.
     * Entity must attached to an entity context.
     */
    @Generated(hash = 128553479)
    public void delete() {
        if (myDao == null) {
            throw new DaoException("Entity is detached from DAO context");
        }
        myDao.delete(this);
    }
    /**
     * Convenient call for {@link org.greenrobot.greendao.AbstractDao#refresh(Object)}.
     * Entity must attached to an entity context.
     */
    @Generated(hash = 1942392019)
    public void refresh() {
        if (myDao == null) {
            throw new DaoException("Entity is detached from DAO context");
        }
        myDao.refresh(this);
    }
    /**
     * Convenient call for {@link org.greenrobot.greendao.AbstractDao#update(Object)}.
     * Entity must attached to an entity context.
     */
    @Generated(hash = 713229351)
    public void update() {
        if (myDao == null) {
            throw new DaoException("Entity is detached from DAO context");
        }
        myDao.update(this);
    }
    /** called by internal mechanisms, do not call yourself. */
    @Generated(hash = 548011606)
    public void __setDaoSession(DaoSession daoSession) {
        this.daoSession = daoSession;
        myDao = daoSession != null ? daoSession.getSwPayOrderTableDao() : null;
    }

    @Override
    public String toString() {
        return "SwPayOrderTable{" +
                "id=" + id +
                ", result='" + result + '\'' +
                ", tranResult='" + tranResult + '\'' +
                ", businessId='" + businessId + '\'' +
                ", businessName='" + businessName + '\'' +
                ", campusId='" + campusId + '\'' +
                ", corpId='" + corpId + '\'' +
                ", vposId='" + vposId + '\'' +
                ", deviceId='" + deviceId + '\'' +
                ", custId='" + custId + '\'' +
                ", username='" + username + '\'' +
                ", accNo='" + accNo + '\'' +
                ", accBal='" + accBal + '\'' +
                ", accType='" + accType + '\'' +
                ", orderId='" + orderId + '\'' +
                ", traceId='" + traceId + '\'' +
                ", payType='" + payType + '\'' +
                ", payContent='" + payContent + '\'' +
                ", payment='" + payment + '\'' +
                ", actualPayment='" + actualPayment + '\'' +
                ", payTime='" + payTime + '\'' +
                ", payDate='" + payDate + '\'' +
                ", offline='" + offline + '\'' +
                ", flag=" + flag +
                ", accList=" + accList +
                ", actualMealName='" + actualMealName + '\'' +
                ", breakfastTime=" + breakfastTime +
                ", lunchTime=" + lunchTime +
                ", dinnerTime=" + dinnerTime +
                ", supperTime=" + supperTime +
                ", standardMealName='" + standardMealName + '\'' +
                ", standardName='" + standardName + '\'' +
                ", standardNum='" + standardNum + '\'' +
                ", ruleRestTime=" + ruleRestTime +
                ", rulePrice=" + rulePrice +
                ", subsidyMoney='" + subsidyMoney + '\'' +
                ", daoSession=" + daoSession +
                ", myDao=" + myDao +
                '}';
    }
}
