//package audio.rabid.dev.roe.models;
//
//import android.content.Context;
//import android.database.sqlite.SQLiteDatabase;
//
//import com.j256.ormlite.android.apptools.OrmLiteSqliteOpenHelper;
//import com.j256.ormlite.support.ConnectionSource;
//import com.j256.ormlite.table.TableUtils;
//
//import java.io.File;
//import java.io.InputStream;
//import java.sql.SQLException;
//
///**
// * Created by charles on 11/9/15.
// */
//public abstract class RoeDatabaseOld extends OrmLiteSqliteOpenHelper {
//
////    private HashMap<Class<? extends Resource>, Source> sources = new HashMap<>();
//
//    /**
//     * @param context         Associated content from the application. This is needed to locate the database.
//     * @param databaseName    Name of the database we are opening.
//     * @param factory         Cursor factory or null if none.
//     * @param databaseVersion Version of the database we are opening. This causes {@link #onUpgrade(SQLiteDatabase, int, int)} to be
//     */
//    public RoeDatabaseOld(Context context, String databaseName, SQLiteDatabase.CursorFactory factory, int databaseVersion) {
//        super(context, databaseName, factory, databaseVersion);
//    }
//
//    /**
//     * Same as the other constructor with the addition of a file-id of the table config-file. See
//     * {@link OrmLiteConfigUtil} for details.
//     *
//     * @param context         Associated content from the application. This is needed to locate the database.
//     * @param databaseName    Name of the database we are opening.
//     * @param factory         Cursor factory or null if none.
//     * @param databaseVersion Version of the database we are opening. This causes {@link #onUpgrade(SQLiteDatabase, int, int)} to be
//     *                        called if the stored database is a different version.
//     * @param configFileId
//     */
//    public RoeDatabaseOld(Context context, String databaseName, SQLiteDatabase.CursorFactory factory, int databaseVersion, int configFileId) {
//        super(context, databaseName, factory, databaseVersion, configFileId);
//    }
//
//    /**
//     * Same as the other constructor with the addition of a config-file. See {@link OrmLiteConfigUtil} for details.
//     *
//     * @param context         Associated content from the application. This is needed to locate the database.
//     * @param databaseName    Name of the database we are opening.
//     * @param factory         Cursor factory or null if none.
//     * @param databaseVersion Version of the database we are opening. This causes {@link #onUpgrade(SQLiteDatabase, int, int)} to be
//     *                        called if the stored database is a different version.
//     * @param configFile
//     */
//    public RoeDatabaseOld(Context context, String databaseName, SQLiteDatabase.CursorFactory factory, int databaseVersion, File configFile) {
//        super(context, databaseName, factory, databaseVersion, configFile);
//    }
//
//    /**
//     * Same as the other constructor with the addition of a input stream to the table config-file. See
//     * {@link OrmLiteConfigUtil} for details.
//     *
//     * @param context         Associated content from the application. This is needed to locate the database.
//     * @param databaseName    Name of the database we are opening.
//     * @param factory         Cursor factory or null if none.
//     * @param databaseVersion Version of the database we are opening. This causes {@link #onUpgrade(SQLiteDatabase, int, int)} to be
//     *                        called if the stored database is a different version.
//     * @param stream
//     */
//    public RoeDatabaseOld(Context context, String databaseName, SQLiteDatabase.CursorFactory factory, int databaseVersion, InputStream stream) {
//        super(context, databaseName, factory, databaseVersion, stream);
//    }
//
//    @Override
//    public void onCreate(SQLiteDatabase database, ConnectionSource connectionSource) {
//        try {
//            TableUtils.createTable(connectionSource, DeletedResource.class);
//            TableUtils.createTable(connectionSource, UnsyncedResource.class);
//        } catch (SQLException e) {
//            throw new RuntimeException("Problem creating database", e);
//        }
//    }
//
////    @SuppressWarnings("unchecked")
////    public <S extends Source<R, LK>, R extends Resource<LK>, LK> S getSource(Class<R> rClass){
////        return (S) sources.get(rClass);
////    }
////
////    public <S extends Source<R, LK>, R extends Resource<LK>, LK> void addSource(Class<R> rClass, S source){
////        sources.put(rClass, source);
////    }
//}