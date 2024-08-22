package com.yannuo.dgcanteen.greendao.entity;

import org.greenrobot.greendao.annotation.Entity;
import org.greenrobot.greendao.annotation.Id;
import org.greenrobot.greendao.annotation.ToOne;
import org.greenrobot.greendao.annotation.Generated;
import org.greenrobot.greendao.DaoException;
import com.yannuo.dgcanteen.greendao.dao.DaoSession;
import com.yannuo.dgcanteen.greendao.dao.OfflineOrderTableDao;
import com.yannuo.dgcanteen.greendao.dao.OfflineDishTableDao;

/**
 * Author: filowl
 * Description: ***
 * Date: 2024/8/21 10:11
 **/
@Entity
public class OfflineDishTable {
    @Id(autoincrement = true)
    private Long id;
    private Long offlineOrderId;    //外键
    private String dishesId;        //所关联的菜品Id
    private String dishesName;      //菜品名称
    private String dishesNumber;    //菜品数量
    private String dishesPrice;     //菜品单价

    @ToOne(joinProperty = "offlineOrderId")
    private OfflineOrderTable offlineOrderTable;
    /** Used to resolve relations */
    @Generated(hash = 2040040024)
    private transient DaoSession daoSession;
    /** Used for active entity operations. */
    @Generated(hash = 1425208848)
    private transient OfflineDishTableDao myDao;

    @Generated(hash = 1994648452)
    public OfflineDishTable(Long id, Long offlineOrderId, String dishesId,
            String dishesName, String dishesNumber, String dishesPrice) {
        this.id = id;
        this.offlineOrderId = offlineOrderId;
        this.dishesId = dishesId;
        this.dishesName = dishesName;
        this.dishesNumber = dishesNumber;
        this.dishesPrice = dishesPrice;
    }

    @Generated(hash = 1008073614)
    public OfflineDishTable() {
    }

    public Long getId() {
        return this.id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getOfflineOrderId() {
        return this.offlineOrderId;
    }

    public void setOfflineOrderId(Long offlineOrderId) {
        this.offlineOrderId = offlineOrderId;
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

    @Generated(hash = 1775446895)
    private transient Long offlineOrderTable__resolvedKey;

    /** To-one relationship, resolved on first access. */
    @Generated(hash = 1135532103)
    public OfflineOrderTable getOfflineOrderTable() {
        Long __key = this.offlineOrderId;
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
    @Generated(hash = 23788557)
    public void setOfflineOrderTable(OfflineOrderTable offlineOrderTable) {
        synchronized (this) {
            this.offlineOrderTable = offlineOrderTable;
            offlineOrderId = offlineOrderTable == null ? null
                    : offlineOrderTable.getId();
            offlineOrderTable__resolvedKey = offlineOrderId;
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
    @Generated(hash = 430453264)
    public void __setDaoSession(DaoSession daoSession) {
        this.daoSession = daoSession;
        myDao = daoSession != null ? daoSession.getOfflineDishTableDao() : null;
    }

}
