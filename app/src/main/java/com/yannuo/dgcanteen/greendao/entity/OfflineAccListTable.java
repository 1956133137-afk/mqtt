package com.yannuo.dgcanteen.greendao.entity;

import org.greenrobot.greendao.annotation.Entity;
import org.greenrobot.greendao.annotation.Id;
import org.greenrobot.greendao.annotation.ToOne;
import org.greenrobot.greendao.annotation.Generated;
import org.greenrobot.greendao.DaoException;
import com.yannuo.dgcanteen.greendao.dao.DaoSession;
import com.yannuo.dgcanteen.greendao.dao.PayOrderTableDao;
import com.yannuo.dgcanteen.greendao.dao.OfflineAccListTableDao;
import com.yannuo.dgcanteen.greendao.dao.OfflineOrderTableDao;
@Entity
public class OfflineAccListTable {
    @Id(autoincrement = true)
    private Long id;
    private Long accId;        //外键
    private String ACC_BAL;    //账户余额
    private String ACC_NO;     //账户编号
    private String ACC_TYPE;   //账户类型
    private String PAYMENT;    //支付金额
    private String TRAN_ID;    //交易流水号
    @ToOne(joinProperty = "accId")
    private OfflineOrderTable offlineOrderTable;
    /** Used to resolve relations */
    @Generated(hash = 2040040024)
    private transient DaoSession daoSession;
    /** Used for active entity operations. */
    @Generated(hash = 858338849)
    private transient OfflineAccListTableDao myDao;
    @Generated(hash = 2100398950)
    public OfflineAccListTable(Long id, Long accId, String ACC_BAL, String ACC_NO,
            String ACC_TYPE, String PAYMENT, String TRAN_ID) {
        this.id = id;
        this.accId = accId;
        this.ACC_BAL = ACC_BAL;
        this.ACC_NO = ACC_NO;
        this.ACC_TYPE = ACC_TYPE;
        this.PAYMENT = PAYMENT;
        this.TRAN_ID = TRAN_ID;
    }
    @Generated(hash = 574317949)
    public OfflineAccListTable() {
    }
    public Long getId() {
        return this.id;
    }
    public void setId(Long id) {
        this.id = id;
    }
    public Long getAccId() {
        return this.accId;
    }
    public void setAccId(Long accId) {
        this.accId = accId;
    }
    public String getACC_BAL() {
        return this.ACC_BAL;
    }
    public void setACC_BAL(String ACC_BAL) {
        this.ACC_BAL = ACC_BAL;
    }
    public String getACC_NO() {
        return this.ACC_NO;
    }
    public void setACC_NO(String ACC_NO) {
        this.ACC_NO = ACC_NO;
    }
    public String getACC_TYPE() {
        return this.ACC_TYPE;
    }
    public void setACC_TYPE(String ACC_TYPE) {
        this.ACC_TYPE = ACC_TYPE;
    }
    public String getPAYMENT() {
        return this.PAYMENT;
    }
    public void setPAYMENT(String PAYMENT) {
        this.PAYMENT = PAYMENT;
    }
    public String getTRAN_ID() {
        return this.TRAN_ID;
    }
    public void setTRAN_ID(String TRAN_ID) {
        this.TRAN_ID = TRAN_ID;
    }
    @Generated(hash = 1775446895)
    private transient Long offlineOrderTable__resolvedKey;
    /** To-one relationship, resolved on first access. */
    @Generated(hash = 622992805)
    public OfflineOrderTable getOfflineOrderTable() {
        Long __key = this.accId;
        if (offlineOrderTable__resolvedKey == null
                || !offlineOrderTable__resolvedKey.equals(__key)) {
            final DaoSession daoSession = this.daoSession;
            if (daoSession == null) {
                throw new DaoException("Entity is detached from DAO context");
            }
            OfflineOrderTableDao targetDao = daoSession.getOfflineOrderTableDao();
            OfflineOrderTable offlineOrderTableNew = targetDao.load(__key);
            synchronized (this) {
                offlineOrderTable = offlineOrderTableNew;
                offlineOrderTable__resolvedKey = __key;
            }
        }
        return offlineOrderTable;
    }
    /** called by internal mechanisms, do not call yourself. */
    @Generated(hash = 2041350516)
    public void setOfflineOrderTable(OfflineOrderTable offlineOrderTable) {
        synchronized (this) {
            this.offlineOrderTable = offlineOrderTable;
            accId = offlineOrderTable == null ? null : offlineOrderTable.getId();
            offlineOrderTable__resolvedKey = accId;
        }
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
    @Generated(hash = 2127860559)
    public void __setDaoSession(DaoSession daoSession) {
        this.daoSession = daoSession;
        myDao = daoSession != null ? daoSession.getOfflineAccListTableDao() : null;
    }
}
