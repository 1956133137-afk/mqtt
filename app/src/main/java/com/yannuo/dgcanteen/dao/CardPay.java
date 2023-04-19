package com.yannuo.dgcanteen.dao;

import org.greenrobot.greendao.annotation.Entity;
import org.greenrobot.greendao.annotation.Id;
import org.greenrobot.greendao.annotation.Generated;
import org.greenrobot.greendao.annotation.ToMany;

import java.util.List;
import org.greenrobot.greendao.DaoException;

@Entity
public class CardPay {
    @Id(autoincrement = true)
    private Long id;
    private String campus_id=""; //园区ID
    private String corp_id=""; //
    private String txcode ="PAY005"; //交易码
    private String business_id="" ; //商家编号
    private String vpos_id="" ; //柜台编号
    private String payment="" ; //支付金额
    private String actual_payment="" ; //实际支付金额
    private String coupon_info =""; //优惠信息描述
    private String offline ="0"; //离线标识
    private String sign_time="" ; //离线签单时间
    private String acc_nos =""; //	用户可使用的账户
    private String card_id =""; //卡号
    private String cust_id =""; //	用户编号
    private String order_id =""; //	订单编号
    private String cipherUrl = ""; //加密请求串

    private boolean up = false;          //上传标识，用于上传失败跳过，以便上传下一条记录

    @ToMany(referencedJoinProperty = "orderid")
    private List<CardDishTable> cardDishesList;
    /** Used to resolve relations */
    @Generated(hash = 2040040024)
    private transient DaoSession daoSession;
    /** Used for active entity operations. */
    @Generated(hash = 1250569876)
    private transient CardPayDao myDao;

    @Generated(hash = 1086481032)
    public CardPay(Long id, String campus_id, String corp_id, String txcode,
            String business_id, String vpos_id, String payment,
            String actual_payment, String coupon_info, String offline,
            String sign_time, String acc_nos, String card_id, String cust_id,
            String order_id, String cipherUrl, boolean up) {
        this.id = id;
        this.campus_id = campus_id;
        this.corp_id = corp_id;
        this.txcode = txcode;
        this.business_id = business_id;
        this.vpos_id = vpos_id;
        this.payment = payment;
        this.actual_payment = actual_payment;
        this.coupon_info = coupon_info;
        this.offline = offline;
        this.sign_time = sign_time;
        this.acc_nos = acc_nos;
        this.card_id = card_id;
        this.cust_id = cust_id;
        this.order_id = order_id;
        this.cipherUrl = cipherUrl;
        this.up = up;
    }

    @Generated(hash = 1325644051)
    public CardPay() {
    }

    public Long getId() {
        return this.id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCampus_id() {
        return this.campus_id;
    }

    public void setCampus_id(String campus_id) {
        this.campus_id = campus_id;
    }

    public String getCorp_id() {
        return this.corp_id;
    }

    public void setCorp_id(String corp_id) {
        this.corp_id = corp_id;
    }

    public String getTxcode() {
        return this.txcode;
    }

    public void setTxcode(String txcode) {
        this.txcode = txcode;
    }

    public String getBusiness_id() {
        return this.business_id;
    }

    public void setBusiness_id(String business_id) {
        this.business_id = business_id;
    }

    public String getVpos_id() {
        return this.vpos_id;
    }

    public void setVpos_id(String vpos_id) {
        this.vpos_id = vpos_id;
    }

    public String getPayment() {
        return this.payment;
    }

    public void setPayment(String payment) {
        this.payment = payment;
    }

    public String getActual_payment() {
        return this.actual_payment;
    }

    public void setActual_payment(String actual_payment) {
        this.actual_payment = actual_payment;
    }

    public String getCoupon_info() {
        return this.coupon_info;
    }

    public void setCoupon_info(String coupon_info) {
        this.coupon_info = coupon_info;
    }

    public String getOffline() {
        return this.offline;
    }

    public void setOffline(String offline) {
        this.offline = offline;
    }

    public String getSign_time() {
        return this.sign_time;
    }

    public void setSign_time(String sign_time) {
        this.sign_time = sign_time;
    }

    public String getAcc_nos() {
        return this.acc_nos;
    }

    public void setAcc_nos(String acc_nos) {
        this.acc_nos = acc_nos;
    }

    public String getCard_id() {
        return this.card_id;
    }

    public void setCard_id(String card_id) {
        this.card_id = card_id;
    }

    public String getCust_id() {
        return this.cust_id;
    }

    public void setCust_id(String cust_id) {
        this.cust_id = cust_id;
    }

    public String getOrder_id() {
        return this.order_id;
    }

    public void setOrder_id(String order_id) {
        this.order_id = order_id;
    }

    public boolean getUp() {
        return this.up;
    }

    public void setUp(boolean up) {
        this.up = up;
    }

    public String getCipherUrl() {
        return this.cipherUrl;
    }

    public void setCipherUrl(String cipherUrl) {
        this.cipherUrl = cipherUrl;
    }

    /**
     * To-many relationship, resolved on first access (and after reset).
     * Changes to to-many relations are not persisted, make changes to the target entity.
     */
    @Generated(hash = 686855838)
    public List<CardDishTable> getCardDishesList() {
        if (cardDishesList == null) {
            final DaoSession daoSession = this.daoSession;
            if (daoSession == null) {
                throw new DaoException("Entity is detached from DAO context");
            }
            CardDishTableDao targetDao = daoSession.getCardDishTableDao();
            List<CardDishTable> cardDishesListNew = targetDao
                    ._queryCardPay_CardDishesList(id);
            synchronized (this) {
                if (cardDishesList == null) {
                    cardDishesList = cardDishesListNew;
                }
            }
        }
        return cardDishesList;
    }

    /** Resets a to-many relationship, making the next get call to query for a fresh result. */
    @Generated(hash = 209922827)
    public synchronized void resetCardDishesList() {
        cardDishesList = null;
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
    @Generated(hash = 1494325980)
    public void __setDaoSession(DaoSession daoSession) {
        this.daoSession = daoSession;
        myDao = daoSession != null ? daoSession.getCardPayDao() : null;
    }
}
