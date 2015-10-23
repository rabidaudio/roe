package audio.rabid.dev.sampleapp.backend;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import com.j256.ormlite.android.apptools.OrmLiteSqliteOpenHelper;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.TableUtils;

import java.sql.SQLException;

import audio.rabid.dev.sampleapp.models.Author;

/**
 * Created by charles on 10/23/15.
 */
public class Database extends OrmLiteSqliteOpenHelper {

    private static final Class[] MODELS = {Author.class};

    private static Database instance;

    public static Database getInstance(){
        if(instance==null) throw new RuntimeException("Need to create an instance with context first");
        return instance;
    }

    public static Database getInstance(Context context){
        if(instance == null){
            instance = new Database(context);
        }
        return instance;
    }

    private Database(Context context){
        super(context, "simple.db", null, 1);
    }

    public static com.j256.ormlite.dao.Dao getDBDao(Class clazz){
        try {
            return getInstance().getDao(clazz);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void onCreate(SQLiteDatabase database, ConnectionSource connectionSource) {

        try {
            for(Class c : MODELS){
                TableUtils.createTable(connectionSource, c);
            }

        }catch (SQLException e){
            throw new RuntimeException("Problem creating database", e);
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase database, ConnectionSource connectionSource, int oldVersion, int newVersion) {
        //TODO use oldVersion and newVersion to do migrations here. increment VERSION for changes

        //http://ormlite.com/javadoc/ormlite-core/doc-files/ormlite_4.html#Upgrading-Schema
    }
}
