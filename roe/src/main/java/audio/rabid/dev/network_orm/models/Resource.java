package audio.rabid.dev.network_orm.models;

import android.support.annotation.Nullable;

import com.j256.ormlite.field.DatabaseField;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;

import audio.rabid.dev.network_orm.models.rails.NetworkDate;

/**
 * Created by charles on 10/23/15.
 * <p/>
 * Because a single record is shared as a single instance (possibly to multiple threads), all mutable
 * changes should be wrapped in a synchronized(*.Lock){} block for thread safety. TODO doesn't protect against simultaneous off-thread save/deletes
 */
public abstract class Resource<T extends Resource> extends TypedObservable<T> {

    public abstract Source<T> getSource();

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

    public int getId() {
        return id;
    }

    public boolean isSynced() {
        return synced;
    }

    public boolean wasDeleted() {
        return deleted;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public Date getUpdatedAt() {
        return updatedAt;
    }

    public boolean isNew() {
        return id < 0;
    }

    public int getServerId() {
        return serverId;
    }

    @SuppressWarnings("unchecked")
    public synchronized void save(@Nullable Source.OperationCallback<T> callback) {
        getSource().createOrUpdate((T) this, callback);
    }

    @SuppressWarnings("unchecked")
    public synchronized void delete(@Nullable Source.OperationCallback<T> callback) {
        getSource().deleteLocal((T) this, callback);
    }

    @Override
    public String toString() {
        try {
            return getClass().getSimpleName() + ": " + toJSON().toString();
        } catch (JSONException e) {
            return super.toString();
        }
    }

    public JSONObject toJSON() throws JSONException {
        JSONObject o = new JSONObject()
                .put("id", serverId);
        if (createdAt != null) {
            o.put("created_at", NetworkDate.encode(createdAt));  //TODO allow custom date formats
        }
        if (updatedAt != null) {
            o.put("updated_at", NetworkDate.encode(updatedAt));
        }
        return o;
    }

    /**
     * Set values from a JSON object
     *
     * @return were any object fields changed?
     */
    public synchronized boolean updateFromJSON(JSONObject data) throws JSONException {
        int id = data.getInt("id");
        boolean updated = serverId != id;
        if (updated) {
            serverId = id;
        }
        return updated;
    }
}
