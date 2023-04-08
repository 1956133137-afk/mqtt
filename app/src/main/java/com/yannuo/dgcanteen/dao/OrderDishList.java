package com.yannuo.dgcanteen.dao;

import org.greenrobot.greendao.annotation.Entity;
import org.greenrobot.greendao.annotation.Id;
import org.greenrobot.greendao.annotation.Generated;
import org.greenrobot.greendao.annotation.ToOne;
import org.greenrobot.greendao.DaoException;

@Entity
public class OrderDishList {
    @Id(autoincrement = true)
    private Long id;

    private Long orderid; //外键


    private String dishesId = null ;     //所关联的菜品Id
    private String dishesName = null ;     //菜品名称
    private int dishesNumber ;      //菜品数量
    private double dishesPrice ;     //菜品单价

    @ToOne(joinProperty = "orderid")
    private OwnOrder order;

    /** Used to resolve relations */
    @Generated(hash = 2040040024)
    private transient DaoSession daoSession;

    /** Used for active entity operations. */
    @Generated(hash = 843323393)
    private transient OrderDishListDao myDao;

    @Generated(hash = 219913283)
    private transient Long order__resolvedKey;

    @Generated(hash = 255114229)
    public OrderDishList(Long id, Long orderid, String dishesId, String dishesName,
            int dishesNumber, double dishesPrice) {
        this.id = id;
        this.orderid = orderid;
        this.dishesId = dishesId;
        this.dishesName = dishesName;
        this.dishesNumber = dishesNumber;
        this.dishesPrice = dishesPrice;
    }
    @Generated(hash = 234736215)
    public OrderDishList() {
    }
    public Long getId() {
        return this.id;
    }
    public void setId(Long id) {
        this.id = id;
    }
    public Long getOrderid() {
        return this.orderid;
    }
    public void setOrderid(Long orderid) {
        this.orderid = orderid;
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
    /** To-one relationship, resolved on first access. */
    @Generated(hash = 1736510927)
    public OwnOrder getOrder() {
        Long __key = this.orderid;
        if (order__resolvedKey == null || !order__resolvedKey.equals(__key)) {
            final DaoSession daoSession = this.daoSession;
            if (daoSession == null) {
                throw new DaoException("Entity is detached from DAO context");
            }
            OwnOrderDao targetDao = daoSession.getOwnOrderDao();
            OwnOrder orderNew = targetDao.load(__key);
            synchronized (this) {
                order = orderNew;
                order__resolvedKey = __key;
            }
        }
        return order;
    }
    /** called by internal mechanisms, do not call yourself. */
    @Generated(hash = 506136276)
    public void setOrder(OwnOrder order) {
        synchronized (this) {
            this.order = order;
            orderid = order == null ? null : order.getId();
            order__resolvedKey = orderid;
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
    @Generated(hash = 1961635969)
    public void __setDaoSession(DaoSession daoSession) {
        this.daoSession = daoSession;
        myDao = daoSession != null ? daoSession.getOrderDishListDao() : null;
    }

}
