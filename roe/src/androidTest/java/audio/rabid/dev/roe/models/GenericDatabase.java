package audio.rabid.dev.roe.models;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.TableUtils;

import java.sql.SQLException;

/**
 * Created by charles on 11/11/15.
 */
public class GenericDatabase extends Database {

    private static GenericDatabase instance;

    public static void createInstance(Context context) {
        if (instance == null) {
            instance = new GenericDatabase(context);
        }
    }

    public static GenericDatabase getInstance() {
        if (instance == null) {
            throw new IllegalArgumentException("Need to initialize with Context first.");
        }
        return instance;
    }

    private GenericDatabase(Context context) {
        super(context, "test.db", null, 1);
    }

    public void clearTables() throws SQLException {
        TableUtils.clearTable(getConnectionSource(), GenericChild.class);
        TableUtils.clearTable(getConnectionSource(), GenericResource.class);
        TableUtils.clearTable(getConnectionSource(), UnsyncedResource.class);
    }

    @Override
    public <T> Server getServer(Class<T> tClass) {
        return MockServer.getInstance();
    }

    @Override
    public void onCreate(SQLiteDatabase database, ConnectionSource connectionSource) {
        super.onCreate(database, connectionSource);

        try {
            TableUtils.createTable(connectionSource, GenericResource.class);
            TableUtils.createTable(connectionSource, GenericChild.class);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase database, ConnectionSource connectionSource, int oldVersion, int newVersion) {

    }
}
