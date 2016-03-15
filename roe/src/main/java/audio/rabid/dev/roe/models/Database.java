package audio.rabid.dev.roe.models;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;

import com.j256.ormlite.android.apptools.OrmLiteSqliteOpenHelper;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.TableUtils;

import java.io.File;
import java.io.InputStream;
import java.sql.SQLException;

/**
 * Created by charles on 11/11/15.
 *
 * Use this instead of {@link OrmLiteSqliteOpenHelper} to get the creation of sync tables and instantiation of
 * {@link NetworkSyncableDao}s handled automatically. Just be sure to override {@link #onCreate(SQLiteDatabase, ConnectionSource)}
 * and call super in it!
 */
public abstract class Database extends OrmLiteSqliteOpenHelper {

    /**
     * @inheritDoc
     */
    public Database(Context context, String databaseName, SQLiteDatabase.CursorFactory factory, int databaseVersion) {
        super(context, databaseName, factory, databaseVersion);
    }

    /**
     * @inheritDoc
     */
    public Database(Context context, String databaseName, SQLiteDatabase.CursorFactory factory, int databaseVersion, int configFileId) {
        super(context, databaseName, factory, databaseVersion, configFileId);
    }

    /**
     * @inheritDoc
     */
    public Database(Context context, String databaseName, SQLiteDatabase.CursorFactory factory, int databaseVersion, File configFile) {
        super(context, databaseName, factory, databaseVersion, configFile);
    }

    /**
     * @inheritDoc
     */
    public Database(Context context, String databaseName, SQLiteDatabase.CursorFactory factory, int databaseVersion, InputStream stream) {
        super(context, databaseName, factory, databaseVersion, stream);
    }

    @Override
    public <D extends Dao<T, ?>, T> D getDao(Class<T> clazz) {
        try {
            D result = super.getDao(clazz);
            if (clazz != UnsyncedResource.class && result instanceof NetworkSyncableDao) {
                ((NetworkSyncableDao) result).initialize(getServer(clazz), this);
            }
            return result;
        }catch (SQLException e){
            throw new RuntimeException(e);
        }
    }

    @Override
    public void onCreate(SQLiteDatabase database, ConnectionSource connectionSource) {
        try {
            TableUtils.createTableIfNotExists(connectionSource, UnsyncedResource.class);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public abstract <T> Server getServer(Class<T> tClass);

    public synchronized boolean isSyncRequired() {
        try {
            return getDao(UnsyncedResource.class).countOf() > 0;
        }catch (SQLException e){
            e.printStackTrace();
            return true;
        }
    }

    public synchronized boolean blockingSyncWithServer(){
        try {
            return UnsyncedResource.attemptAllPendingRequests(this, null);
        }catch (SQLException e){
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Get progress on sync task. Override {@link #onPostExecute(Object)} and {@link #onProgressUpdate(Object[])}
     * (which returns two ints, the number complete and the total) to update the UI while sync is in progress.
     */
    public class SyncTask extends AsyncTask<Void, Integer, Boolean> implements UnsyncedResource.SyncProgressListener {
        @Override
        protected Boolean doInBackground(Void... params) {
            try {
                return UnsyncedResource.attemptAllPendingRequests(Database.this, this);
            } catch (SQLException e) {
                e.printStackTrace();
                return false;
            }
        }
        @Override
        public void onProgress(int complete, int total) {
            publishProgress(complete, total);
        }
    }
}