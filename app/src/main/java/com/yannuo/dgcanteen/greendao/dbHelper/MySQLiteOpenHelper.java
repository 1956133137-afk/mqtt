package com.yannuo.dgcanteen.greendao.dbHelper;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import com.github.yuweiguocn.library.greendao.MigrationHelper;
import com.yannuo.dgcanteen.greendao.dao.DaoMaster;
import com.yannuo.dgcanteen.greendao.dao.DishesTableDao;
import com.yannuo.dgcanteen.greendao.dao.FaceRecordDao;
import com.yannuo.dgcanteen.greendao.dao.FaceTokensDao;
import com.yannuo.dgcanteen.greendao.dao.MealTableDao;
import com.yannuo.dgcanteen.greendao.dao.OfflineDishTableDao;
import com.yannuo.dgcanteen.greendao.dao.OfflineOrderTableDao;
import com.yannuo.dgcanteen.greendao.dao.PayDishTableDao;
import com.yannuo.dgcanteen.greendao.dao.PayOrderTableDao;
import com.yannuo.dgcanteen.greendao.dao.PersonsDao;
import com.yannuo.dgcanteen.greendao.dao.QuotaTimeTableDao;
import com.yannuo.dgcanteen.greendao.dao.SwPayOrderTableDao;
import com.yannuo.dgcanteen.greendao.dao.VerifyDishesDao;

import org.greenrobot.greendao.database.Database;

public class MySQLiteOpenHelper extends DaoMaster.OpenHelper {

    public MySQLiteOpenHelper(Context context, String name, SQLiteDatabase.CursorFactory factory) {
        super(context, name, factory);
    }

    @Override
    public void onUpgrade(Database db, int oldVersion, int newVersion) {
        MigrationHelper.migrate(db, new MigrationHelper.ReCreateAllTableListener() {

                    @Override
                    public void onCreateAllTables(Database db, boolean ifNotExists) {
                        DaoMaster.createAllTables(db, ifNotExists);
                    }

                    @Override
                    public void onDropAllTables(Database db, boolean ifExists) {
                        DaoMaster.dropAllTables(db, ifExists);
                    }
                },
                DishesTableDao.class,
                MealTableDao.class,
                PersonsDao.class,
                FaceTokensDao.class,
                FaceRecordDao.class,
                VerifyDishesDao.class,
                PayOrderTableDao.class,
                PayDishTableDao.class,
                OfflineOrderTableDao.class,
                OfflineDishTableDao.class,
                SwPayOrderTableDao.class,
                QuotaTimeTableDao.class
        );
    }
}

