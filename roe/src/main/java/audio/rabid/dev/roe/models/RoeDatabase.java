package audio.rabid.dev.roe.models;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import com.j256.ormlite.android.apptools.OrmLiteSqliteOpenHelper;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.TableUtils;

import java.io.File;
import java.io.InputStream;
import java.sql.SQLException;

/**
 * Created by charles on 11/10/15.
 */
public abstract class RoeDatabase extends OrmLiteSqliteOpenHelper {

    /**
     * @inheritDoc
     */
    public RoeDatabase(Context context, String databaseName, SQLiteDatabase.CursorFactory factory, int databaseVersion) {
        super(context, databaseName, factory, databaseVersion);
    }

    /**
     * @inheritDoc
     */
    public RoeDatabase(Context context, String databaseName, SQLiteDatabase.CursorFactory factory, int databaseVersion, int configFileId) {
        super(context, databaseName, factory, databaseVersion, configFileId);
    }

    /**
     * @inheritDoc
     */
    public RoeDatabase(Context context, String databaseName, SQLiteDatabase.CursorFactory factory, int databaseVersion, File configFile) {
        super(context, databaseName, factory, databaseVersion, configFile);
    }

    /**
     * @inheritDoc
     */
    public RoeDatabase(Context context, String databaseName, SQLiteDatabase.CursorFactory factory, int databaseVersion, InputStream stream) {
        super(context, databaseName, factory, databaseVersion, stream);
    }

    @Override
    public <D extends Dao<T, ?>, T> D getDao(Class<T> clazz) throws SQLException {
        D result = super.getDao(clazz);
        if (clazz != UnsyncedResource.class && result instanceof NetworkRoeDao) {
            ((NetworkRoeDao) result).initialize(getServer(clazz), this);
        }
        return result;
    }

    @Override
    public void onCreate(SQLiteDatabase database, ConnectionSource connectionSource) {
        try {
            TableUtils.createTableIfNotExists(connectionSource, UnsyncedResource.class);
        }catch (SQLException e){
            throw new RuntimeException(e);
        }
    }

    public abstract <T> Server getServer(Class<T> tClass);
}
