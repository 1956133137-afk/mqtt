package com.yannuo.dgcanteen.greendao.entity;

import org.greenrobot.greendao.annotation.Entity;
import org.greenrobot.greendao.annotation.Id;
import org.greenrobot.greendao.annotation.ToOne;
import org.greenrobot.greendao.annotation.Generated;
import org.greenrobot.greendao.DaoException;
import com.yannuo.dgcanteen.greendao.dao.DaoSession;
import com.yannuo.dgcanteen.greendao.dao.PayOrderTableDao;
import com.yannuo.dgcanteen.greendao.dao.PayDishTableDao;

/**
 * Author: filowl
 * Description: ***
 * Date: 2024/8/21 9:42
 **/
@Entity
public class PayDishTable {
    @Id(autoincrement = true)
    private Long id;
    private Long payOrderId;        //外键
    private String dishesId;        //所关联的菜品Id
    private String dishesName;      //菜品名称
    private String dishesNumber;    //菜品数量
    private String dishesPrice;     //菜品单价

    @ToOne(joinProperty = "payOrderId")
    private PayOrderTable payOrderTable;
    /** Used to resolve relations */
    @Generated(hash = 2040040024)
    private transient DaoSession daoSession;
    /** Used for active entity operations. */
    @Generated(hash = 1351638681)
    private transient PayDishTableDao myDao;

    @Generated(hash = 1215396611)
    public PayDishTable(Long id, Long payOrderId, String dishesId,
            String dishesName, String dishesNumber, String dishesPrice) {
        this.id = id;
        this.payOrderId = payOrderId;
        this.dishesId = dishesId;
        this.dishesName = dishesName;
        this.dishesNumber = dishesNumber;
        this.dishesPrice = dishesPrice;
    }

    @Generated(hash = 2070056490)
    public PayDishTable() {
    }

    public Long getId() {
        return this.id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getPayOrderId() {
        return this.payOrderId;
    }

    public void setPayOrderId(Long payOrderId) {
        this.payOrderId = payOrderId;
    }

    public String getDishesId() {
        return this.dishesId;
    }

    public void setDishesId(String dishesId) {
        this.dishesId = dishesId;
    }

    public String getDishesName() {
        return this.dishesName;
    }

    public void setDishesName(String dishesName) {
        this.dishesName = dishesName;
    }

    public String getDishesNumber() {
        return this.dishesNumber;
    }

    public void setDishesNumber(String dishesNumber) {
        this.dishesNumber = dishesNumber;
    }

    public String getDishesPrice() {
        return this.dishesPrice;
    }

    public void setDishesPrice(String dishesPrice) {
        this.dishesPrice = dishesPrice;
    }

    @Generated(hash = 236130374)
    private transient Long payOrderTable__resolvedKey;

    /** To-one relationship, resolved on first access. */
    @Generated(hash = 1200316527)
    public PayOrderTable getPayOrderTable() {
        Long __key = this.payOrderId;
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
    @Generated(hash = 50243479)
    public void setPayOrderTable(PayOrderTable payOrderTable) {
        synchronized (this) {
            this.payOrderTable = payOrderTable;
            payOrderId = payOrderTable == null ? null : payOrderTable.getId();
            payOrderTable__resolvedKey = payOrderId;
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
    @Generated(hash = 1018616183)
    public void __setDaoSession(DaoSession daoSession) {
        this.daoSession = daoSession;
        myDao = daoSession != null ? daoSession.getPayDishTableDao() : null;
    }
}
