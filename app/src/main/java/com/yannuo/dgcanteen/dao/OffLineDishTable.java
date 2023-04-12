package com.yannuo.dgcanteen.dao;

import org.greenrobot.greendao.annotation.Entity;
import org.greenrobot.greendao.annotation.Id;
import org.greenrobot.greendao.annotation.ToOne;
import org.greenrobot.greendao.annotation.Generated;
import org.greenrobot.greendao.DaoException;

@Entity
public class OffLineDishTable {
    @Id(autoincrement = true)
    private Long id;

    private Long corDishId; //外键


    private String dishesId = null ;     //所关联的菜品Id
    private String dishesName = null ;     //菜品名称
    private int dishesNumber ;      //菜品数量
    private double dishesPrice ;     //菜品单价

    @ToOne(joinProperty = "corDishId")
    private OffLineTable corDish;

    /** Used to resolve relations */
    @Generated(hash = 2040040024)
    private transient DaoSession daoSession;

    /** Used for active entity operations. */
    @Generated(hash = 504879945)
    private transient OffLineDishTableDao myDao;

    @Generated(hash = 1801681635)
    public OffLineDishTable(Long id, Long corDishId, String dishesId,
            String dishesName, int dishesNumber, double dishesPrice) {
        this.id = id;
        this.corDishId = corDishId;
        this.dishesId = dishesId;
        this.dishesName = dishesName;
        this.dishesNumber = dishesNumber;
        this.dishesPrice = dishesPrice;
    }

    @Generated(hash = 431126531)
    public OffLineDishTable() {
    }

    public Long getId() {
        return this.id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getCorDishId() {
        return this.corDishId;
    }

    public void setCorDishId(Long corDishId) {
        this.corDishId = corDishId;
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

    public int getDishesNumber() {
        return this.dishesNumber;
    }

    public void setDishesNumber(int dishesNumber) {
        this.dishesNumber = dishesNumber;
    }

    public double getDishesPrice() {
        return this.dishesPrice;
    }

    public void setDishesPrice(double dishesPrice) {
        this.dishesPrice = dishesPrice;
    }

    @Generated(hash = 827979901)
    private transient Long corDish__resolvedKey;

    /** To-one relationship, resolved on first access. */
    @Generated(hash = 1966834348)
    public OffLineTable getCorDish() {
        Long __key = this.corDishId;
        if (corDish__resolvedKey == null || !corDish__resolvedKey.equals(__key)) {
            final DaoSession daoSession = this.daoSession;
            if (daoSession == null) {
                throw new DaoException("Entity is detached from DAO context");
            }
            OffLineTableDao targetDao = daoSession.getOffLineTableDao();
            OffLineTable corDishNew = targetDao.load(__key);
            synchronized (this) {
                corDish = corDishNew;
                corDish__resolvedKey = __key;
            }
        }
        return corDish;
    }

    /** called by internal mechanisms, do not call yourself. */
    @Generated(hash = 806745501)
    public void setCorDish(OffLineTable corDish) {
        synchronized (this) {
            this.corDish = corDish;
            corDishId = corDish == null ? null : corDish.getId();
            corDish__resolvedKey = corDishId;
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
    @Generated(hash = 1174986677)
    public void __setDaoSession(DaoSession daoSession) {
        this.daoSession = daoSession;
        myDao = daoSession != null ? daoSession.getOffLineDishTableDao() : null;
    }

}
