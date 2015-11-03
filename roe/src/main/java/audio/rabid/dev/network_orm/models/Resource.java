package audio.rabid.dev.network_orm.models;

import android.support.annotation.Nullable;
import android.util.Log;

import com.j256.ormlite.field.DatabaseField;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Field;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

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
    @JSONField(export = false, accept = false)
    protected Integer id;

    @JSONField(key = "id")
    @DatabaseField(index = true)
    protected Integer serverId = null;

    @DatabaseField
    @JSONField(export = false, accept = false)
    protected boolean synced = false;

    @DatabaseField
    @JSONField(key = "created_at", accept = false)
    protected Date createdAt;

    @DatabaseField
    @JSONField(key = "updated_at", accept = false)
    protected Date updatedAt;

    protected boolean deleted = false;

    @Nullable
    public Integer getId() {
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
        return id != null;
    }

    @Nullable
    public Integer getServerId() {
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

    /**
     * This method uses reflection by default, using the {@link JSONField} annotation. Override to
     * customize the json output of your Resource.
     */
    public JSONObject toJSON() throws JSONException {
        JSONObject object = new JSONObject();

        for(Map.Entry<String, Field> f : getJSONFields().entrySet()){
            Field field = f.getValue();
            JSONField jsonField = field.getAnnotation(JSONField.class);
            DatabaseField dbf = f.getValue().getAnnotation(DatabaseField.class);
            try {
                if(jsonField.export()) {
                    Object value;
                    if (field.getType().isAssignableFrom(Date.class)) {
                        //dates
                        //TODO allow custom date formats
                        if(field.get(this) == null){
                            value = JSONObject.NULL; //explicit null value
                        }else {
                            value = NetworkDate.encode((Date) field.get(this));
                        }
                    } else if (dbf != null && dbf.foreign()) {
                        //foreign fields
                        Resource r = (Resource) field.get(this);
                        if (r != null && r.getId() != null) {
                            value = r.getId();
                        } else {
                            value = JSONObject.NULL; //explicit null value
                        }
                    } else {
                        value = field.get(this);
                    }
                    object.put(f.getKey(), value);
                }
            } catch (Exception e) {
                throw new JSONException(e.getMessage());
            }
        }
        return object;
    }

    //app mockups > severity.jpg

    private Map<String, Field> jsonFields = null;
    private Map<String, Field> getJSONFields(){
        if(jsonFields==null) {
            jsonFields = new HashMap<>();
            for (Class<?> classWalk = getClass(); classWalk != null; classWalk = classWalk.getSuperclass()) {
                for (Field field : classWalk.getDeclaredFields()) {
                    JSONField jsonField = field.getAnnotation(JSONField.class);
                    if (jsonField != null) {
                        String key = jsonField.key();
                        if(key == null || key.isEmpty()){
                            key = field.getName();
                        }
                        jsonFields.put(key, field);
                    }
                }
            }
        }
        return jsonFields;
    }

    /**
     * Set values from a JSON object
     *
     * @return were any object fields changed?
     */
    public synchronized boolean updateFromJSON(JSONObject data) throws JSONException {
        boolean updated = false;
        Iterator<String> keys = data.keys();
        while (keys.hasNext()){
            String key = keys.next();
            Object value = data.get(key);
            Field f = getJSONFields().get(key);
            JSONField jsonField = f.getAnnotation(JSONField.class);
            try {
                if (value != null && jsonField.accept()) {
                    DatabaseField dbf = f.getAnnotation(DatabaseField.class);
                    if(f.getType().isAssignableFrom(Date.class)) {
                        Date newDate = NetworkDate.decode((String) value);
                        if (newDate != null && (f.get(this) == null || ((Date) f.get(this)).getTime() != newDate.getTime())) {
                            f.set(this, newDate);
                            updated = true;
                        }
                    }else if(dbf != null && dbf.foreign()){
                        //TODO how to populate children?
                        Log.w("JSON", "Didn't populate relation "+f.getName());

//                    }else if(f.getType().isAssignableFrom(value.getClass())){
                    }else{
                        if(f.get(this) == null || !f.get(this).equals(value)) {
                            f.set(this, value);
                            updated = true;
                        }
//                    }else{
//                        throw new JSONException("Couldn't figure out how to map "+String.valueOf(value)+" to field "+key);
                    }
                }
            }catch (IllegalAccessException e){
                throw new JSONException(e.getMessage());
            }
        }
        return updated;
    }
}
