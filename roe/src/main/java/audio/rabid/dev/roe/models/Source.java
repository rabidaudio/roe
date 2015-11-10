package audio.rabid.dev.roe.models;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.field.DatabaseField;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Field;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.FieldPosition;
import java.text.ParseException;
import java.text.ParsePosition;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import audio.rabid.dev.roe.BackgroundThread;
import audio.rabid.dev.roe.models.cache.ResourceCache;
import audio.rabid.dev.roe.models.cache.WeakMapResourceCache;

/**
 * Created by charles on 10/28/15.
 * <p>
 * This stores methods for interacting with particular {@link Resource}s. It assumes all calls are from the
 * main thread, and handles fetching off the main thread and returning on it. It also handles instance
 * caches and deciding when to update values. A resource can override this to handle these interactions
 * in a custom way or add more.
 * </p>
 * <p>
 * This will handle running operations in the background and returning on main.
 * </p>
 */
public class Source<R extends Resource<LK>, LK> {

    private Dao<R, LK> dao;
    private Class<R> rClass;
    private ResourceCache<R, LK> resourceCache;
    private PermissionsManager<R> permissions;
    private DateFormat dateFormat;

    private Map<String, Field> jsonFields;

    private Map<R, TypedObservable<R>> observers = new WeakHashMap<>();

    protected TypedObservable<R> getObservable(R item) {
        TypedObservable<R> o = observers.get(item);
        if (o == null) {
            o = new TypedObservable<>();
            observers.put(item, o);
        }
        return o;
    }

    public void addObserver(R item, TypedObserver<R> observer) {
        getObservable(item).addObserver(observer);
    }

    protected void notifyObservers(R item, boolean deleted) {
        getObservable(item).notifyObservers(item, deleted);
    }


    /**
     * Create a new Source
     *
     * @param roeDatabase        the databas instance to use for database operations
     * @param resourceCache   the cache to use for keeping instances consistent
     * @param permissions     the operations allowed to be done on the resource
     * @param dateFormat      the formatter used to map dates to json (defaults to unix timestamp)
     */
    public Source(@NonNull RoeDatabase roeDatabase, @NonNull Class<R> rClass,
                  @Nullable ResourceCache<R, LK> resourceCache,
                  @Nullable PermissionsManager<R> permissions,
                  @Nullable DateFormat dateFormat) {
        try {
            this.dao = roeDatabase.getDao(rClass);
        } catch (SQLException e) {
            onSQLException(e);
        }
        this.rClass = rClass;
        if (resourceCache == null) {
            this.resourceCache = new WeakMapResourceCache<>(50);
        } else {
            this.resourceCache = resourceCache;
        }
        if(permissions == null){
            this.permissions = new SimplePermissionsManager<R>().all();
        }else {
            this.permissions = permissions;
        }
        if(dateFormat==null){
            this.dateFormat = new DateFormat() {
                @Override
                public StringBuffer format(Date date, StringBuffer buffer, FieldPosition field) {
                    return new StringBuffer(String.valueOf(date.getTime()));
                }

                @Override
                public Date parse(String string, ParsePosition position) {
                    try {
                        Date d = new Date(Long.parseLong(string));
                        position.setIndex(string.length());
                        return d;
                    } catch (NumberFormatException e) {
                        position.setErrorIndex(0);
                        return null;
                    }
                }
            };
        }else{
            this.dateFormat = dateFormat;
        }
    }

    public Source(@NonNull RoeDatabase roeDatabase, @NonNull Class<R> rClass) {
        this(roeDatabase, rClass, null, null, null);
    }

    /**
     * Return the {@link Dao} used for database operations. You shouldn't need to access this, as
     * using the <code>doOperation</code> methods should give you the instance to use.
     */
    protected Dao<R, LK> getDao() {
        return dao;
    }

    protected ResourceCache<R, LK> getResourceCache() {
        return resourceCache;
    }

    /**
     * Get the {@link PermissionsManager} for determining which CRUD operations are allowed on a
     * {@link Resource}.
     */
    public PermissionsManager<R> getPermissions() {
        return permissions;
    }

    /**
     * Override this method to catch {@link SQLException}s thrown while trying to do database operations.
     * The default implementation is to throw a {@link RuntimeException}.
     */
    public void onSQLException(SQLException e){
        throw new RuntimeException(e);
    }

    /**
     * Method for encoding a date in JSON. Default implementation is to use Unix time
     * (milliseconds since Jan. 1, 1970, midnight GMT). Override for custom date format.
     */
    public DateFormat getDateFormat() {
        return dateFormat;
    }

    public Class<R> getDataClass() {
        return rClass;
    }


    protected void onAfterCacheAdd(R added) {

    }

    protected void onBeforeCreated(R created) {

    }

    protected void onAfterCreated(R created) {

    }

    protected void onBeforeUpdated(R updated) {

    }

    protected void onAfterUpdated(R updated) {

    }

    protected void onBeforeDeleted(R deleted) {

    }

    protected void onAfterDeleted(R deleted) {

    }

    /**
     * Get a resource by it's local id. Method will try the cache, otherwise resorting to the database
     * and checking for updates on the network if possible.
     */
    public void find(final @NonNull LK localId, @NonNull final OperationCallback<R> callback) {
        BackgroundThread.postBackground(new Runnable() {
            @Override
            public void run() {
                ResourceCache.CacheResult<R> result = resourceCache.get(localId, new ResourceCache.CacheMissCallback<R, LK>() {
                    @Nullable
                    @Override
                    public R onCacheMiss(LK id) {
                        try {
                            return dao.queryForId(localId);
                        } catch (SQLException e) {
                            onSQLException(e);
                            return null;
                        }
                    }
                });
                final R item = permissions.can(Op.READ, result.getItem()) ? result.getItem() : null;
                BackgroundThread.postMain(new Runnable() {
                    @Override
                    public void run() {
                        callback.onResult(item);
                    }
                });
                if (!result.wasInCache()) {
                    onAfterCacheAdd(item);
                }
            }
        });
    }

    /**
     * Get all the instances stored locally. Will default to the cached versions, and update from the
     * network if necessary.
     */
    public void getAllLocal(@NonNull final OperationCallback<List<R>> callback) {
        executeLocalQuery(new LocalQuery<R, LK>() {
            @Override
            public List<R> executeQuery(Dao<R, LK> dao) throws SQLException {
                return dao.queryForAll();
            }
        }, callback);
    }

    public void executeLocalQuery(final LocalQuery<R, LK> localQuery, final @NonNull OperationCallback<List<R>> callback) {
        BackgroundThread.postBackground(new Runnable() {
            @Override
            public void run() {
                final List<R> returnResults = new ArrayList<>();
                final List<R> unCached = new ArrayList<>();
                try {
                    List<R> results = localQuery.executeQuery(dao);
                    for (final R result : results) {
                        if (permissions.can(Op.READ, result)) {
                            returnResults.add(resourceCache.get(result.getId(), new ResourceCache.CacheMissCallback<R, LK>() {
                                @Nullable
                                @Override
                                public R onCacheMiss(LK id) {
                                    unCached.add(result);
                                    return result;
                                }
                            }).getItem());
                        }
                    }
                } catch (SQLException e) {
                    onSQLException(e);
                }
                BackgroundThread.postMain(new Runnable() {
                    @Override
                    public void run() {
                        callback.onResult(returnResults);
                    }
                });
                for (R u : unCached) {
                    onAfterCacheAdd(u);
                }
            }
        });
    }

    protected interface LocalQuery<T, K> {
        List<T> executeQuery(Dao<T, K> dao) throws SQLException;
    }

    public void create(final R resource, @Nullable final OperationCallback<R> callback) {
        if (!resource.isNew()) {
            throw new RuntimeException("Can't create a resource that has already been saved. Use update instead");
        }
        getPermissions().checkPermission(Op.CREATE, resource);
        BackgroundThread.postBackground(new Runnable() {
            @Override
            public void run() {
                try {
                    synchronized (resource) {
                        onBeforeCreated(resource);
                        dao.create(resource);
                        getObservable(resource).setChanged();
                    }
                    resourceCache.put(resource);
                } catch (SQLException e) {
                    onSQLException(e);
                }
                BackgroundThread.postMain(new Runnable() {
                    @Override
                    public void run() {
                        if (callback != null) {
                            callback.onResult(resource);
                        }
                        notifyObservers(resource, false);
                    }
                });
                onAfterCreated(resource);
            }
        });
    }

    public void update(final R resource, final @Nullable OperationCallback<R> callback) {
        if (resource.isNew()) {
            throw new RuntimeException("Can't update a resource that hasn't been created yet. Use create instead");
        }
        getPermissions().checkPermission(Op.UPDATE, resource);
        BackgroundThread.postBackground(new Runnable() {
            @Override
            public void run() {
                resourceCache.get(resource.getId(), new ResourceCache.CacheMissCallback<R, LK>() {
                    @Nullable
                    @Override
                    public R onCacheMiss(LK id) {
                        throw new RuntimeException("Tried to update a resource missing from the cache");
                    }
                });
                try {
                    synchronized (resource) {
                        onBeforeUpdated(resource);
                        dao.update(resource);
                        getObservable(resource).setChanged();
                    }
                } catch (SQLException e) {
                    onSQLException(e);
                }
                BackgroundThread.postMain(new Runnable() {
                    @Override
                    public void run() {
                        if (callback != null) {
                            callback.onResult(resource);
                        }
                        notifyObservers(resource, false);
                    }
                });
                onAfterUpdated(resource);
            }
        });
    }

    public void createOrUpdate(R resource, @Nullable OperationCallback<R> callback) {
        if (resource.isNew()) {
            create(resource, callback);
        } else {
            update(resource, callback);
        }
    }

    public void delete(final R resource, @Nullable final OperationCallback<R> callback) {
        getPermissions().checkPermission(Op.DELETE, resource);
        BackgroundThread.postBackground(new Runnable() {
            @Override
            public void run() {
                try {
                    onBeforeDeleted(resource);
                    dao.delete(resource);
                    resourceCache.delete(resource);
                    synchronized (resource) {
//                        resource.deleted = true;
                        getObservable(resource).setChanged();
                    }
                } catch (SQLException e) {
                    onSQLException(e);
                }
                BackgroundThread.postMain(new Runnable() {
                    @Override
                    public void run() {
                        if (callback != null) {
                            callback.onResult(resource);
                        }
                        notifyObservers(resource, true);
                    }
                });
                onAfterDeleted(resource);
            }
        });
    }


    /**
     * This method uses reflection by default, using the {@link JSONField} annotation. Override to
     * customize the json output of your Resource.
     */
    public JSONObject toJSON(R item) throws JSONException {
        JSONObject object = new JSONObject();

        for (Map.Entry<String, Field> f : getJSONFields().entrySet()) {
            Field field = f.getValue();
            JSONField jsonField = field.getAnnotation(JSONField.class);
            DatabaseField dbf = f.getValue().getAnnotation(DatabaseField.class);
            try {
                field.setAccessible(true);
                if (jsonField.export()) {
                    Object value;
                    if (field.getType().isAssignableFrom(Date.class)) {
                        //dates
                        if (field.get(item) == null) {
                            value = JSONObject.NULL; //explicit null value
                        } else {
                            value = getDateFormat().format((Date) field.get(item));
                        }
                    } else if (dbf != null && dbf.foreign()) {
                        //foreign fields
                        Resource r = (Resource) field.get(item);
                        if (r != null && r.getId() != null) {
                            value = r.getId();
                        } else {
                            value = JSONObject.NULL; //explicit null value
                        }
                    } else {
                        value = field.get(item);
                    }
                    object.put(f.getKey(), value);
                }
            } catch (Exception e) {
                throw new JSONException(e.getMessage());
            } finally {
                field.setAccessible(false);
            }
        }
        return object;
    }

    protected Map<String, Field> getJSONFields() {
        if (jsonFields == null) {
            jsonFields = new HashMap<>();
            for (Class<?> classWalk = getDataClass(); classWalk != null; classWalk = classWalk.getSuperclass()) {
                for (Field field : classWalk.getDeclaredFields()) {
                    JSONField jsonField = field.getAnnotation(JSONField.class);
                    if (jsonField != null && (jsonField.accept() || jsonField.export())) {
                        String key = jsonField.key();
                        if (key == null || key.isEmpty()) {
                            key = field.getName();
                        }
                        if (jsonFields.get(key) == null) { //subclassed keys override superclass keys
                            jsonFields.put(key, field);
                        }
                    }
                }
            }
        }
        return jsonFields;
    }

    protected Field getIdField(){
        for (Class<?> classWalk = getDataClass(); classWalk != null; classWalk = classWalk.getSuperclass()) {
            for (Field field : classWalk.getDeclaredFields()) {
                DatabaseField databaseField = field.getAnnotation(DatabaseField.class);
                if (databaseField != null && (databaseField.generatedId()||databaseField.id()|| !databaseField.generatedIdSequence().equals(""))) {
                    return field;
                }
            }
        }
        return null;
    }

    protected String getIdFieldKey(){
        Field f = getIdField();
        DatabaseField d = f.getAnnotation(DatabaseField.class);
        if(d != null && !d.columnName().isEmpty()){
            return d.columnName();
        }else{
            return f.getName();
        }
    }

    /**
     * Set values from a JSON object. If you override this method, be sure to mark it as synchronized
     * to make its changes thread-safe. Also DO NOT save the object after populating values, as that
     * is handled by the {@link Source}. Don't set createdAt or updatedAt either.
     *
     * @return true if any of the fields changed, or false if the object is exactly the same as before
     */
    public synchronized boolean updateFromJSON(R item, JSONObject data) throws JSONException {
        boolean updated = false;
        Iterator<String> keys = data.keys();
        while (keys.hasNext()) {
            String key = keys.next();
            Object value = data.get(key);
            Field f = getJSONFields().get(key);
            if (f != null) {
                JSONField jsonField = f.getAnnotation(JSONField.class);
                try {
                    if (value != null && jsonField.accept()) {
                        DatabaseField dbf = f.getAnnotation(DatabaseField.class);
                        f.setAccessible(true);
                        if (f.getType().isAssignableFrom(Date.class)) {
                            try {
                                Date newDate = getDateFormat().parse((String) value);
                                if (newDate != null &&
                                        (f.get(item) == null || ((Date) f.get(item)).getTime() != newDate.getTime())) {
                                    f.set(item, newDate);
                                    updated = true;
                                }
                            } catch (ParseException e) {
                                throw new JSONException(e.getMessage());
                            }
                        } else if (dbf != null && dbf.foreign()) {

                            //TODO how to populate children?
                            Log.w("JSON", "Didn't populate relation " + f.getName());
                        } else {
                            if (f.get(item) == null || !f.get(item).equals(value)) {
                                try {
                                    if (value.equals(JSONObject.NULL)) {
                                        f.set(item, null);
                                    } else {
                                        f.set(item, value);
                                    }
                                    updated = true;
                                } catch (Exception e) {
                                    throw new JSONException(String.format(
                                            "Couldn't figure out how to map '%s' to field '%s' on %s",
                                            String.valueOf(value), key, getClass().toString()));
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    throw new JSONException(e.getMessage());
                } finally {
                    f.setAccessible(false);
                }
            }
        }
        return updated;
    }


    /**
     * The general callback for operations done by {@link Source} in the background. Result could be
     * null if the item could not be found.
     */
    public interface OperationCallback<Q> {
        void onResult(@Nullable Q result);
    }
}
