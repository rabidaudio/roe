package audio.rabid.dev.network_orm.testobjects;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import com.j256.ormlite.android.apptools.OrmLiteSqliteOpenHelper;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.TableUtils;

import java.sql.SQLException;

/**
 * Created by charles on 10/30/15.
 */
public class GenericDatabase extends OrmLiteSqliteOpenHelper {

    private static GenericDatabase instance;

    public static GenericDatabase getInstance() {
        if (instance == null)
            throw new RuntimeException("Need to create an instance with context first");
        return instance;
    }

    public static GenericDatabase getInstance(Context context) {
        if (instance == null) {
            instance = new GenericDatabase(context);
        }
        return instance;
    }

    public void clearDatabase() {
        try {
            TableUtils.clearTable(getConnectionSource(), DummyObject.class);
            TableUtils.clearTable(getConnectionSource(), DummyChild.class);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private GenericDatabase(Context context) {
        super(context, "simple.db", null, VERSION);
    }

    public static Dao getDaoOrThrow(Class clazz) {
        try {
            return getInstance().getDao(clazz);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void onCreate(SQLiteDatabase database, ConnectionSource connectionSource) {
        try {
            TableUtils.createTable(connectionSource, DummyObject.class);
            TableUtils.createTable(connectionSource, DummyChild.class);
        } catch (SQLException e) {
            throw new RuntimeException("Problem creating database", e);
        }
    }

    private static final int VERSION = 1;

    @Override
    public void onUpgrade(SQLiteDatabase database, ConnectionSource connectionSource, int oldVersion, int newVersion) {

    }
}
