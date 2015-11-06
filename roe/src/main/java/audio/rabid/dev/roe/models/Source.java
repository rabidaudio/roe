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
import audio.rabid.dev.roe.models.cache.GenericKeyResourceCache;
import audio.rabid.dev.roe.models.cache.ResourceCache;

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
public class Source<R extends Resource<R, LK>, LK> {

    private Dao<R, LK> dao;
    private ResourceCache<R, LK> resourceCache;
    private PermissionsManager<R> permissions;
    private DateFormat dateFormat;

    /**
     * Create a new Source
     *
     * @param dao             the dao instance to use for database operations
     * @param resourceCache   the cache to use for keeping instances consistent
     * @param permissions     the operations allowed to be done on the resource
     * @param dateFormat      the formatter used to map dates to json (defaults to unix timestamp)
     */
    public Source(@NonNull Dao<R, LK> dao,
                  @Nullable ResourceCache<R, LK> resourceCache,
                  @Nullable PermissionsManager<R> permissions,
                  @Nullable DateFormat dateFormat) {
        this.dao = dao;
        if (resourceCache == null) {
            this.resourceCache = new GenericKeyResourceCache<>(50);
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

    public static class Builder<T extends Resource<T, K>, K> {
        Dao<T, K> dao;
        ResourceCache<T, K> resourceCache;
        PermissionsManager<T> permissionsManager = new SimplePermissionsManager<T>().all();
        DateFormat dateFormat;

        public Builder() {

        }

        public Builder(OrmLiteSqliteOpenHelper database, Class<T> tClass) {
            setDatabase(database, tClass);
        }

        public Builder(Dao<T, K> dao) {
            setDao(dao);
        }

        public Builder<T, K> setDatabase(@NonNull OrmLiteSqliteOpenHelper database, Class<T> tClass) {
            try {
                this.dao = database.getDao(tClass);
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
            return this;
        }

        public Builder<T, K> setDao(@NonNull Dao<T, K> dao) {
            this.dao = dao;
            return this;
        }

        public Builder<T, K> setPermissionsManager(PermissionsManager<T> permissionsManager) {
            this.permissionsManager = permissionsManager;
            return this;
        }

        public Builder<T, K> setPermissions(Op... allowedOps) {
            this.permissionsManager = new SimplePermissionsManager<>(allowedOps);
            return this;
        }

        public Builder<T, K> setResourceCache(ResourceCache<T, K> resourceCache) {
            this.resourceCache = resourceCache;
            return this;
        }

        public Builder<T, K> setDateFormat(DateFormat dateFormat) {
            this.dateFormat = dateFormat;
            return this;
        }

        public Source<T, K> build() {
            if (dao == null)
                throw new IllegalArgumentException("Must supply either a Dao or a Database instance");
            return new Source<>(dao, resourceCache, permissionsManager, dateFormat);
        }
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
        return dao.getDataClass();
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
                            resource.notifyObservers();
                        }
                    });
                }
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
                            resource.notifyObservers();
                        }
                    });
                }
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
                            resource.notifyObservers();
                        }
                    });
                }
                onAfterDeleted(resource);
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
