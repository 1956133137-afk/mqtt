package com.yannuo.dgcanteen.greendao.entity;

import org.greenrobot.greendao.annotation.Entity;
import org.greenrobot.greendao.annotation.Id;
import org.greenrobot.greendao.annotation.ToMany;
import org.greenrobot.greendao.annotation.Unique;

import java.util.List;

import org.greenrobot.greendao.annotation.Generated;
import org.greenrobot.greendao.DaoException;

import com.yannuo.dgcanteen.greendao.dao.DaoSession;
import com.yannuo.dgcanteen.greendao.dao.OfflineDishTableDao;
import com.yannuo.dgcanteen.greendao.dao.OfflineOrderTableDao;

/**
 * Author: filowl
 * Description: ***
 * Date: 2024/8/21 10:03
 **/
@Entity
public class OfflineOrderTable {
    @Id(autoincrement = true)
    private Long id;
    private String businessId;      //商家编号
    private String businessName;    //商家名称
    private String campusId;        //园区Id
    private String corpId;          //合作方Id
    private String vposId;          //柜台编号
    private String deviceId;        //设备序列号
    private String custId;          //智慧食堂用户唯一标识
    private String username;        //用户姓名
    private String payType;         //支付类型  1-刷脸 2-扫码 3-刷卡
    private String payContent;      //支付内容  payType: 1-刷脸 2-二维码 3-卡号
    private String payment;         //订单金额
    private String actualPayment;   //实际支付金额
    @Unique
    private String sessionId;       //订单唯一随机标记位
    private String payDate;         //支付日期
    private String signTime;        //交易时间 yyyyMMddHHmmss
    private String offline;         //离线标记 0在线 1离线
    private Integer flag = 0;       //上传标记 0未上传 1已上传

    @ToMany(referencedJoinProperty = "offlineOrderId")
    private List<OfflineDishTable> paymentDishes;
    /**
     * Used to resolve relations
     */
    @Generated(hash = 2040040024)
    private transient DaoSession daoSession;
    /**
     * Used for active entity operations.
     */
    @Generated(hash = 420413952)
    private transient OfflineOrderTableDao myDao;

    @Generated(hash = 1145552457)
    public OfflineOrderTable(Long id, String businessId, String businessName, String campusId,
            String corpId, String vposId, String deviceId, String custId, String username,
            String payType, String payContent, String payment, String actualPayment, String sessionId,
            String payDate, String signTime, String offline, Integer flag) {
        this.id = id;
        this.businessId = businessId;
        this.businessName = businessName;
        this.campusId = campusId;
        this.corpId = corpId;
        this.vposId = vposId;
        this.deviceId = deviceId;
        this.custId = custId;
        this.username = username;
        this.payType = payType;
        this.payContent = payContent;
        this.payment = payment;
        this.actualPayment = actualPayment;
        this.sessionId = sessionId;
        this.payDate = payDate;
        this.signTime = signTime;
        this.offline = offline;
        this.flag = flag;
    }

    @Generated(hash = 1486345693)
    public OfflineOrderTable() {
    }

    public Long getId() {
        return this.id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public String getSessionId() {
        return this.sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getSignTime() {
        return this.signTime;
    }

    public void setSignTime(String signTime) {
        this.signTime = signTime;
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
    @Generated(hash = 56822470)
    public List<OfflineDishTable> getPaymentDishes() {
        if (paymentDishes == null) {
            final DaoSession daoSession = this.daoSession;
            if (daoSession == null) {
                throw new DaoException("Entity is detached from DAO context");
            }
            OfflineDishTableDao targetDao = daoSession.getOfflineDishTableDao();
            List<OfflineDishTable> paymentDishesNew = targetDao
                    ._queryOfflineOrderTable_PaymentDishes(id);
            synchronized (this) {
                if (paymentDishes == null) {
                    paymentDishes = paymentDishesNew;
                }
            }
        }
        return paymentDishes;
    }

    /**
     * Resets a to-many relationship, making the next get call to query for a fresh result.
     */
    @Generated(hash = 975409320)
    public synchronized void resetPaymentDishes() {
        paymentDishes = null;
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

    /**
     * called by internal mechanisms, do not call yourself.
     */
    @Generated(hash = 484564419)
    public void __setDaoSession(DaoSession daoSession) {
        this.daoSession = daoSession;
        myDao = daoSession != null ? daoSession.getOfflineOrderTableDao() : null;
    }

    public String getPayDate() {
        return this.payDate;
    }

    public void setPayDate(String payDate) {
        this.payDate = payDate;
    }

}
