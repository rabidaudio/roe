package audio.rabid.dev.roe.models;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.j256.ormlite.android.apptools.OrmLiteSqliteOpenHelper;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.support.ConnectionSource;

import java.sql.SQLException;
import java.text.DateFormat;
import java.text.FieldPosition;
import java.text.ParsePosition;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import audio.rabid.dev.roe.BackgroundThread;
import audio.rabid.dev.roe.models.cache.ResourceCache;
import audio.rabid.dev.roe.models.cache.SparseArrayResourceCache;
import audio.rabid.dev.roe.models.rails.Op;

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
public class Source<T extends Resource> {

    private Dao<T, Integer> dao;
    private ResourceCache<T> resourceCache;
    private PermissionsManager<T> permissions;
    private DateFormat dateFormat;

    /**
     * Create a new Source
     *
     * @param dao             the dao instance to use for database operations
     * @param connectionSource the ConnectionSource for the database
     * @param resourceCache   the cache to use for keeping instances consistent
     * @param permissions     the operations allowed to be done on the resource
     * @param dateFormat      the formatter used to map dates to json (defaults to unix timestamp)
     */
    public Source(@NonNull Dao<T, Integer> dao,
                  @NonNull ConnectionSource connectionSource,
                  @Nullable ResourceCache<T> resourceCache,
                  @Nullable PermissionsManager<T> permissions,
                  @Nullable DateFormat dateFormat) {
        this.dao = dao;
        if (resourceCache == null) {
            this.resourceCache = new SparseArrayResourceCache<>(50);
        } else {
            this.resourceCache = resourceCache;
        }
        if(permissions == null){
            this.permissions = new SimplePermissionsManager<T>().all();
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

    public static class Builder<T extends Resource> {
        ConnectionSource connectionSource;
        Dao<T, Integer> dao;
        ResourceCache<T> resourceCache;
        PermissionsManager<T> permissionsManager = new SimplePermissionsManager<T>().all();
        DateFormat dateFormat;

        public Builder() {

        }

        public Builder(OrmLiteSqliteOpenHelper database, Class<T> tClass) {
            setDatabase(database, tClass);
        }

        public Builder(Dao<T, Integer> dao, ConnectionSource connectionSource) {
            setDao(dao);
            setConnectionSource(connectionSource);
        }

        public Builder<T> setDatabase(@NonNull OrmLiteSqliteOpenHelper database, Class<T> tClass) {
            this.connectionSource = database.getConnectionSource();
            try {
                this.dao = database.getDao(tClass);
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
            return this;
        }

        public Builder<T> setDao(@NonNull Dao<T, Integer> dao) {
            this.dao = dao;
            return this;
        }

        public Builder<T> setConnectionSource(@NonNull ConnectionSource connectionSource) {
            this.connectionSource = connectionSource;
            return this;
        }

        public Builder<T> setPermissionsManager(PermissionsManager<T> permissionsManager) {
            this.permissionsManager = permissionsManager;
            return this;
        }

        public Builder<T> setPermissions(Op... allowedOps) {
            this.permissionsManager = new SimplePermissionsManager<>(allowedOps);
            return this;
        }

        public Builder<T> setResourceCache(ResourceCache<T> resourceCache) {
            this.resourceCache = resourceCache;
            return this;
        }

        public Builder<T> setDateFormat(DateFormat dateFormat){
            this.dateFormat = dateFormat;
            return this;
        }

        public Source<T> build() {
            if (connectionSource == null || dao == null)
                throw new IllegalArgumentException("Must supply either a Dao and ConnectionSource or a Database instance");
            return new Source<>(dao, connectionSource, resourceCache, permissionsManager, dateFormat);
        }
    }


    /**
     * Return the {@link Dao} used for database operations. You shouldn't need to access this, as
     * using the <code>doOperation</code> methods should give you the instance to use.
     */
    protected Dao<T, Integer> getDao() {
        return dao;
    }

    protected ResourceCache<T> getResourceCache(){
        return resourceCache;
    }

    /**
     * Get the {@link PermissionsManager} for determining which CRUD operations are allowed on a
     * {@link Resource}.
     */
    public PermissionsManager<T> getPermissions() {
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

    public Class<T> getDataClass(){
        return dao.getDataClass();
    }


    protected void onAfterCacheAdd(T added){

    }

    protected void onBeforeCreated(T created){

    }

    protected void onAfterCreated(T created){

    }

    protected void onBeforeUpdated(T updated){

    }

    protected void onAfterUpdated(T updated){

    }

    protected void onBeforeDeleted(T deleted){

    }

    protected void onAfterDeleted(T deleted){

    }

    /**
     * Get a resource by it's local id. Method will try the cache, otherwise resorting to the database
     * and checking for updates on the network if possible.
     */
    public void find(final @NonNull Integer localId, @NonNull final OperationCallback<T> callback) {
        BackgroundThread.postBackground(new Runnable() {
            @Override
            public void run() {
                ResourceCache.CacheResult<T> result = resourceCache.get(localId, new ResourceCache.CacheMissCallback<T>() {
                    @Nullable
                    @Override
                    public T onCacheMiss(int id) {
                        try {
                            return dao.queryForId(localId);
                        } catch (SQLException e) {
                            onSQLException(e);
                            return null;
                        }
                    }
                });
                final T item = permissions.can(Op.READ, result.getItem()) ? result.getItem() : null;
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
    public void getAllLocal(@NonNull final OperationCallback<List<T>> callback) {
        executeLocalQuery(new LocalQuery<T>() {
            @Override
            public List<T> executeQuery(Dao<T, Integer> dao) throws SQLException {
                return dao.queryForAll();
            }
        }, callback);
    }

    public void executeLocalQuery(final LocalQuery<T> localQuery, final  @NonNull OperationCallback<List<T>> callback){
        BackgroundThread.postBackground(new Runnable() {
            @Override
            public void run() {
                final List<T> returnResults = new ArrayList<>();
                final List<T> unCached = new ArrayList<>();
                try {
                    List<T> results = localQuery.executeQuery(dao);
                    for (final T result : results) {
                        if (permissions.can(Op.READ, result)) {
                            returnResults.add(resourceCache.get(result.getId(), new ResourceCache.CacheMissCallback<T>() {
                                @Nullable
                                @Override
                                public T onCacheMiss(int id) {
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
                for (T u : unCached) {
                    onAfterCacheAdd(u);
                }
            }
        });
    }

    protected interface LocalQuery<T> {
        List<T> executeQuery(Dao<T, Integer> dao) throws SQLException;
    }

    public void create(final T resource, @Nullable final OperationCallback<T> callback) {
        if (!resource.isNew()) {
            throw new RuntimeException("Can't create a resource that has already been saved. Use update instead");
        }
        getPermissions().checkPermission(Op.CREATE, resource);
        BackgroundThread.postBackground(new Runnable() {
            @Override
            public void run() {
                try {
                    synchronized (resource) {
                        resource.createdAt = resource.updatedAt = new Date();
                        onBeforeCreated(resource);
                        dao.create(resource);
                        resource.setChanged();
                    }
                    resourceCache.put(resource);
                } catch (SQLException e) {
                    onSQLException(e);
                }
                if(callback!=null) {
                    BackgroundThread.postMain(new Runnable() {
                        @Override
                        public void run() {
                            callback.onResult(resource);
                        }
                    });
                }
                onAfterCreated(resource);
                BackgroundThread.postMain(new Runnable() {
                    @Override
                    public void run() {
                        resource.notifyObservers();
                    }
                });
            }
        });
    }

    public void update(final T resource, final @Nullable OperationCallback<T> callback) {
        if (resource.isNew()) {
            throw new RuntimeException("Can't update a resource that hasn't been created yet. Use create instead");
        }
        getPermissions().checkPermission(Op.UPDATE, resource);
        BackgroundThread.postBackground(new Runnable() {
            @Override
            public void run() {
                resourceCache.get(resource.getId(), new ResourceCache.CacheMissCallback<T>() {
                    @Nullable
                    @Override
                    public T onCacheMiss(int id) {
                        throw new RuntimeException("Tried to update a resource missing from the cache");
                    }
                });
                try {
                    synchronized (resource) {
                        resource.updatedAt = new Date();
                        onBeforeUpdated(resource);
                        dao.update(resource);
                        resource.setChanged();
                    }
                } catch (SQLException e) {
                    onSQLException(e);
                }
                if (callback != null) {
                    BackgroundThread.postMain(new Runnable() {
                        @Override
                        public void run() {
                            callback.onResult(resource);
                        }
                    });
                }
                onAfterUpdated(resource);
                BackgroundThread.postMain(new Runnable() {
                    @Override
                    public void run() {
                        resource.notifyObservers();
                    }
                });
            }
        });
    }

    public void createOrUpdate(T resource, @Nullable OperationCallback<T> callback) {
        if (resource.isNew()) {
            create(resource, callback);
        } else {
            update(resource, callback);
        }
    }

    public void delete(final T resource, @Nullable final OperationCallback<T> callback) {
        getPermissions().checkPermission(Op.DELETE, resource);
        BackgroundThread.postBackground(new Runnable() {
            @Override
            public void run() {
                try {
                    onBeforeDeleted(resource);
                    dao.delete(resource);
                    resourceCache.delete(resource);
                    synchronized (resource) {
                        resource.deleted = true;
                        resource.setChanged();
                    }
                } catch (SQLException e) {
                    onSQLException(e);
                }
                if (callback != null) {
                    BackgroundThread.postMain(new Runnable() {
                        @Override
                        public void run() {
                            callback.onResult(resource);
                        }
                    });
                }
                onAfterDeleted(resource);
                BackgroundThread.postMain(new Runnable() {
                    @Override
                    public void run() {
                        resource.notifyObservers();
                    }
                });
            }
        });
    }

    /**
     * The general callback for operations done by {@link Source} in the background. Result could be
     * null if the item could not be found.
     */
    public interface OperationCallback<Q> {
        void onResult(@Nullable Q result);
    }
}
