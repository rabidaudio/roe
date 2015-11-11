//package audio.rabid.dev.roe.ormlike;
//
//import android.database.sqlite.SQLiteDatabase;
//import android.os.Build;
//import android.os.Looper;
//
//import org.json.JSONException;
//import org.json.JSONObject;
//
//import nl.elastique.poetry.json.JsonPersister;
//
///**
// * Created by charles on 11/10/15.
// */
//public class RoeJSONPersister extends JsonPersister {
//
//    /**
//     * @inheritDoc
//     */
//    public RoeJSONPersister(SQLiteDatabase writableDatabase) {
//        super(writableDatabase);
//    }
//
//    /**
//     * @inheritDoc
//     */
//    public RoeJSONPersister(SQLiteDatabase writableDatabase, int options) {
//        super(writableDatabase, options);
//    }
//
//    /**
//     * Recursively persist this object and all its children.
//     *
//     * @param modelClass the type to persist
//     * @param jsonObject the json to process
//     * @param <IdType> the ID type to return
//     * @return the ID of the persisted object
//     * @throws JSONException when something went wrong through parsing, this also fails the database transaction and results in no data changes
//     */
//    public <IdType> IdType persistObjectByOverwrite(Class<?> modelClass, JSONObject jsonObject) throws JSONException
//    {
//        if (Looper.myLooper() == Looper.getMainLooper())
//        {
//            sLogger.warn("please call persistObject() on a background thread");
//        }
//
//        if (Build.VERSION.SDK_INT >= 11)
//        {
//            return persistObjectApi11(modelClass, jsonObject);
//        }
//        else
//        {
//            return persistObjectApiDeprecate(modelClass, jsonObject);
//        }
//    }
//}
