package audio.rabid.dev.network_orm.models;

import android.support.annotation.Nullable;
import android.util.Log;

import com.j256.ormlite.field.DatabaseField;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Field;
import java.text.ParseException;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Created by charles on 10/23/15.
 *
 * The {@link Resource} is your base class that all of your models should subclass (passing their class
 * in for the generic <code><T></code>). It gives you a couple of default fields, including local and
 * remote public keys. It also gives you built-in ways to convert your models to and from {@link JSONObject}s.
 *
 * For each of your custom fields, use the {@link DatabaseField} annotation so ORMLite knows which to
 * store in the database. Also use the {@link JSONField} annotation to help roe convert your models
 * to JSON. You can also override {@link #toJSON()} and {@link #updateFromJSON(JSONObject)} if you
 * need finer control over the conversion (or for performance reasons).
 *
 * Because a single record is shared as a single instance (possibly to multiple threads), be sure to
 * synchronize where necessary.
 */
public abstract class Resource<T extends Resource> extends TypedObservable<T> {

    private Map<String, Field> jsonFields = null;

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

    /**
     * An auto-generated public key for the item in the local database. Can be null if the item has
     * not yet been saved locally.
     */
    @Nullable
    public Integer getId() {
        return id;
    }

    /**
     * The public key of the item for the network. This can (and almost surely will) be different than
     * the local id, since if a new item is created without network availability, it must have a local
     * id, but it doesn't yet have a server id.
     */
    @Nullable
    public Integer getServerId() {
        return serverId;
    }

    /**
     * If the item has pending changes that have not been saved to the network, then this will return
     * false.
     */
    public boolean isSynced() {
        return synced;
    }

    /**
     * This will return true after the item has been deleted. {@link TypedObserver}s should check this
     * on {@link TypedObserver#update(TypedObservable, Object)}, because the instance will still be
     * available.
     */
    public boolean wasDeleted() {
        return deleted;
    }

    /**
     * A timestamp for when the item was created locally. Totally independent of server create time
     * (if applicable).
     */
    public Date getCreatedAt() {
        return createdAt;
    }

    /**
     * A timestamp for when the item was last updated locally. Totally independent of server update time
     * (if applicable).
     */
    public Date getUpdatedAt() {
        return updatedAt;
    }

    /**
     * Returns true if the item has not yet been saved locally.
     */
    public boolean isNew() {
        return id != null;
    }

    /**
     * The {@link Source} is the way you manipulate {@link Resource}s. Use the source to query for
     * instances and do CRUD operations. The {@link Source} will handle all of the caching, updating
     * of database and network, etc. Since they are somewhat heavy, you should make a static instance
     * and use it across all of your {@link Resource} instances. You can either use the default
     * {@link Source} class or subclass it to add your own query methods.
     *
     * @see Source
     */
    public abstract Source<T> getSource();

    /**
     * Save the current item. Shorthand for {@link Source#createOrUpdate(Resource, Source.OperationCallback)}
     *
     * @param callback (optional) callback to run when save is complete
     */
    @SuppressWarnings("unchecked")
    public synchronized void save(@Nullable Source.OperationCallback<T> callback) {
        getSource().createOrUpdate((T) this, callback);
    }

    /**
     * Delete the current item. Shorthand for {@link Source#deleteLocal(Resource, Source.OperationCallback)}
     *
     * @param callback (optional) callback to run when delete is complete
     */
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
                        } else {
                            value = getSource().getDateFormat().format((Date) field.get(this));
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
     * Set values from a JSON object. If you override this method, be sure to mark it as synchronized
     * to make its changes thread-safe. Also DO NOT save the object after populating values, as that
     * is handled by the {@link Source}. Don't set createdAt or updatedAt either.
     *
     * @return true if any of the fields changed, or false if the object is exactly the same as before
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
                        try {
                            Date newDate = getSource().getDateFormat().parse((String) value);
                            if (newDate != null &&
                                    (f.get(this) == null || ((Date) f.get(this)).getTime() != newDate.getTime())) {
                                f.set(this, newDate);
                                updated = true;
                            }
                        } catch (ParseException e) {
                            throw new JSONException(e.getMessage());
                        }
                    }else if(dbf != null && dbf.foreign()){
                        //TODO how to populate children?
                        Log.w("JSON", "Didn't populate relation "+f.getName());
                    }else{
                        if(f.get(this) == null || !f.get(this).equals(value)) {
                            try {
                                f.set(this, value);
                                updated = true;
                            } catch (Exception e) {
                                throw new JSONException(String.format(
                                        "Couldn't figure out how to map '%s' to field '%s' on %s",
                                        String.valueOf(value), key, getClass().toString()));
                            }
                        }
                    }
                }
            }catch (IllegalAccessException e){
                throw new JSONException(e.getMessage());
            }
        }
        return updated;
    }
}
