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

    public abstract Source<T> getSource();

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

    protected boolean deleted = false;

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

    public int getServerId(){
        return serverId;
    }

    @SuppressWarnings("unchecked")
    public synchronized void save(@Nullable Source.QueryCallback<T> callback){
        getSource().createOrUpdate((T) this, callback);
    }

    @SuppressWarnings("unchecked")
    public synchronized void delete(@Nullable Source.QueryCallback<T> callback){
        getSource().delete((T) this, true, callback);
    }

    @Override
    public String toString(){
        try {
            return getClass().getSimpleName() + ": " + toJSON().toString();
        }catch (JSONException e){
            return super.toString();
        }
    }

    public JSONObject toJSON() throws JSONException {
        return new JSONObject()
                .put("id", serverId)
                .put("created_at", NetworkDate.encode(createdAt))
                .put("updated_at", NetworkDate.encode(updatedAt));
    }

    /**
     * Set values from a JSON object
     * @return were any object fields changed?
     */
    protected synchronized boolean updateFromJSON(JSONObject data) throws JSONException{
        int id = data.getInt("id");
        boolean updated = serverId != id;
        if(updated){
            serverId = id;
        }
        return updated;
    }
}
