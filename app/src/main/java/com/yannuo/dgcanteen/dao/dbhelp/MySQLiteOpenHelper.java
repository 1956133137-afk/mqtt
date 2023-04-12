package com.yannuo.dgcanteen.dao.dbhelp;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import com.github.yuweiguocn.library.greendao.MigrationHelper;
import com.yannuo.dgcanteen.dao.DaoMaster;
import com.yannuo.dgcanteen.dao.DishesTableDao;
import com.yannuo.dgcanteen.dao.MealTableDao;
import com.yannuo.dgcanteen.dao.OffLineDishTableDao;
import com.yannuo.dgcanteen.dao.OffLineTableDao;
import com.yannuo.dgcanteen.dao.OrderDishListDao;
import com.yannuo.dgcanteen.dao.OwnOrderDao;

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
                OffLineTableDao.class,
                OffLineDishTableDao.class,
                OrderDishListDao.class,
                OwnOrderDao.class
        );
    }
}

