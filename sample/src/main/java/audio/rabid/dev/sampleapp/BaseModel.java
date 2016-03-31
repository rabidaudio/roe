package audio.rabid.dev.sampleapp;

import com.snappydb.DB;

import audio.rabid.dev.roe.JSONAPIModel;

/**
 * Created by  charles  on 3/30/16.
 */
public abstract class BaseModel<T> extends JSONAPIModel<T> {

    private DB database;

    public BaseModel(DB database, String pathPattern){
        super("http://localhost:3000", pathPattern);
        this.database = database;
    }

    @Override
    public void createDatabase(String key, T object) throws Exception {
        database.put(generatePath(key), object);
    }

    @Override
    public void updateDatabase(String key, T object) throws Exception {
        database.put(generatePath(key), object);
    }

    @Override
    public void deleteDatabase(String key) throws Exception {
        database.del(key);
    }
}
