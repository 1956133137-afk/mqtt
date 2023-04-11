package com.yannuo.dgcanteen.dao.dbhelp;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import com.yannuo.dgcanteen.dao.DaoMaster;
import com.yannuo.dgcanteen.dao.DaoSession;
import com.yannuo.dgcanteen.dao.DishesTable;
import com.yannuo.dgcanteen.dao.DishesTableDao;
import com.yannuo.dgcanteen.dao.MealTable;
import com.yannuo.dgcanteen.dao.MealTableDao;
import com.yannuo.dgcanteen.dao.OrderDishList;
import com.yannuo.dgcanteen.dao.OrderDishListDao;
import com.yannuo.dgcanteen.dao.OwnOrder;
import com.yannuo.dgcanteen.dao.OwnOrderDao;

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

    public List<DishesTable> queryDishesByStatus(int Status){

        return  mDishesTableDao.queryBuilder()
                .where(DishesTableDao.Properties.Status.eq(Status))
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
    public void updateDishes(String id,int status){
        DishesTable dishes = mDishesTableDao.queryBuilder().where(DishesTableDao.Properties.DishesId.eq(id)).build().unique();
        dishes.setStatus(status);
        mDishesTableDao.update(dishes);
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
     * 保存自有平台消费订单
     * @param order
     */
    public void insertConsumerOrder(OwnOrder order){
        mOwnOrderDao.insert(order);
    }


    /**
     * 保存自有平台消费订单中的消费菜品
     * @param dishes
     */
    public void insertConsumerDishes(List<OrderDishList> dishes){
        mOrderDishListDao.insertInTx(dishes);
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
