package audio.rabid.dev.sampleapp;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.TableUtils;

import java.sql.SQLException;

import audio.rabid.dev.roe.models.RoeDatabase;
import audio.rabid.dev.sampleapp.models.Author;
import audio.rabid.dev.sampleapp.models.Post;

/**
 * Created by charles on 10/23/15.
 */
public class Database extends RoeDatabase {

    private static Database instance;

    public static Database getInstance() {
        if (instance == null)
            throw new RuntimeException("Need to create an instance with context first");
        return instance;
    }

    public static Database createInstance(Context context) {
        if (instance == null) {
            instance = new Database(context);
        }
        return instance;
    }

    private Database(Context context) {
        super(context, "simple.db", null, VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase database, ConnectionSource connectionSource) {
        super.onCreate(database, connectionSource);
        try {
            TableUtils.createTable(connectionSource, Author.class);
            TableUtils.createTable(connectionSource, Post.class);
        } catch (SQLException e) {
            throw new RuntimeException("Problem creating database", e);
        }
    }

    private static final int VERSION = 2;

    @Override
    public void onUpgrade(SQLiteDatabase database, ConnectionSource connectionSource, int oldVersion, int newVersion) {
        try {
            switch (oldVersion) {
                case 1: //--> 2
                    TableUtils.createTable(connectionSource, Post.class);
                case 2: //--> 3
//                database.execSQL(/* SOME SQL */);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Problem upgrading database", e);
        }
    }
}
