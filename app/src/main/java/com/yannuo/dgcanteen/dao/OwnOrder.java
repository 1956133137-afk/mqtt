package com.yannuo.dgcanteen.dao;

import org.greenrobot.greendao.annotation.Entity;
import org.greenrobot.greendao.annotation.Id;
import org.greenrobot.greendao.annotation.ToMany;
import org.greenrobot.greendao.annotation.Unique;

import java.util.List;
import org.greenrobot.greendao.annotation.Generated;
import org.greenrobot.greendao.DaoException;

@Entity
public class OwnOrder {

    @Id(autoincrement = true)
    private Long id;
    private String deviceSerialNumber = null; //设备序列号
    private  String businessId  = null ;           //商家Id
    private String counterId = null ;         //柜台号
    private String RESULT = null    ;         //订单结果: N：失败，Y：成功
    private String CUST_ID = null ;           //用户唯一标识
    private double PAYMENT = 0.0;            //原始金额
    private double ACTUAL_PAYMENT = 0.0;     //实际支付金额
    private String ACC_NO = null;             //支付账户
    private double ACC_BAL= 0.0;            //虚拟账户金额
    private int ACC_TYPE = 1;              //账户类型：01-现金账户，02-餐补账户1,03-餐补账户2，04-餐补账户3，05-餐补账户4，06-餐补账户5
    private String TRACEID =  null ;           //交易流水号

    @Unique
    private String ORDER_ID = null ;          //订单号
    private int TRAN_RESULT = 3   ;        //支付结果：1：待支付，2：支付失败，3：支付成功
    private int OFFLINE = 0;               //离线订单标识：0：联机支付，1离线补扣
    private String ERRCODE = null    ;        //错误码
    private String ERRMSG = null  ;           //错误信息
    private String ACCALIAS = null  ;         //账户类型名称
    private String PAYTIME = null    ;        //支付时间
    private String BUSINESS_NAME = null  ;    //商家名称

    private boolean up = false;          //上传标识，用于上传失败跳过，以便上传下一条记录

    @ToMany(referencedJoinProperty = "orderid")
    private List<OrderDishList> paymentDishesList;
    /** Used to resolve relations */
    @Generated(hash = 2040040024)
    private transient DaoSession daoSession;
    /** Used for active entity operations. */
    @Generated(hash = 431676984)
    private transient OwnOrderDao myDao;

    @Generated(hash = 2025299057)
    public OwnOrder(Long id, String deviceSerialNumber, String businessId, String counterId,
            String RESULT, String CUST_ID, double PAYMENT, double ACTUAL_PAYMENT, String ACC_NO,
            double ACC_BAL, int ACC_TYPE, String TRACEID, String ORDER_ID, int TRAN_RESULT, int OFFLINE,
            String ERRCODE, String ERRMSG, String ACCALIAS, String PAYTIME, String BUSINESS_NAME,
            boolean up) {
        this.id = id;
        this.deviceSerialNumber = deviceSerialNumber;
        this.businessId = businessId;
        this.counterId = counterId;
        this.RESULT = RESULT;
        this.CUST_ID = CUST_ID;
        this.PAYMENT = PAYMENT;
        this.ACTUAL_PAYMENT = ACTUAL_PAYMENT;
        this.ACC_NO = ACC_NO;
        this.ACC_BAL = ACC_BAL;
        this.ACC_TYPE = ACC_TYPE;
        this.TRACEID = TRACEID;
        this.ORDER_ID = ORDER_ID;
        this.TRAN_RESULT = TRAN_RESULT;
        this.OFFLINE = OFFLINE;
        this.ERRCODE = ERRCODE;
        this.ERRMSG = ERRMSG;
        this.ACCALIAS = ACCALIAS;
        this.PAYTIME = PAYTIME;
        this.BUSINESS_NAME = BUSINESS_NAME;
        this.up = up;
    }

    @Generated(hash = 1730371821)
    public OwnOrder() {
    }

    public Long getId() {
        return this.id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getDeviceSerialNumber() {
        return this.deviceSerialNumber;
    }

    public void setDeviceSerialNumber(String deviceSerialNumber) {
        this.deviceSerialNumber = deviceSerialNumber;
    }

    public String getBusinessId() {
        return this.businessId;
    }

    public void setBusinessId(String businessId) {
        this.businessId = businessId;
    }

    public String getCounterId() {
        return this.counterId;
    }

    public void setCounterId(String counterId) {
        this.counterId = counterId;
    }

    public String getRESULT() {
        return this.RESULT;
    }

    public void setRESULT(String RESULT) {
        this.RESULT = RESULT;
    }

    public String getCUST_ID() {
        return this.CUST_ID;
    }

    public void setCUST_ID(String CUST_ID) {
        this.CUST_ID = CUST_ID;
    }

    public double getPAYMENT() {
        return this.PAYMENT;
    }

    public void setPAYMENT(double PAYMENT) {
        this.PAYMENT = PAYMENT;
    }

    public double getACTUAL_PAYMENT() {
        return this.ACTUAL_PAYMENT;
    }

    public void setACTUAL_PAYMENT(double ACTUAL_PAYMENT) {
        this.ACTUAL_PAYMENT = ACTUAL_PAYMENT;
    }

    public String getACC_NO() {
        return this.ACC_NO;
    }

    public void setACC_NO(String ACC_NO) {
        this.ACC_NO = ACC_NO;
    }

    public double getACC_BAL() {
        return this.ACC_BAL;
    }

    public void setACC_BAL(double ACC_BAL) {
        this.ACC_BAL = ACC_BAL;
    }

    public int getACC_TYPE() {
        return this.ACC_TYPE;
    }

    public void setACC_TYPE(int ACC_TYPE) {
        this.ACC_TYPE = ACC_TYPE;
    }

    public String getTRACEID() {
        return this.TRACEID;
    }

    public void setTRACEID(String TRACEID) {
        this.TRACEID = TRACEID;
    }

    public String getORDER_ID() {
        return this.ORDER_ID;
    }

    public void setORDER_ID(String ORDER_ID) {
        this.ORDER_ID = ORDER_ID;
    }

    public int getTRAN_RESULT() {
        return this.TRAN_RESULT;
    }

    public void setTRAN_RESULT(int TRAN_RESULT) {
        this.TRAN_RESULT = TRAN_RESULT;
    }

    public int getOFFLINE() {
        return this.OFFLINE;
    }

    public void setOFFLINE(int OFFLINE) {
        this.OFFLINE = OFFLINE;
    }

    public String getERRCODE() {
        return this.ERRCODE;
    }

    public void setERRCODE(String ERRCODE) {
        this.ERRCODE = ERRCODE;
    }

    public String getERRMSG() {
        return this.ERRMSG;
    }

    public void setERRMSG(String ERRMSG) {
        this.ERRMSG = ERRMSG;
    }

    public String getACCALIAS() {
        return this.ACCALIAS;
    }

    public void setACCALIAS(String ACCALIAS) {
        this.ACCALIAS = ACCALIAS;
    }

    public String getPAYTIME() {
        return this.PAYTIME;
    }

    public void setPAYTIME(String PAYTIME) {
        this.PAYTIME = PAYTIME;
    }

    public String getBUSINESS_NAME() {
        return this.BUSINESS_NAME;
    }

    public void setBUSINESS_NAME(String BUSINESS_NAME) {
        this.BUSINESS_NAME = BUSINESS_NAME;
    }

    /**
     * To-many relationship, resolved on first access (and after reset).
     * Changes to to-many relations are not persisted, make changes to the target entity.
     */
    @Generated(hash = 896077264)
    public List<OrderDishList> getPaymentDishesList() {
        if (paymentDishesList == null) {
            final DaoSession daoSession = this.daoSession;
            if (daoSession == null) {
                throw new DaoException("Entity is detached from DAO context");
            }
            OrderDishListDao targetDao = daoSession.getOrderDishListDao();
            List<OrderDishList> paymentDishesListNew = targetDao._queryOwnOrder_PaymentDishesList(id);
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
    @Generated(hash = 1505353647)
    public void __setDaoSession(DaoSession daoSession) {
        this.daoSession = daoSession;
        myDao = daoSession != null ? daoSession.getOwnOrderDao() : null;
    }

    public boolean getUp() {
        return this.up;
    }

    public void setUp(boolean up) {
        this.up = up;
    }


}
