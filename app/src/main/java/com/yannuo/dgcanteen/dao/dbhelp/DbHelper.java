package com.yannuo.dgcanteen.dao.dbhelp;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import com.yannuo.dgcanteen.dao.DaoMaster;
import com.yannuo.dgcanteen.dao.DaoSession;


/**
 * @ClassName: DbHelper
 * @Description:
 * @Author: cgz
 * @CreateDate: 2020/10/16 9:41
 * @Version: 1.0
 */
public class DbHelper {
    private String TAG = "DbHelper";

    /**
     * Helper
     */
    private MySQLiteOpenHelper mHelper;//获取Helper对象
    /**
     * 数据库
     */
    private SQLiteDatabase db;
    /**
     * 数据库名称
     */
    private final String dbName = "pr.db";
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
    private Context context;


    private static DbHelper mDbHelper;



    /**
     * 获取单例
     */
    public static DbHelper getInstance(Context context){
        if(mDbHelper == null){
            synchronized (DbHelper.class){
                if(mDbHelper == null){
                    mDbHelper = new DbHelper(context);
                }
            }
        }
        return mDbHelper;
    }

    public static DbHelper getInstance(){
        if(mDbHelper == null){
            return null;
        }
        return mDbHelper;
    }

    /**
     * 初始化
     * @param context
     */
    public DbHelper(Context context) {
        this.context = context;
        mHelper = new MySQLiteOpenHelper(context, dbName, null);
        mDaoMaster =new DaoMaster(getWritableDatabase());
        mDaoSession = mDaoMaster.newSession();



    }
    /**
     * 获取可读数据库
     */
    private SQLiteDatabase getReadableDatabase(){
        if(mHelper == null){
            mHelper = new MySQLiteOpenHelper(context, dbName, null);
        }
        SQLiteDatabase db =mHelper.getReadableDatabase();
        return db;
    }

    /**
     * 获取可写数据库
     * @return
     */
    private SQLiteDatabase getWritableDatabase(){
        if(mHelper == null){
            mHelper = new MySQLiteOpenHelper(context, dbName, null);
        }
        SQLiteDatabase db = mHelper.getWritableDatabase();
        return db;
    }


    /****************************插入并替换***********************************************/


    /**
     * 插入人员记录
     * @param peopleInfo
     */
//    public void insertOrReplace(PeopleRecord peopleInfo){
//        long row = mPeopledao.insertOrReplace(mealDb);
//        LogUtil.i(TAG,"insertOrReplace row --> "+row);
//    }

    /**
     * 插入人脸特征
     * @param tokens
     */
//    public void insertFaceToken(FaceTokens tokens){
//        if (tokens == null)return;
////        FaceTokens bean = mFaceTokensDao.queryBuilder().where(FaceTokensDao.Properties.Number.eq(tokens.getNumber()))
////                .build()
////                .unique();
////        if (bean != null)tokens.setId(bean.getId());
//        mFaceTokensDao.insertOrReplace(tokens);
//    }


    /*************************************************插入*****************************************/


    /**
     * 插入商品列表
     * @param
     */
//    public void insertPeopleRecords(LinkedList<ProductsTable> peopleInfo){
//        mProductsTableDao.insertOrReplaceInTx(peopleInfo);
//    }

    /**
     * 插入单个商品
     * @param
     */
//    public void insertOrReplaceProduct(ProductsTable product){
//       long row = mProductsTableDao.insertOrReplace(product);
//       LogUtil.d(TAG,"商品插入到 "+row);
//    }


    /*************************************************更新数据*****************************************/


    /**
     * 更新通行记录上传标志
     * @param id
     */
//
//    public void updatePassThroughStatus(long id){
//        PassThrough unique = mPassThroughDao.queryBuilder()
//                .where(PassThroughDao.Properties.Id.eq(id))
//                .build()
//                .unique();
//        if (unique!= null){
//            unique.setUp(true);
//            mPassThroughDao.update(unique);
//        }
//    }


    /*************************************************条件查询数据*****************************************/


//    public ProductsTable queryProduct(String barCode){
//        ProductsTable product = mProductsTableDao.queryBuilder()
//                .where(ProductsTableDao.Properties.BarCode.eq(barCode))
//                .build()
//                .unique();
//        return product;
//
//    }



    /**
     * 分页查询未上传健康记录

     * @return
//     */
//    public List<HealthCodeRecord> searchHealthRecord(int page ,boolean up){
//
//
//        return mHealthCodeRecordDao.queryBuilder()
//                .where(HealthCodeRecordDao.Properties.Up.eq(up))
//                .offset(page * 100)
//                .limit(100)
//                .build()
//                .list();
//    }

    /**
     * 获取商品类型分组
     * @return
     */
//    public List<String> getGroup(){
//        String sgl = "SELECT "+ ProductsTableDao.Properties.Type.columnName+" FROM " +
//                mProductsTableDao.getTablename() + " GROUP BY "+ProductsTableDao.Properties.Type.columnName;
//        Cursor lCursor = getWritableDatabase().rawQuery(sgl, null);
//        List<String> groups = new ArrayList<>();
//        if (lCursor != null && lCursor.getCount() > 0) {
//            lCursor.moveToFirst();
//            do {
//               int index = lCursor.getColumnIndex(ProductsTableDao.Properties.Type.columnName);
//               String group = lCursor.getString(index);
//                groups.add(group);
//
//            }while (lCursor.moveToNext());
//        }
//      return groups;
//    }


    /**
     * 获取指定类型的商品
     * @param type
     * @return
     */
//    public List<ProductsTable> queryProductsByType(String type){
//
//      return  mProductsTableDao.queryBuilder()
//                .where(ProductsTableDao.Properties.Type.eq(type))
//                .build()
//                .list();
//    }


//    public List<ProductsTable> queryProducts(){
//        return  mProductsTableDao.queryBuilder()
//                .build()
//                .list();
//    }

    /*************************************************查询所有数据*****************************************/

    /**
     * 查询所有数据
     */
//    public List<PeopleRecord> searchAll(){
//        List<PeopleRecord> list = mPeopledao.queryBuilder().list();
//        LogUtil.i(TAG, "searchAll --> ");
//        return list;
//    }

    /**
     * 提取一条未更新人员消息
     */
//    public long getTotalPeopleInfoCount(){
//        return mPeopledao.count();
//
//
//    }

    /*************************************************删除数据*****************************************/


    /**
     * 根据行ID删除未上传通行记录
     * @param id
     */
//    public void deleteHealthRecord(long id){
//        mHealthCodeRecordDao.queryBuilder()
//                .where(HealthCodeRecordDao.Properties.Id.eq(id))
//                .buildDelete()
//                .executeDeleteWithoutDetachingEntities();
//    }

    /**
     * 清空所有商品数据
     */
//    public void deleteProduct(ProductsTable product){
//        mProductsTableDao.queryBuilder()
//         .where(ProductsTableDao.Properties.BarCode.eq(product.getBarCode()))
//        .buildDelete()
//
//        .executeDeleteWithoutDetachingEntities()
//        ;
//        LogUtil.i(TAG, "清空该商品数据...");
//    }


    /*************************************************清空所有数据*****************************************/

    /**
     * 清空所有商品数据
     */
//    public void deleteProductAll(){
//        mProductsTableDao.deleteAll();
//        LogUtil.i(TAG, "清空全部数据...");
//    }



    /**********************************************************/
    /***********************模糊查询人员信息*********************/
    /**********************************************************/

//    public List<EmpListDb> searchMan(String keyword){
//        List<EmpListDb> list = mEmpListDbDao.queryBuilder()
//                .whereOr(EmpListDbDao.Properties.EmpID.like('%' + keyword + '%'),
//                        EmpListDbDao.Properties.EmpName.like('%' + keyword + '%'))
//                .list();
//        return list;
//    }
}
