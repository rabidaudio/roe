package audio.rabid.dev.sampleapp.backend;

import android.support.annotation.Nullable;

import com.j256.ormlite.field.DatabaseField;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by charles on 10/23/15.
 */
public abstract class Resource<T extends Resource> {

    public abstract Dao<T> getDao();

    @DatabaseField(id=true)
    protected int id;

    @DatabaseField(unique = true, index = true)
    protected int serverId;

    @DatabaseField
    protected boolean synced = false;

    public int getId(){
        return id;
    }

    public boolean isSynced(){
        return synced;
    }

    @SuppressWarnings("unchecked")
    public void save(@Nullable Dao.SingleQueryCallback<T> callback){
        getDao().save((T) this, callback);
    }

    @SuppressWarnings("unchecked")
    public void delete(@Nullable Dao.SingleQueryCallback<T> callback){
        getDao().delete((T) this, callback);
    }

    public abstract JSONObject toJSON() throws JSONException;
}
