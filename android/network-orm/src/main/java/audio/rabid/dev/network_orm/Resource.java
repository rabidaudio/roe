package audio.rabid.dev.network_orm;

import com.j256.ormlite.field.DatabaseField;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by charles on 10/23/15.
 */
public abstract class Resource<T extends Resource> {

    public abstract Dao<T> getDao();

    @DatabaseField(generatedId = true)
    protected int id;

    @DatabaseField(index = true)
    protected int serverId = -1;

    @DatabaseField
    protected boolean synced = false;

    public int getId(){
        return id;
    }

    public boolean isSynced(){
        return synced;
    }

    @SuppressWarnings("unchecked")
    public void save(Dao.SingleQueryCallback<T> callback){
        getDao().save((T) this, callback);
    }

    @SuppressWarnings("unchecked")
    public void delete(Dao.SingleQueryCallback<T> callback){
        getDao().delete((T) this, callback);
    }

    public abstract JSONObject toJSON() throws JSONException;
}
