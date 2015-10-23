package audio.rabid.dev.sampleapp;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import com.j256.ormlite.android.apptools.OrmLiteSqliteOpenHelper;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.TableUtils;

import java.sql.SQLException;

import audio.rabid.dev.sampleapp.models.Author;

/**
 * Created by charles on 10/23/15.
 */
public class Database extends OrmLiteSqliteOpenHelper {

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
        super(context, "simple.db", null, VERSION);
    }

    public static Dao getDBDao(Class clazz){
        try {
            return getInstance().getDao(clazz);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void onCreate(SQLiteDatabase database, ConnectionSource connectionSource) {
        try {
            TableUtils.createTable(connectionSource, Author.class);
        }catch (SQLException e){
            throw new RuntimeException("Problem creating database", e);
        }
    }

    private static final int VERSION = 1;

    @Override
    public void onUpgrade(SQLiteDatabase database, ConnectionSource connectionSource, int oldVersion, int newVersion) {
        //http://ormlite.com/javadoc/ormlite-core/doc-files/ormlite_4.html#Upgrading-Schema
        while (oldVersion < newVersion){
            switch (oldVersion){
                case 2:
                    //TODO new migration
            }
            oldVersion++;
        }
    }
}
