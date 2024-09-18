package com.yannuo.dgcanteen.greendao.entity;

import org.greenrobot.greendao.annotation.Entity;
import org.greenrobot.greendao.annotation.Id;
import org.greenrobot.greendao.annotation.ToMany;
import org.greenrobot.greendao.annotation.Unique;

import java.util.List;
import org.greenrobot.greendao.annotation.Generated;
import org.greenrobot.greendao.DaoException;
import com.yannuo.dgcanteen.greendao.dao.DaoSession;
import com.yannuo.dgcanteen.greendao.dao.PayDishTableDao;
import com.yannuo.dgcanteen.greendao.dao.PayOrderTableDao;
import com.yannuo.dgcanteen.greendao.dao.AccListTableDao;

/**
 * Author: filowl
 * Description: ***
 * Date: 2024/8/21 9:26
 **/
@Entity
public class PayOrderTable {
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

    @ToMany(referencedJoinProperty = "payOrderId")
    private List<PayDishTable> paymentDishesList;

    @ToMany(referencedJoinProperty = "accId")
    private List<AccListTable> accList;
    /** Used to resolve relations */
    @Generated(hash = 2040040024)
    private transient DaoSession daoSession;
    /** Used for active entity operations. */
    @Generated(hash = 808079322)
    private transient PayOrderTableDao myDao;

    @Generated(hash = 727829600)
    public PayOrderTable(Long id, String result, String tranResult, String businessId,
            String businessName, String campusId, String corpId, String vposId, String deviceId,
            String custId, String username, String accNo, String accBal, String accType, String orderId,
            String traceId, String payType, String payContent, String payment, String actualPayment,
            String payTime, String payDate, String offline, Integer flag) {
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
    }

    @Generated(hash = 1504739685)
    public PayOrderTable() {
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

    /**
     * To-many relationship, resolved on first access (and after reset).
     * Changes to to-many relations are not persisted, make changes to the target entity.
     */
    @Generated(hash = 649929711)
    public List<PayDishTable> getPaymentDishesList() {
        if (paymentDishesList == null) {
            final DaoSession daoSession = this.daoSession;
            if (daoSession == null) {
                throw new DaoException("Entity is detached from DAO context");
            }
            PayDishTableDao targetDao = daoSession.getPayDishTableDao();
            List<PayDishTable> paymentDishesListNew = targetDao
                    ._queryPayOrderTable_PaymentDishesList(id);
            synchronized (this) {
                if (paymentDishesList == null) {
                    paymentDishesList = paymentDishesListNew;
                }
            }
        }
        return paymentDishesList;
    }

    /** Resets a to-many relationship, making the next get call to query for a fresh result. */
    @Generated(hash = 25025082)
    public synchronized void resetPaymentDishesList() {
        paymentDishesList = null;
    }

    /**
     * To-many relationship, resolved on first access (and after reset).
     * Changes to to-many relations are not persisted, make changes to the target entity.
     */
    @Generated(hash = 656921021)
    public List<AccListTable> getAccList() {
        if (accList == null) {
            final DaoSession daoSession = this.daoSession;
            if (daoSession == null) {
                throw new DaoException("Entity is detached from DAO context");
            }
            AccListTableDao targetDao = daoSession.getAccListTableDao();
            List<AccListTable> accListNew = targetDao._queryPayOrderTable_AccList(id);
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
    @Generated(hash = 183950458)
    public void __setDaoSession(DaoSession daoSession) {
        this.daoSession = daoSession;
        myDao = daoSession != null ? daoSession.getPayOrderTableDao() : null;
    }
    
}
