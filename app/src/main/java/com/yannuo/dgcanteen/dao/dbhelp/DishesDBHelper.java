package com.yannuo.dgcanteen.dao.dbhelp;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import com.yannuo.dgcanteen.dao.DaoMaster;
import com.yannuo.dgcanteen.dao.DaoSession;
import com.yannuo.dgcanteen.dao.DishesTable;
import com.yannuo.dgcanteen.dao.DishesTableDao;
import com.yannuo.dgcanteen.dao.ProductsTable;
import com.yannuo.dgcanteen.dao.ProductsTableDao;

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
    private final String dbName = "test.db";
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

    /**
     * 获取全部菜品
     * @return
     */
    public List<DishesTable> queryDishes(){
        return  mDishesTableDao.queryBuilder()
                .build()
                .list();
    }
}
