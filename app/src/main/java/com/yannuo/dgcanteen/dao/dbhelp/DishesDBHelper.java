package com.yannuo.dgcanteen.dao.dbhelp;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import com.yannuo.dgcanteen.dao.CardDishTable;
import com.yannuo.dgcanteen.dao.CardDishTableDao;
import com.yannuo.dgcanteen.dao.CardPay;
import com.yannuo.dgcanteen.dao.CardPayDao;
import com.yannuo.dgcanteen.dao.DaoMaster;
import com.yannuo.dgcanteen.dao.DaoSession;
import com.yannuo.dgcanteen.dao.DishesTable;
import com.yannuo.dgcanteen.dao.DishesTableDao;
import com.yannuo.dgcanteen.dao.MealTable;
import com.yannuo.dgcanteen.dao.MealTableDao;
import com.yannuo.dgcanteen.dao.OffLineDishTable;
import com.yannuo.dgcanteen.dao.OffLineDishTableDao;
import com.yannuo.dgcanteen.dao.OffLineTable;
import com.yannuo.dgcanteen.dao.OffLineTableDao;
import com.yannuo.dgcanteen.dao.OrderDishList;
import com.yannuo.dgcanteen.dao.OrderDishListDao;
import com.yannuo.dgcanteen.dao.OwnOrder;
import com.yannuo.dgcanteen.dao.OwnOrderDao;
import com.yannuo.dgcanteen.dao.Persons;
import com.yannuo.dgcanteen.dao.PersonsDao;

import java.util.List;

public class DishesDBHelper {
    private String TAG = "DishesDBHelper";
    /**
     * Helper
     */
    private MySQLiteOpenHelper mHelper;
    /**
     * 数据库
     */
    private SQLiteDatabase db;
    /**
     * 数据库名称
     */
    private final String dbName = "canteen.db";
    /**
     * DaoMaster
     */
    private DaoMaster mDaoMaster;
    /**
     * DaoSession
     */
    private DaoSession mDaoSession;
    /**
     * 上下文
     */
    private Context mContext;

    private static DishesDBHelper mDBHelper;
    private DishesTableDao mDishesTableDao;
    private MealTableDao mMealTableDao;
    private OwnOrderDao mOwnOrderDao;
    private OrderDishListDao mOrderDishListDao;
    private OffLineTableDao mOffLineTableDao;
    private OffLineDishTableDao mOffLineDishTableDao;
    private CardPayDao mCardPayDao;
    private CardDishTableDao mCardDishTableDao;
    private PersonsDao mPersonsDao;

    //获取实例
    public static DishesDBHelper getInstance(Context context){
        if (mDBHelper == null){
            synchronized (DishesDBHelper.class){
                if (mDBHelper == null){
                    mDBHelper = new DishesDBHelper(context);
                }
            }
        }
        return mDBHelper;
    }

    public static DishesDBHelper getInstance(){
        if (mDBHelper == null){
            return null;
        }
        return mDBHelper;
    }


    /**
     * 初始化
     * @param context
     */
    public DishesDBHelper(Context context){
        this.mContext = context;
        mHelper = new MySQLiteOpenHelper(context,dbName,null);
        mDaoMaster = new DaoMaster(getWritableDatabase());
        mDaoSession = mDaoMaster.newSession();

        mDishesTableDao = mDaoSession.getDishesTableDao();
        mMealTableDao = mDaoSession.getMealTableDao();
        mOwnOrderDao = mDaoSession.getOwnOrderDao();
        mOrderDishListDao = mDaoSession.getOrderDishListDao();
        mOffLineTableDao = mDaoSession.getOffLineTableDao();
        mOffLineDishTableDao = mDaoSession.getOffLineDishTableDao();
        mCardPayDao = mDaoSession.getCardPayDao();
        mCardDishTableDao = mDaoSession.getCardDishTableDao();
        mPersonsDao = mDaoSession.getPersonsDao();
    }

    /**
     * 获取可读数据库
     */
    private SQLiteDatabase getReadableDatabase(){
        if(mHelper == null){
            mHelper = new MySQLiteOpenHelper(mContext, dbName, null);
        }
        SQLiteDatabase db = mHelper.getReadableDatabase();
        return db;
    }

    /**
     * 获取可写数据库
     * @return
     */
    private SQLiteDatabase getWritableDatabase(){
        if(mHelper == null){
            mHelper = new MySQLiteOpenHelper(mContext, dbName, null);
        }
        SQLiteDatabase db = mHelper.getWritableDatabase();
        return db;
    }

    /**
     * 获取指定餐别菜品
     * @param mealId
     * @return
     */
    public List<DishesTable> queryDishesByMealId(int mealId){

        return  mDishesTableDao.queryBuilder()
                .where(DishesTableDao.Properties.MealId.eq(mealId))
                .build()
                .list();
    }

    public List<DishesTable> queryDishesByMealIdAneStatus(int MealId,int Status){

        return  mDishesTableDao.queryBuilder()
                .where(DishesTableDao.Properties.MealId.eq(MealId),DishesTableDao.Properties.Status.eq(Status))
                .build()
                .list();
    }

    /**
     * 获取全部菜品
     * @return
     */
    public List<DishesTable> queryDishes(){
        return  mDishesTableDao.queryBuilder()
                .build()
                .list();
    }

    /**
     * 更新数据
     * @param
     */
    public void updateDishes(String dishId, int mealId, int status){
        DishesTable dish = mDishesTableDao.queryBuilder()
                .where(DishesTableDao.Properties.DishesId.eq(dishId),DishesTableDao.Properties.MealId.eq(mealId))
                .build().unique();
        dish.setStatus(status);
        mDishesTableDao.update(dish);
    }

    /**
     * 插入菜品列表
     * @param
     */
    public void insertDishes(List<DishesTable> dishes){
        mDishesTableDao.insertInTx(dishes);
    }

    /**
     * 插入全部餐别
     * @param
     */
    public void insertMeals(List<MealTable> meals){
        mMealTableDao.insertInTx(meals);
    }

    /**
     * 查询餐别
     * @param
     */
    public MealTable queryToMeals(int meals){
        return mMealTableDao.queryBuilder()
                .where(MealTableDao.Properties.MealId.eq(meals))
                .build()
                .unique();
    }

    public List<MealTable> queryAllMeals(){
        return mMealTableDao.queryBuilder()
                .build()
                .list();
    }


    /**
     * 保存自有平台消费订单
     * @param order
     */
    public void insertConsumerOrder(OwnOrder order){
        mOwnOrderDao.insert(order);
    }

    /**
     * 保存人员
     * @param persons
     */
    public void insertPersons(List<Persons> persons){
        mPersonsDao.insertOrReplaceInTx(persons);
    }

    /**
     * @param id 人员Id
     * @return
     */
    public Persons queryPersonToCustId(String id){
        if (id == null)return null;
      return  mPersonsDao.queryBuilder()
                .where(PersonsDao.Properties.CustId.eq(id))
                .build()
                .unique();
    }

    /**
     * @param id 卡号
     * @return
     */
    public Persons queryPersonToCardId(String id){
        if (id == null)return null;
        return  mPersonsDao.queryBuilder()
                .where(PersonsDao.Properties.CardId.eq(id))
                .build()
                .unique();
    }

    /**
     * cidNo
     * @param personNumber
     * @return
     */
    public Persons queryPersonToNumber(String personNumber){
        if (personNumber == null)return null;
        return  mPersonsDao.queryBuilder()
                .where(PersonsDao.Properties.PersonNumber.eq(personNumber))
                .build()
                .unique();
    }
    /**
     * 保存自有平台消费订单中的消费菜品
     * @param dishes
     */
    public void insertConsumerDishes(List<OrderDishList> dishes){
        mOrderDishListDao.insertInTx(dishes);
    }

    /**
     * 保存离线补扣菜品
     * @param dishes
     */
    public void insertOffLineDishes(List<OffLineDishTable> dishes){
        mOffLineDishTableDao.insertInTx(dishes);
    }

    /**
     * 查询对应离线菜品
     * @param orderId
     */
    public List<OffLineDishTable> queryOffLineDish(long orderId){
        return mOffLineDishTableDao.queryBuilder()
                .where(OffLineDishTableDao.Properties.Orderid.eq(orderId))
                .build()
                .list();
    }

    /**
     * 插入一条离线订单
     * @param offLine
     */
    public void insertOffLineOrder(OffLineTable offLine){
        mOffLineTableDao.insert(offLine);
    }

    /**
     * 查询离线订单
     * @return
     */
    public OffLineTable queryOffLineOrder(){
        return  mOffLineTableDao.queryBuilder()
                .where(OffLineTableDao.Properties.PostTag.eq(0))
                .limit(1)
                .build()
                .unique();
    }

    /**
     * 修改消费记录
     * @return
     */
    public void updateOffLineOrder(OffLineTable offLine){
        mOffLineTableDao.update(offLine);
    }

    /**
     * 删除一条离线记录
     * @return
     */
    public void deleteOffLineOrder(String orderId){
        mOffLineTableDao.queryBuilder()
                .where(OffLineTableDao.Properties.ORDER_ID.eq(orderId))
                .buildDelete()
                .executeDeleteWithoutDetachingEntities();
    }


    /**
     * 保存刷卡离线补扣菜品
     * @param dishes
     */
    public void insertCardDishes(List<CardDishTable> dishes){
        mCardDishTableDao.insertInTx(dishes);
    }



    /**
     * 删除对应刷卡离线菜品
     * @param orderId
     */
    public void deleteCardDish(long orderId){
        mCardDishTableDao.queryBuilder()
                .where(CardDishTableDao.Properties.Orderid.eq(orderId))
                .buildDelete()
                .executeDeleteWithoutDetachingEntities();
    }

    /**
     * 插入一条刷卡离线订单
     * @param cardPay
     */
    public void insertCardOrder(CardPay cardPay){
        mCardPayDao.insert(cardPay);
    }

    /**
     * 查询刷卡离线订单
     * @return
     */
    public CardPay queryCardOrder(){
        return  mCardPayDao.queryBuilder()
                .where(CardPayDao.Properties.Up.eq(false))
                .limit(1)
                .build()
                .unique();
    }

    /**
     * 修改刷卡消费记录
     * @return
     */
    public void updateCardOrder(CardPay cardPay){
        mCardPayDao.update(cardPay);
    }

    /**
     * 删除一条刷卡离线记录
     * @return
     */
    public void deleteCardOrder(String orderId){
        mCardPayDao.queryBuilder()
                .where(CardPayDao.Properties.Order_id.eq(orderId))
                .buildDelete()
                .executeDeleteWithoutDetachingEntities();
    }


    /**
     * 获取一条未更新的记录
     * @return
     */
    public OwnOrder queryConsumerOrder(){
      return  mOwnOrderDao.queryBuilder()
                .where(OwnOrderDao.Properties.Up.eq(false))
                .limit(1)
                .build()
                .unique();
    }


    /**
     * 提取100条消费记录
     * @return
     */
    public List<OwnOrder> extractConsumerOrder(boolean up){
      return  mOwnOrderDao.queryBuilder()
                .where(OwnOrderDao.Properties.Up.eq(up))
                .limit(100)
                .build()
                .list();

    }

    /**
     * 修改多条条消费记录
     * @return
     */
    public void updateConsumerOrders(List<OwnOrder> ownOrders){
        if (ownOrders.size() <1)return;
          mOwnOrderDao.updateInTx(ownOrders);
    }
    /**
     * 修改消费记录
     * @return
     */
    public void updateConsumerOrder(OwnOrder ownOrder){
        mOwnOrderDao.update(ownOrder);
    }

    /**
     * 删除一条消费记录
     * @return
     */
    public void deleteConsumerOrder(String orderId){
          mOwnOrderDao.queryBuilder()
                .where(OwnOrderDao.Properties.ORDER_ID.eq(orderId))
                .buildDelete()
                .executeDeleteWithoutDetachingEntities();
    }

    /**
     * 删除对应消费记录的消费的菜品
     * @return
     */
    public void deleteRelatedDish(long orderId){
        mOrderDishListDao.queryBuilder()
                .where(OrderDishListDao.Properties.Orderid.eq(orderId))
                .buildDelete()
                .executeDeleteWithoutDetachingEntities();
    }

    /**
     * 删除对应离线菜品
     * @param orderId
     */
    public void deleteOffLineDish(long orderId){
        mOffLineDishTableDao.queryBuilder()
                .where(OffLineDishTableDao.Properties.Orderid.eq(orderId))
                .buildDelete()
                .executeDeleteWithoutDetachingEntities();
    }



    /**
     * 清空所有菜品
     */
    public void clearAllDishes(){
        mDishesTableDao.deleteAll();
    }

    /**
     * 清除餐别
     */
    public void clearAllMeal(){
        mMealTableDao.deleteAll();
    }


}
