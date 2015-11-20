package audio.rabid.dev.roe.models.idlookups;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.TableUtils;

import java.sql.SQLException;

import audio.rabid.dev.roe.models.Database;
import audio.rabid.dev.roe.models.Server;
import audio.rabid.dev.roe.models.UnsyncedResource;

/**
 * Created by charles on 11/13/15.
 */
public class IdDb extends Database {

    private static IdDb instance;

    public static void createInstance(Context context) {
        if (instance == null) {
            instance = new IdDb(context);
        }
    }

    public static IdDb getInstance() {
        if (instance == null) {
            throw new IllegalArgumentException("Need to initialize with Context first.");
        }
        return instance;
    }

    private IdDb(Context context) {
        super(context, "idtest.db", null, 1);
    }

    public void clearTables() throws SQLException {
        TableUtils.clearTable(getConnectionSource(), IntegerIdResource.class);
        TableUtils.clearTable(getConnectionSource(), StringIdResource.class);
        TableUtils.clearTable(getConnectionSource(), IntIdResource.class);
        TableUtils.clearTable(getConnectionSource(), DoubleIdResource.class);
        TableUtils.clearTable(getConnectionSource(), UnsyncedResource.class);
    }

    @Override
    public <T> Server getServer(Class<T> tClass) {
        return null;
    }

    @Override
    public void onCreate(SQLiteDatabase database, ConnectionSource connectionSource) {
        super.onCreate(database, connectionSource);

        try {
            TableUtils.createTable(getConnectionSource(), IntegerIdResource.class);
            TableUtils.createTable(connectionSource, StringIdResource.class);
            TableUtils.createTable(connectionSource, IntIdResource.class);
            TableUtils.createTable(connectionSource, DoubleIdResource.class);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase database, ConnectionSource connectionSource, int oldVersion, int newVersion) {

    }
}
