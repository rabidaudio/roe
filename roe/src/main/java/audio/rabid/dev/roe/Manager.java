package audio.rabid.dev.roe;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import com.j256.ormlite.android.apptools.OrmLiteSqliteOpenHelper;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.TableUtils;

import org.jdeferred.android.AndroidDeferredManager;

import java.net.MalformedURLException;
import java.net.URL;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;

import audio.rabid.dev.roe.models.resource.Resource;

/**
 * Created by  charles  on 3/15/16.
 *
 * Tables are created automatically at start, but you'll have to handle migrations yourself. To do
 * so, override {@link #onUpgrade(SQLiteDatabase, int, int)} with something like this:
 * <pre>
 *   try {
 *      switch (oldVersion) {
 *          case 1: //--> 2
 *              database.execSQL(...);
 *          case 2: //--> 3
 *      }
 *  } catch (SQLException e) {
 *      throw new RuntimeException("Problem upgrading database", e);
 *  }
 * </pre>
 */
public abstract class Manager extends OrmLiteSqliteOpenHelper {

    private List<Resource> resources;

    private URL baseURL;

    public AndroidDeferredManager deferredManager = new AndroidDeferredManager();

    public Manager(Context context, String databaseName, int databaseVersion, String baseURL, Resource... resources) {
        super(context, databaseName, null, databaseVersion);
        this.resources = Arrays.asList(resources);
        for(Resource r : resources){
            r.setManager(this);
        }
        try {
            this.baseURL = new URL(baseURL);
        }catch (MalformedURLException e){
            throw new RuntimeException(e);
        }
    }

    @Override
    public void onCreate(SQLiteDatabase database, ConnectionSource connectionSource) {
        for(Resource r : resources){
            try {
                TableUtils.createTableIfNotExists(connectionSource, r.getModelClass());
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
