package com.yannuo.dgcanteen.greendao.entity;

import org.greenrobot.greendao.annotation.Entity;
import org.greenrobot.greendao.annotation.Id;
import org.greenrobot.greendao.annotation.ToOne;
import org.greenrobot.greendao.annotation.Generated;
import org.greenrobot.greendao.DaoException;
import com.yannuo.dgcanteen.greendao.dao.DaoSession;
import com.yannuo.dgcanteen.greendao.dao.SwPayOrderTableDao;
import com.yannuo.dgcanteen.greendao.dao.PayOrderTableDao;
import com.yannuo.dgcanteen.greendao.dao.AccListTableDao;

@Entity
public class AccListTable {
    @Id(autoincrement = true)
    private Long id;
    private Long accId;        //外键
    private String ACC_BAL;    //账户余额
    private String ACC_NO;     //账户编号
    private String ACC_TYPE;   //账户类型
    private String PAYMENT;    //支付金额
    private String TRAN_ID;    //交易流水号
    @ToOne(joinProperty = "accId")
    private PayOrderTable payOrderTable;

    @ToOne(joinProperty = "accId")
    private SwPayOrderTable swPayOrderTable;
    /** Used to resolve relations */
    @Generated(hash = 2040040024)
    private transient DaoSession daoSession;
    /** Used for active entity operations. */
    @Generated(hash = 2042922282)
    private transient AccListTableDao myDao;

    @Generated(hash = 975868530)
    public AccListTable(Long id, Long accId, String ACC_BAL, String ACC_NO,
            String ACC_TYPE, String PAYMENT, String TRAN_ID) {
        this.id = id;
        this.accId = accId;
        this.ACC_BAL = ACC_BAL;
        this.ACC_NO = ACC_NO;
        this.ACC_TYPE = ACC_TYPE;
        this.PAYMENT = PAYMENT;
        this.TRAN_ID = TRAN_ID;
    }

    @Generated(hash = 1540985685)
    public AccListTable() {
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

    @Generated(hash = 236130374)
    private transient Long payOrderTable__resolvedKey;

    /** To-one relationship, resolved on first access. */
    @Generated(hash = 1191416637)
    public PayOrderTable getPayOrderTable() {
        Long __key = this.accId;
        if (payOrderTable__resolvedKey == null
                || !payOrderTable__resolvedKey.equals(__key)) {
            final DaoSession daoSession = this.daoSession;
            if (daoSession == null) {
                throw new DaoException("Entity is detached from DAO context");
            }
            PayOrderTableDao targetDao = daoSession.getPayOrderTableDao();
            PayOrderTable payOrderTableNew = targetDao.load(__key);
            synchronized (this) {
                payOrderTable = payOrderTableNew;
                payOrderTable__resolvedKey = __key;
            }
        }
        return payOrderTable;
    }

    /** called by internal mechanisms, do not call yourself. */
    @Generated(hash = 64354715)
    public void setPayOrderTable(PayOrderTable payOrderTable) {
        synchronized (this) {
            this.payOrderTable = payOrderTable;
            accId = payOrderTable == null ? null : payOrderTable.getId();
            payOrderTable__resolvedKey = accId;
        }
    }

    @Generated(hash = 1797053847)
    private transient Long swPayOrderTable__resolvedKey;

    /** To-one relationship, resolved on first access. */
    @Generated(hash = 128915238)
    public SwPayOrderTable getSwPayOrderTable() {
        Long __key = this.accId;
        if (swPayOrderTable__resolvedKey == null
                || !swPayOrderTable__resolvedKey.equals(__key)) {
            final DaoSession daoSession = this.daoSession;
            if (daoSession == null) {
                throw new DaoException("Entity is detached from DAO context");
            }
            SwPayOrderTableDao targetDao = daoSession.getSwPayOrderTableDao();
            SwPayOrderTable swPayOrderTableNew = targetDao.load(__key);
            synchronized (this) {
                swPayOrderTable = swPayOrderTableNew;
                swPayOrderTable__resolvedKey = __key;
            }
        }
        return swPayOrderTable;
    }

    /** called by internal mechanisms, do not call yourself. */
    @Generated(hash = 155667337)
    public void setSwPayOrderTable(SwPayOrderTable swPayOrderTable) {
        synchronized (this) {
            this.swPayOrderTable = swPayOrderTable;
            accId = swPayOrderTable == null ? null : swPayOrderTable.getId();
            swPayOrderTable__resolvedKey = accId;
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
    @Generated(hash = 1574792428)
    public void __setDaoSession(DaoSession daoSession) {
        this.daoSession = daoSession;
        myDao = daoSession != null ? daoSession.getAccListTableDao() : null;
    }


}
