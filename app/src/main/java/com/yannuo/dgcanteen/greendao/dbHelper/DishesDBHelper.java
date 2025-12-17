package com.yannuo.dgcanteen.greendao.dbHelper;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.text.TextUtils;

import com.yannuo.dgcanteen.greendao.dao.AccListTableDao;
import com.yannuo.dgcanteen.greendao.dao.CategoryTableDao;
import com.yannuo.dgcanteen.greendao.dao.DaoMaster;
import com.yannuo.dgcanteen.greendao.dao.DaoSession;
import com.yannuo.dgcanteen.greendao.dao.DishesTableDao;
import com.yannuo.dgcanteen.greendao.dao.FacePayTableDao;
import com.yannuo.dgcanteen.greendao.dao.FaceRecordDao;
import com.yannuo.dgcanteen.greendao.dao.FaceTokensDao;
import com.yannuo.dgcanteen.greendao.dao.MealTableDao;
import com.yannuo.dgcanteen.greendao.dao.OfflineAccListTableDao;
import com.yannuo.dgcanteen.greendao.dao.OfflineDishTableDao;
import com.yannuo.dgcanteen.greendao.dao.OfflineOrderTableDao;
import com.yannuo.dgcanteen.greendao.dao.PayDishTableDao;
import com.yannuo.dgcanteen.greendao.dao.PayOrderTableDao;
import com.yannuo.dgcanteen.greendao.dao.PersonsDao;
import com.yannuo.dgcanteen.greendao.dao.QuotaTimeTableDao;
import com.yannuo.dgcanteen.greendao.dao.SwPayOrderTableDao;
import com.yannuo.dgcanteen.greendao.dao.UserFaceDataDao;
import com.yannuo.dgcanteen.greendao.dao.VerifyDishesDao;
import com.yannuo.dgcanteen.greendao.entity.AccListTable;
import com.yannuo.dgcanteen.greendao.entity.CategoryTable;
import com.yannuo.dgcanteen.greendao.entity.DishesTable;
import com.yannuo.dgcanteen.greendao.entity.FacePayTable;
import com.yannuo.dgcanteen.greendao.entity.FaceRecord;
import com.yannuo.dgcanteen.greendao.entity.FaceTokens;
import com.yannuo.dgcanteen.greendao.entity.MealTable;
import com.yannuo.dgcanteen.greendao.entity.OfflineAccListTable;
import com.yannuo.dgcanteen.greendao.entity.OfflineDishTable;
import com.yannuo.dgcanteen.greendao.entity.OfflineOrderTable;
import com.yannuo.dgcanteen.greendao.entity.PayDishTable;
import com.yannuo.dgcanteen.greendao.entity.PayOrderTable;
import com.yannuo.dgcanteen.greendao.entity.Persons;
import com.yannuo.dgcanteen.greendao.entity.QuotaTimeTable;
import com.yannuo.dgcanteen.greendao.entity.SwPayOrderTable;
import com.yannuo.dgcanteen.greendao.entity.UserFaceData;
import com.yannuo.dgcanteen.greendao.entity.VerifyDishes;
import com.yannuo.dgcanteen.util.LogUtil;

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
    private PersonsDao mPersonsDao;
    private FaceTokensDao mFaceTokensDao;
    private FaceRecordDao mFaceRecordDao;
    private VerifyDishesDao mVerifyDishesDao;
    private PayOrderTableDao payOrderTableDao;
    private PayDishTableDao payDishTableDao;
    private OfflineOrderTableDao olOrderTableDao;
    private OfflineDishTableDao olDishTableDao;
    private AccListTableDao accListDao;
    private OfflineAccListTableDao olAccListDao;
    private SwPayOrderTableDao swPayOrderTableDao;
    private QuotaTimeTableDao quotaTimeTableDao;
    private CategoryTableDao categoryTableDao;
    private UserFaceDataDao userFaceDataDao;
    private FacePayTableDao facePayTableDao;

    //获取实例
    public static DishesDBHelper getInstance(Context context) {
        if (mDBHelper == null) {
            synchronized (DishesDBHelper.class) {
                if (mDBHelper == null) {
                    mDBHelper = new DishesDBHelper(context);
                }
            }
        }
        return mDBHelper;
    }

    public static DishesDBHelper getInstance() {
        if (mDBHelper == null) {
            return null;
        }
        return mDBHelper;
    }

    public DaoSession getsession() {
        return mDaoSession;
    }

    /**
     * 初始化
     *
     * @param context
     */
    public DishesDBHelper(Context context) {
        this.mContext = context;
        mHelper = new MySQLiteOpenHelper(context, dbName, null);
        mDaoMaster = new DaoMaster(getWritableDatabase());
        mDaoSession = mDaoMaster.newSession();

        mDishesTableDao = mDaoSession.getDishesTableDao();
        mMealTableDao = mDaoSession.getMealTableDao();
        mPersonsDao = mDaoSession.getPersonsDao();
        mFaceTokensDao = mDaoSession.getFaceTokensDao();
        mFaceRecordDao = mDaoSession.getFaceRecordDao();
        mVerifyDishesDao = mDaoSession.getVerifyDishesDao();
        payOrderTableDao = mDaoSession.getPayOrderTableDao();
        payDishTableDao = mDaoSession.getPayDishTableDao();
        olOrderTableDao = mDaoSession.getOfflineOrderTableDao();
        olDishTableDao = mDaoSession.getOfflineDishTableDao();
        accListDao = mDaoSession.getAccListTableDao();
        olAccListDao = mDaoSession.getOfflineAccListTableDao();
        swPayOrderTableDao = mDaoSession.getSwPayOrderTableDao();
        quotaTimeTableDao = mDaoSession.getQuotaTimeTableDao();
        categoryTableDao = mDaoSession.getCategoryTableDao();
        userFaceDataDao = mDaoSession.getUserFaceDataDao();
        facePayTableDao = mDaoSession.getFacePayTableDao();
    }

    /**
     * 获取可读数据库
     */
    private SQLiteDatabase getReadableDatabase() {
        if (mHelper == null) {
            mHelper = new MySQLiteOpenHelper(mContext, dbName, null);
        }
        SQLiteDatabase db = mHelper.getReadableDatabase();
        return db;
    }

    /**
     * 获取可写数据库
     *
     * @return
     */
    private SQLiteDatabase getWritableDatabase() {
        if (mHelper == null) {
            mHelper = new MySQLiteOpenHelper(mContext, dbName, null);
        }
        SQLiteDatabase db = mHelper.getWritableDatabase();
        return db;
    }

    /**
     * 清空缓存
     */
    public void clearCache() {
        mDaoSession.clear();
    }

    /**
     * 获取指定餐别菜品
     *
     * @param mealId
     * @return
     */
    public List<DishesTable> queryDishesByMealId(int mealId) {

        return mDishesTableDao.queryBuilder()
                .where(DishesTableDao.Properties.MealId.eq(mealId))
                .build()
                .list();
    }

    public List<DishesTable> queryDishesByMealIdAneStatus(int MealId, int Status) {

        return mDishesTableDao.queryBuilder()
                .where(DishesTableDao.Properties.MealId.eq(MealId), DishesTableDao.Properties.Status.eq(Status))
                .orderAsc(DishesTableDao.Properties.Price)
                .build()
                .list();
    }

    public List<DishesTable> queryDishesByMealIdAneStatusDesc(int MealId, int Status) {

        return mDishesTableDao.queryBuilder()
                .where(DishesTableDao.Properties.MealId.eq(MealId), DishesTableDao.Properties.Status.eq(Status))
                .orderDesc(DishesTableDao.Properties.Price)
                .build()
                .list();
    }

    public List<DishesTable> queryDishesByMealIdAneStatusAnCategory(int MealId, int Status, String Category) {

        return mDishesTableDao.queryBuilder()
                .where(DishesTableDao.Properties.MealId.eq(MealId), DishesTableDao.Properties.Status.eq(Status), DishesTableDao.Properties.CategoryName.eq(Category))
                .orderAsc(DishesTableDao.Properties.Price)
                .build()
                .list();
    }

    /**
     * 获取全部菜品
     *
     * @return
     */
    public List<DishesTable> queryDishes() {
        return mDishesTableDao.queryBuilder()
                .build()
                .list();
    }

    /**
     * 更新数据
     *
     * @param
     */
    public void updateDishes(String dishId, int mealId, int status) {
        DishesTable dish = mDishesTableDao.queryBuilder()
                .where(DishesTableDao.Properties.DishesId.eq(dishId), DishesTableDao.Properties.MealId.eq(mealId))
                .limit(1)
                .build().unique();
        dish.setStatus(status);
        mDishesTableDao.update(dish);
    }

    /**
     * 插入菜品列表
     *
     * @param
     */
    public void insertDishes(List<DishesTable> dishes) {
        mDishesTableDao.insertOrReplaceInTx(dishes);
    }

    public void insertDishe(DishesTable dishes) {
        mDishesTableDao.insert(dishes);
    }

    public Long queryDishCount(String dishId,Integer mealId){
        return mDishesTableDao.queryBuilder()
                .where(DishesTableDao.Properties.DishesId.eq(dishId),DishesTableDao.Properties.MealId.eq(mealId))
                .count();
    }

    public List<DishesTable> queryDishById(String dishId) {
        return mDishesTableDao.queryBuilder()
                .where(DishesTableDao.Properties.DishesId.eq(dishId))
                .build().list();
    }

    /**
     * 插入全部餐别
     *
     * @param
     */
    public void insertMeals(List<MealTable> meals) {
        mMealTableDao.insertOrReplaceInTx(meals);
    }

    /**
     * 查询餐别
     *
     * @param
     */
    public MealTable queryToMeals(int meals) {
        return mMealTableDao.queryBuilder()
                .where(MealTableDao.Properties.MealId.eq(meals))
                .build()
                .unique();
    }

    public List<MealTable> queryAllMeals() {
        return mMealTableDao.queryBuilder()
                .orderAsc(MealTableDao.Properties.EndTime)
                .build()
                .list();
    }

    /**
     * 保存人员
     *
     * @param persons
     */
    public void insertPersons(List<Persons> persons) {
        mPersonsDao.insertOrReplaceInTx(persons);
    }

    /**
     * 总人数人员
     *
     * @param
     */
    public long getPersonsCount() {
        return mPersonsDao.count();
    }

    //删除人员信息表
    public void deleteAllPersons() {
        mPersonsDao.deleteAll();
    }

    /**
     * 添加一条待入库的记录
     *
     * @param record
     */
    public void insertWaitAddFace(FaceRecord record) {
        mFaceRecordDao.insertOrReplaceInTx(record);
    }

    /**
     * 清空待入库记录表
     */
    public void deleteAllWaitAddFace() {
        mFaceRecordDao.deleteAll();
    }

    /**
     * @param id 人员Id
     * @return
     */
    public Persons queryPersonToCustId(String id) {
        if (id == null) return null;
        return mPersonsDao.queryBuilder()
                .where(PersonsDao.Properties.CustId.eq(id))
                .build()
                .unique();
    }

    public List<Persons> queryPersonToName(String name, String phone) {
        return mPersonsDao.queryBuilder()
                .where(PersonsDao.Properties.PersonName.like("%"+name+"%"),PersonsDao.Properties.Phone.like("%"+phone+"%"))
                .build()
                .list();
    }

    /**
     * 筛选未更新的人员记录
     *
     * @return
     */
    public FaceRecord queryOnePerson() {
        return mFaceRecordDao.queryBuilder()
                .where(FaceRecordDao.Properties.Tryd.eq(false))
                .limit(1)
                .build()
                .unique();
    }


    public void updatePeopleInfo(FaceRecord info) {
        mFaceRecordDao.update(info);
        LogUtil.i(TAG, "FaceRecord update");
    }

    public void deleteFaceRecord(String custId) {
        mFaceRecordDao.queryBuilder()
                .where(FaceRecordDao.Properties.CustId.eq(custId))
                .buildDelete()
                .executeDeleteWithoutDetachingEntities();
    }

    /**
     * 分页查询图片添加记录
     *
     * @param page
     * @param up
     * @return
     */
    public List<FaceRecord> searchFaceRecords(int page, boolean up) {
        return mFaceRecordDao.queryBuilder()
                .where(FaceRecordDao.Properties.Tryd.in(up))
                .offset(page * 100)
                .limit(100)
                .build()
                .list();

    }

    /**
     * 分页查询人员
     *
     * @param page
     * @return
     */
    public List<Persons> searchPersons(int page) {
        return mPersonsDao.queryBuilder()
                .where(PersonsDao.Properties.Image.isNotNull())
                .offset(page * 100)
                .limit(100)
                .build()
                .list();
    }

    /**
     * 批量修改未入库图片记录
     *
     * @param peopleInfo
     */
    public void insertFaceRecords(List<FaceRecord> peopleInfo) {
        mFaceRecordDao.insertOrReplaceInTx(peopleInfo);
    }

    /**
     * @param id 卡号
     * @return
     */
    public Persons queryPersonToCardId(String id) {
        if (id == null) return null;
        return mPersonsDao.queryBuilder()
                .where(PersonsDao.Properties.CardId.eq(id))
                .build().unique();
    }

    /**
     * @param cidNo 学号
     * @return
     */
    public Persons queryPersonToCidNo(String cidNo) {
        if (cidNo == null) return null;
        return mPersonsDao.queryBuilder()
                .where(PersonsDao.Properties.PersonNumber.eq(cidNo))
                .build()
                .unique();
    }

    /**
     * cidNo
     *
     * @param personNumber
     * @return
     */
    public Persons queryPersonToNumber(String personNumber) {
        if (personNumber == null) return null;
        return mPersonsDao.queryBuilder()
                .where(PersonsDao.Properties.PersonNumber.eq(personNumber))
                .build()
                .unique();
    }

    /**
     * 保存人员
     *
     * @param cardId
     */
    public void deletePersons(String cardId) {
        if (cardId == null || cardId.isEmpty()) return;
        mPersonsDao.queryBuilder()
                .where(PersonsDao.Properties.CardId.eq(cardId))
                .buildDelete()
                .executeDeleteWithoutDetachingEntities();
//        LogUtil.d(TAG,"删除卡号人员"+cardId);
    }

    /**
     * 清空所有菜品
     */
    public void clearAllDishes() {
        mDishesTableDao.deleteAll();
    }

    /**
     * 清除餐别
     */
    public void clearAllMeal() {
        mMealTableDao.deleteAll();
    }


    //特征库相关

    /**
     * 通过人员ID查询人员特征信息
     *
     * @param number 人员ID
     * @return
     */
    public FaceTokens searchFaceToken(String number) {
        if (TextUtils.isEmpty(number)) return null;
        return mFaceTokensDao.queryBuilder()
                .where(FaceTokensDao.Properties.Number.eq(number))
                .build()
                .unique();
    }

    /**
     * 根据行number删除人脸token记录
     *
     * @param number
     */
    public void deleteFaceToken(String number) {
        if (TextUtils.isEmpty(number)) return;
        mFaceTokensDao.queryBuilder()
                .where(FaceTokensDao.Properties.Number.in(number))
                .buildDelete()
                .executeDeleteWithoutDetachingEntities();
    }

    /**
     * 插入人脸特征
     *
     * @param tokens
     */
    public void insertFaceToken(FaceTokens tokens) {
        if (tokens == null) return;
        mFaceTokensDao.insertOrReplace(tokens);
    }

    /**
     * 清空人员特征表
     */
    public void deleteAllFaceToken() {
        mFaceTokensDao.deleteAll();
    }

    /**
     * 插入核销菜品信息
     *
     * @param dishes
     */
    public void insertVerifyDishes(VerifyDishes dishes) {
        mVerifyDishesDao.insert(dishes);
    }

    /**
     * 删除指定Id核销菜品信息
     *
     * @param dishId
     */
    public void deleteVerifyDishes(String dishId) {
        mVerifyDishesDao.queryBuilder()
                .where(VerifyDishesDao.Properties.Id.eq(dishId))
                .buildDelete()
                .executeDeleteWithoutDetachingEntities();
    }

    /**
     * 清空所有核销菜品信息
     */
    public void delAllVerifyDishes() {
        mVerifyDishesDao.deleteAll();
    }

    /**
     * 查询100条核销菜品信息
     */
    public List<VerifyDishes> selectVerifyDishes() {
        return mVerifyDishesDao.queryBuilder()
                .limit(100)
                .build()
                .list();
    }

    public VerifyDishes queryVerifyUser(String date) {
        return mVerifyDishesDao.queryBuilder()
                .where(VerifyDishesDao.Properties.Time.gt(date + " 00:00:00"), VerifyDishesDao.Properties.Time.lt(date + " 23:59:59"))
                .orderDesc(VerifyDishesDao.Properties.Id)
                .limit(1)
                .build().unique();
    }

    public List<VerifyDishes> queryVerifyUserToTen(String date) {
        return mVerifyDishesDao.queryBuilder()
                .where(VerifyDishesDao.Properties.Time.gt(date + " 00:00:00"), VerifyDishesDao.Properties.Time.lt(date + " 23:59:59"))
                .orderDesc(VerifyDishesDao.Properties.Id)
                .limit(10)
                .build().list();
    }

    public List<VerifyDishes> queryVerifyUserToHundred(String date) {
        return mVerifyDishesDao.queryBuilder()
                .where(VerifyDishesDao.Properties.Time.gt(date + " 00:00:00"), VerifyDishesDao.Properties.Time.lt(date + " 23:59:59"))
                .orderDesc(VerifyDishesDao.Properties.Id)
                .limit(100)
                .build().list();
    }

    public List<VerifyDishes> queryVerifyUserToAll(String date, String username) {
        return mVerifyDishesDao.queryBuilder()
                .where(VerifyDishesDao.Properties.Time.gt(date + " 00:00:00"), VerifyDishesDao.Properties.Time.lt(date + " 23:59:59"))
                .where(VerifyDishesDao.Properties.PersonName.like("%" + username + "%"))
                .orderDesc(VerifyDishesDao.Properties.Id)
                .build().list();
    }

    public void deleteVerifyUser(String date) {
        mVerifyDishesDao.queryBuilder()
                .whereOr(VerifyDishesDao.Properties.Time.lt(date + " 00:00:00"), VerifyDishesDao.Properties.Time.gt(date + " 23:59:59"))
                .buildDelete()
                .executeDeleteWithoutDetachingEntities();

    }

    /*******************************  消费记录  *******************************/
    public void insertPayOrder(PayOrderTable payOrderTable) {
        payOrderTableDao.insertOrReplace(payOrderTable);
    }

    public PayOrderTable queryPayOrder(String orderId) {
        return payOrderTableDao.queryBuilder()
                .where(PayOrderTableDao.Properties.OrderId.eq(orderId))
                .build().unique();
    }

    public PayOrderTable queryPayOrderRecord(String custId, String payContent) {
        return payOrderTableDao.queryBuilder()
                .whereOr(PayOrderTableDao.Properties.CustId.like(custId), PayOrderTableDao.Properties.PayContent.like(payContent))
                .orderDesc(PayOrderTableDao.Properties.Id)
                .limit(1).build().unique();
    }

    public void updatePayOrder(PayOrderTable payOrder) {
        PayOrderTable order = queryPayOrder(payOrder.getOrderId());
        if (order != null) {
            order.setFlag(payOrder.getFlag());
            payOrderTableDao.update(order);
        }
    }

    public void updatePayOrderById(PayOrderTable payOrder) {
        payOrderTableDao.update(payOrder);
    }

    public List<PayOrderTable> queryPayOrder(String username, String orderId) {
        return payOrderTableDao.queryBuilder()
                .where(PayOrderTableDao.Properties.TranResult.eq("3"))
                .where(PayOrderTableDao.Properties.Username.like("%" + username + "%"))
                .where(PayOrderTableDao.Properties.OrderId.like("%" + orderId + "%"))
                .orderDesc(PayOrderTableDao.Properties.PayTime)
                .build().list();
    }

    public List<PayOrderTable> queryPayOrderToAll() {
        return payOrderTableDao.queryBuilder()
                .where(PayOrderTableDao.Properties.PayType.eq("1"))
                .where(PayOrderTableDao.Properties.Flag.eq(0))
                .build().list();
    }

    public PayOrderTable queryPayOrderUser(String date) {
        return payOrderTableDao.queryBuilder()
                .where(PayOrderTableDao.Properties.TranResult.eq("3"))
                .where(PayOrderTableDao.Properties.PayTime.gt(date + " 00:00:00"), PayOrderTableDao.Properties.PayTime.lt(date + " 23:59:59"))
                .orderDesc(PayOrderTableDao.Properties.Id)
                .limit(1)
                .build().unique();
    }

    public List<PayOrderTable> queryPayOrderUserToTen(String date) {
        return payOrderTableDao.queryBuilder()
                .where(PayOrderTableDao.Properties.TranResult.eq("3"))
                .where(PayOrderTableDao.Properties.PayTime.gt(date + " 00:00:00"), PayOrderTableDao.Properties.PayTime.lt(date + " 23:59:59"))
                .orderDesc(PayOrderTableDao.Properties.Id)
                .limit(10)
                .build().list();
    }

    public void deletePayOrder(PayOrderTable payOrder) {
        payOrderTableDao.delete(payOrder);
    }

    public void deletePayOrderByPayDate(String payDate) {
        List<PayOrderTable> orderList = payOrderTableDao.queryBuilder()
                .where(PayOrderTableDao.Properties.PayDate.notEq(payDate))
                .where(PayOrderTableDao.Properties.Flag.eq(1))
                .build().list();
        for (PayOrderTable order : orderList) {
            deletePayDish(order.getId());
            deletePayOrder(order);
        }
    }

    public void insertPayDish(PayDishTable payDishTable) {
        payDishTableDao.insert(payDishTable);
    }

    public void deletePayDish(long payOrderId) {
        payDishTableDao.queryBuilder()
                .where(PayDishTableDao.Properties.PayOrderId.eq(payOrderId))
                .buildDelete()
                .executeDeleteWithoutDetachingEntities();
    }

    /**
     * 插入所有账户信息
     *
     * @param accListTable
     */
    public void insertAccList(AccListTable accListTable) {
        accListDao.insert(accListTable);
    }

    public void queryAccList(String orderId) {
        payDishTableDao.queryBuilder()
                .where(PayDishTableDao.Properties.PayOrderId.eq(orderId))
                .build().list();
    }

    /*******************************  离线记录  *******************************/
    public void insertOfflineOrder(OfflineOrderTable offLineOrder) {
        olOrderTableDao.insertOrReplace(offLineOrder);
    }

    public void insertOfflineAccList(OfflineAccListTable accListTable) {
        olAccListDao.insertOrReplace(accListTable);
    }

    public OfflineOrderTable queryOfflineOrder(String sessionId) {
        return olOrderTableDao.queryBuilder()
                .where(OfflineOrderTableDao.Properties.SessionId.eq(sessionId))
                .build().unique();
    }

    public List<OfflineOrderTable> queryOfflineOrderByUsername(String username) {
        return olOrderTableDao.queryBuilder()
                .where(OfflineOrderTableDao.Properties.Flag.eq(0))
                .where(OfflineOrderTableDao.Properties.Username.like("%" + username + "%"))
                .orderDesc(OfflineOrderTableDao.Properties.SignTime)
                .build().list();
    }

    public List<OfflineOrderTable> queryOfflineOrderToAll() {
        return olOrderTableDao.queryBuilder()
                .where(OfflineOrderTableDao.Properties.Flag.notEq(1))
                .build().list();
    }

    public void updateOfflineOrder(OfflineOrderTable offlineOrder) {
        OfflineOrderTable order = queryOfflineOrder(offlineOrder.getSessionId());
        if (order != null) {
            order.setFlag(offlineOrder.getFlag());
            olOrderTableDao.update(order);
        }
    }

    public void deleteOfflineOrder(OfflineOrderTable offlineOrder) {
        olOrderTableDao.delete(offlineOrder);
    }

    public void deleteOfflineOrderByPayDate(String payDate) {
        List<OfflineOrderTable> orderList = olOrderTableDao.queryBuilder()
                .where(OfflineOrderTableDao.Properties.PayDate.notEq(payDate))
                .where(OfflineOrderTableDao.Properties.Flag.eq(1))
                .build().list();
        for (OfflineOrderTable order : orderList) {
            deleteOfflineDish(order.getId());
            deleteOfflineOrder(order);
        }
    }

    public void insertOfflineDish(OfflineDishTable offLineDish) {
        olDishTableDao.insert(offLineDish);
    }

    public void deleteOfflineDish(long offlineOrderId) {
        olDishTableDao.queryBuilder()
                .where(OfflineDishTableDao.Properties.OfflineOrderId.eq(offlineOrderId))
                .buildDelete()
                .executeDeleteWithoutDetachingEntities();
    }

    /*******************************  申万订单  *******************************/
    public void insertSwPayOrder(SwPayOrderTable swPayOrderTable) {
        LogUtil.d(TAG, "insertSwPayOrder：" + swPayOrderTable);
        long l = swPayOrderTableDao.insertOrReplace(swPayOrderTable);
        LogUtil.d(TAG, "insertSwPayOrder l: " + l);
    }

    public SwPayOrderTable querySwPayOrder(String orderId) {
        return swPayOrderTableDao.queryBuilder()
                .where(SwPayOrderTableDao.Properties.OrderId.eq(orderId))
                .build().unique();
    }

    public List<SwPayOrderTable> querySwPayOrderListByDate(String date, String actualMealName) {
        return swPayOrderTableDao.queryBuilder()
                .where(SwPayOrderTableDao.Properties.PayTime.gt(date + " 00:00:00"),
                        SwPayOrderTableDao.Properties.PayTime.lt(date + " 23:59:59"),
                        SwPayOrderTableDao.Properties.ActualMealName.eq(actualMealName))
                .build().list();
    }

    public List<SwPayOrderTable> querySwPayOrderListByCustIdDate(String custId, String date, String actualMealName) {
        return swPayOrderTableDao.queryBuilder()
                .where(SwPayOrderTableDao.Properties.CustId.eq(custId),
                        SwPayOrderTableDao.Properties.PayTime.gt(date + " 00:00:00"),
                        SwPayOrderTableDao.Properties.PayTime.lt(date + " 23:59:59"),
                        SwPayOrderTableDao.Properties.ActualMealName.eq(actualMealName))
                .build().list();
    }

    public List<SwPayOrderTable> querySwPayOrder(String username, String orderId, String date) {
        return swPayOrderTableDao.queryBuilder()
                .where(SwPayOrderTableDao.Properties.Username.like("%" + username + "%"),
                        SwPayOrderTableDao.Properties.OrderId.like("%" + orderId + "%"),
                        SwPayOrderTableDao.Properties.PayTime.gt(date + " 00:00:00"),
                        SwPayOrderTableDao.Properties.PayTime.lt(date + " 23:59:59"))
                .orderDesc(SwPayOrderTableDao.Properties.PayTime)
                .build().list();
    }

    public List<SwPayOrderTable> querySwPayOrderByStandardMealId(int standardMealId, String date) {
        return swPayOrderTableDao.queryBuilder()
                .where(SwPayOrderTableDao.Properties.PayTime.gt(date + " 00:00:00"),
                        SwPayOrderTableDao.Properties.PayTime.lt(date + " 23:59:59"),
                        SwPayOrderTableDao.Properties.StandardMealId.eq(standardMealId))
                .orderDesc(SwPayOrderTableDao.Properties.PayTime)
                .build().list();
    }

    public List<SwPayOrderTable> querySwPayOrderByActualMealId(int actualMealId, String date) {
        return swPayOrderTableDao.queryBuilder()
                .where(SwPayOrderTableDao.Properties.PayTime.gt(date + " 00:00:00"),
                        SwPayOrderTableDao.Properties.PayTime.lt(date + " 23:59:59"),
                        SwPayOrderTableDao.Properties.ActualMealId.eq(actualMealId))
                .orderDesc(SwPayOrderTableDao.Properties.PayTime)
                .build().list();
    }

    public void deleteSwPayOrder(SwPayOrderTable payOrder) {
        swPayOrderTableDao.delete(payOrder);
    }

    public void deleteSwPayOrderByPayDate(String payDate) {
        List<SwPayOrderTable> orderList = swPayOrderTableDao.queryBuilder()
                .where(SwPayOrderTableDao.Properties.PayDate.notEq(payDate))
                .where(SwPayOrderTableDao.Properties.Flag.eq(1))
                .build().list();
        for (SwPayOrderTable order : orderList) {
            deletePayDish(order.getId());
            deleteSwPayOrder(order);
        }
    }

    /*******************************   分时段定额   *******************************/
    public void insertQuotaTime(QuotaTimeTable quotaTime) {
        quotaTimeTableDao.insert(quotaTime);
    }

    public void updateQuotaTime(QuotaTimeTable quotaTime) {
        quotaTimeTableDao.update(quotaTime);
    }

    public List<QuotaTimeTable> queryQuotaTime() {
        return quotaTimeTableDao.queryBuilder().build().list();
    }

    public void deleteQuotaTime(QuotaTimeTable quotaTime) {
        quotaTimeTableDao.delete(quotaTime);
    }

    /*******************************   菜品类别   *******************************/
    public void insertCategory(List<CategoryTable> categoryTable) {
        categoryTableDao.insertOrReplaceInTx(categoryTable);
    }

    public List<CategoryTable> queryCategory() {
        return categoryTableDao.queryBuilder().build().list();
    }

    public List<CategoryTable> queryCategoryByMealId(int i) {
        return categoryTableDao.queryBuilder()
                .where(CategoryTableDao.Properties.MealId.eq(i))
                .build()
                .list();
    }

    public void clearAllCategory() {
        categoryTableDao.deleteAll();
    }

    /*******************************   人脸信息   *******************************/
    public void insertFaceData(UserFaceData user){
        userFaceDataDao.insertOrReplaceInTx(user);
    }

    public void insertFaceListData(List<UserFaceData> userList){
        userFaceDataDao.insertOrReplaceInTx(userList);
    }

    public void deleteAllFace(){
        userFaceDataDao.deleteAll();
    }

    public UserFaceData queryFaceByEigenvalue(String token){
        return userFaceDataDao.queryBuilder().where(UserFaceDataDao.Properties.Eigenvalue.eq(token)).build().unique();
    }

    public UserFaceData queryFaceByCustId(String custID){
        return userFaceDataDao.queryBuilder().where(UserFaceDataDao.Properties.CustId.eq(custID)).build().unique();
    }

    public void deleteFaceByCustId(String custId){
        userFaceDataDao.queryBuilder().where(UserFaceDataDao.Properties.CustId.eq(custId)).buildDelete().executeDeleteWithoutDetachingEntities();
    }

    public List<UserFaceData> queryFaceAll(){
        return userFaceDataDao.loadAll();
    }

    /*******************************   本地脸库离线订单   *******************************/

    public void insertFacePay(FacePayTable tabel){
        facePayTableDao.insertOrReplaceInTx(tabel);
    }

    public FacePayTable queryFacePayByOrderId(String orderId){
        return facePayTableDao.queryBuilder()
                .where(FacePayTableDao.Properties.OrderId.eq(orderId))
                .build().unique();
    }

}
