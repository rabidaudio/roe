package audio.rabid.dev.roe.testobjects;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.TableUtils;

import java.sql.SQLException;

import audio.rabid.dev.roe.models.RoeDatabase;
import audio.rabid.dev.roe.models.Server;
import audio.rabid.dev.roe.models.UnsyncedResource;

/**
 * Created by charles on 10/30/15.
 */
public class GenericDatabase extends RoeDatabase {

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
            TableUtils.clearTable(getConnectionSource(), NoNetworkResource.class);
            TableUtils.clearTable(getConnectionSource(), UnsyncedResource.class);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private GenericDatabase(Context context) {
        super(context, "simple.db", null, VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase database, ConnectionSource connectionSource) {
        super.onCreate(database, connectionSource);
        try {
            TableUtils.createTable(connectionSource, DummyObject.class);
            TableUtils.createTable(connectionSource, DummyChild.class);
            TableUtils.createTable(connectionSource, NoNetworkResource.class);
        } catch (SQLException e) {
            throw new RuntimeException("Problem creating database", e);
        }
    }

    private static final int VERSION = 1;

    @Override
    public void onUpgrade(SQLiteDatabase database, ConnectionSource connectionSource, int oldVersion, int newVersion) {

    }

    @Override
    public <T> Server getServer(Class<T> tClass) {
        return DummyObjectMockServer.getInstance();
    }
}
