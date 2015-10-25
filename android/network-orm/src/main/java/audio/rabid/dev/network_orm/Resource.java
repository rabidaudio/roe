package audio.rabid.dev.network_orm;

import com.j256.ormlite.field.DatabaseField;

import org.jetbrains.annotations.Nullable;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;

/**
 * Created by charles on 10/23/15.
 *
 * Because a single record is shared as a single instance (possibly to multiple threads), all mutable
 * changes should be wrapped in a synchronized(*.Lock){} block for thread safety. TODO doesn't protect against simultaneous off-thread save/deletes
 */
public abstract class Resource<T extends Resource> extends TypedObservable<T> {



    public abstract Dao<T> getDao();

    public final Object Lock = new Object();

    @DatabaseField(generatedId = true)
    protected int id = -1;

    @DatabaseField(index = true)
    protected int serverId = -1;

    @DatabaseField
    protected boolean synced = false;

    @DatabaseField
    protected Date createdAt;

    @DatabaseField
    protected Date updatedAt;

    private boolean deleted = false;

    public int getId(){
        return id;
    }

    public boolean isSynced(){
        return synced;
    }

    public boolean wasDeleted(){
         return deleted;
    }

    public Date getCreatedAt(){
        return createdAt;
    }

    public Date getUpdatedAt(){
        return updatedAt;
    }

    public boolean isNew(){
        return id < 0;
    }

    public abstract AllowedOps getAllowedOps();

    @SuppressWarnings("unchecked")
    public synchronized void save(@Nullable final Dao.SingleQueryCallback<T> callback){
        Date currentTime = new Date();
        if(createdAt==null) createdAt = currentTime;
        updatedAt = currentTime;
        getDao().save((T) this, new Dao.SingleQueryCallback<T>() {
            @Override
            public void onResult(T result) {
                setChanged();
                notifyObservers();
                if(callback!=null) callback.onResult(result);
            }
        });
    }

    @SuppressWarnings("unchecked")
    public synchronized void delete(@Nullable final Dao.SingleQueryCallback<T> callback){
        getDao().delete((T) this, new Dao.SingleQueryCallback<T>() {
            @Override
            public void onResult(T result) {
                deleted = true;
                setChanged();
                notifyObservers();
                if (callback != null) callback.onResult(result);
            }
        });
    }

    public JSONObject toJSON() throws JSONException {
        return new JSONObject()
                .put("id", serverId)
                .put("created_at", NetworkDate.encode(createdAt))
                .put("updated_at", NetworkDate.encode(updatedAt));
    }

    protected void fromJSON(JSONObject data) throws JSONException {
        serverId = data.getInt("id");
    }
}
