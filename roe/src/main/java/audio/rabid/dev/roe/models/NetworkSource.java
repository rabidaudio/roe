package audio.rabid.dev.roe.models;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.j256.ormlite.android.apptools.OrmLiteSqliteOpenHelper;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.TableUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Field;
import java.sql.SQLException;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import audio.rabid.dev.roe.BackgroundThread;
import audio.rabid.dev.roe.models.cache.MapNetworkResourceCache;
import audio.rabid.dev.roe.models.cache.NetworkResourceCache;
import audio.rabid.dev.roe.models.cache.ResourceCache;

/**
 * Created by charles on 11/4/15.
 */
public class NetworkSource<R extends NetworkResource<LK, SK>, LK, SK> extends Source<R, LK> {

    private Server server;
    private Dao<DeletedResource, Integer> deletedResourceDao;
    private Dao<UnsyncedResource, Integer> unsyncedResourceDao;

    private NetworkResourceCache<R, LK, SK> networkResourceCache;

    /**
     * Create a new NetworkSource
     *
     * @param server          the server instance to use for network operations
     * @param dao             the dao instance to use for database operations
     * @param permissions     the operations allowed to be done on the resource
     * @param dateFormat      the formatter used to map dates to json (defaults to unix timestamp)
     */
    public NetworkSource(Server server, Dao<R, LK> dao,
                         @Nullable NetworkResourceCache<R, LK, SK> resourceCache,
                         @Nullable PermissionsManager<R> permissions, @Nullable DateFormat dateFormat) {
        super(dao, resourceCache == null ? new MapNetworkResourceCache<R, LK, SK>(50) : resourceCache, permissions, dateFormat);
        this.server = server;
        this.networkResourceCache = (NetworkResourceCache<R, LK, SK>) getResourceCache();

        try {
            TableUtils.createTableIfNotExists(dao.getConnectionSource(), DeletedResource.class);
            deletedResourceDao = DaoManager.createDao(dao.getConnectionSource(), DeletedResource.class);

            TableUtils.createTableIfNotExists(dao.getConnectionSource(), UnsyncedResource.class);
            unsyncedResourceDao = DaoManager.createDao(dao.getConnectionSource(), UnsyncedResource.class);
        }catch (SQLException e){
            onSQLException(e);
        }
    }

    public static class Builder<T extends NetworkResource<LK, SK>, LK, SK> {
        Dao<T, LK> dao;
        NetworkResourceCache<T, LK, SK> resourceCache;
        PermissionsManager<T> permissionsManager = new SimplePermissionsManager<T>().all();
        DateFormat dateFormat;
        Server server;

        public Builder() {

        }

        public Builder(OrmLiteSqliteOpenHelper database, Class<T> tClass, Server server) {
            setDatabase(database, tClass);
            setServer(server);
        }

        public Builder(Dao<T, LK> dao, Server server) {
            setDao(dao);
            setServer(server);
        }

        public Builder<T, LK, SK> setServer(Server server) {
            this.server = server;
            return this;
        }

        public Builder<T, LK, SK> setDatabase(@NonNull OrmLiteSqliteOpenHelper database, Class<T> tClass) {
            try {
                this.dao = database.getDao(tClass);
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
            return this;
        }

        public Builder<T, LK, SK> setDao(@NonNull Dao<T, LK> dao) {
            this.dao = dao;
            return this;
        }

        public Builder<T, LK, SK> setPermissionsManager(PermissionsManager<T> permissionsManager) {
            this.permissionsManager = permissionsManager;
            return this;
        }

        public Builder<T, LK, SK> setPermissions(Op... allowedOps) {
            this.permissionsManager = new SimplePermissionsManager<>(allowedOps);
            return this;
        }

        public Builder<T, LK, SK> setResourceCache(NetworkResourceCache<T, LK, SK> resourceCache) {
            this.resourceCache = resourceCache;
            return this;
        }

        public Builder<T, LK, SK> setDateFormat(DateFormat dateFormat) {
            this.dateFormat = dateFormat;
            return this;
        }

        public NetworkSource<T, LK, SK> build() {
            if (dao == null)
                throw new IllegalArgumentException("Must supply either a Dao or a Database instance");
            if (server == null)
                throw new IllegalArgumentException("Must supply a Server");

            return new NetworkSource<>(server, dao, resourceCache, permissionsManager, dateFormat);
        }
    }

    @Override
    protected void onBeforeCreated(R created) {
        super.onBeforeCreated(created);
        if (created instanceof IntegerKeyedNetworkResource) {
            ((IntegerKeyedNetworkResource) created).createdAt = new Date();
            ((IntegerKeyedNetworkResource) created).updatedAt = new Date();
        }
    }

    protected void onBeforeUpdated(R updated) {
        super.onBeforeUpdated(updated);
        if (updated instanceof IntegerKeyedNetworkResource) {
            ((IntegerKeyedNetworkResource) updated).updatedAt = new Date();
        }
    }

    /**
     * Get a resource by it's server id. Method will try the cache, otherwise resorting to the database
     * and checking for updates on the network if possible.
     */
    public void findByServerId(final @NonNull SK serverId, final @NonNull OperationCallback<R> callback) {
        BackgroundThread.postBackground(new Runnable() {
            @Override
            public void run() {
                ResourceCache.CacheResult<R> result = networkResourceCache.getByServerId(serverId, new ResourceCache.CacheMissCallback<R, SK>() {
                    @Nullable
                    @Override
                    public R onCacheMiss(SK id) {
                        try {
                            List<R> items = getDao().queryForEq(getServerIdFieldKey(), serverId);

                            if (!items.isEmpty()) {
                                return items.get(0);
                            }
                        } catch (SQLException e) {
                            onSQLException(e);
                        }
                        return null;
                    }
                });
                final R item = getPermissions().can(Op.READ, result.getItem()) ? result.getItem() : null;
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
     * Download a number of resources from the network all at once, creating or updating the local
     * versions as necessary. Unlike other operations, this method HAS to hit the network, so it
     * could fail with no data if network is unavailable. It is also significantly slower than the
     * other methods. It should be used to populate initial data from the server, but not to populate
     * every screen in your app.
     * @param search the query parameters to send to the network
     * @param callback where to send the results when complete
     */
    public void getManyFromNetwork(@Nullable final JSONObject search, @Nullable final OperationCallback<List<R>> callback) {
        BackgroundThread.postBackground(new Runnable() {
            @Override
            public void run() {
                final List<R> returnResults = new ArrayList<>();
                try {

                    List<JSONObject> newInstances = server.getItems(getDataClass(), search);
                    for(JSONObject data : newInstances){
                        final R tempInstance = getDao().getDataClass().newInstance();
                        updateFromJSON(tempInstance, data);
                        ResourceCache.CacheResult<R> result = networkResourceCache.getByServerId(tempInstance.getServerId(), new ResourceCache.CacheMissCallback<R, SK>() {
                            @Nullable
                            @Override
                            public R onCacheMiss(SK serverId) {
                                try {
                                    List<R> results = getDao().queryForEq(getServerIdFieldKey(), serverId);
                                    if (results.isEmpty()) {
                                        //new item was also not in the database
                                        if (getPermissions().can(Op.CREATE, tempInstance)) {
                                            //create it from data
                                            onBeforeCreated(tempInstance);
                                            getDao().create(tempInstance);
                                            return tempInstance;
                                        } //otherwise, can't create so oh well
                                    } else {
                                        return results.get(0);
                                    }
                                } catch (SQLException e) {
                                    onSQLException(e);
                                }
                                return null;
                            }
                        });
                        R resource = result.getItem();
                        if (resource != null) {
                            if (getPermissions().canUpdate(resource)) {
                                boolean changed;
                                synchronized (resource) {
                                    changed = updateFromJSON(resource, data);
                                    if (changed) {
                                        onBeforeCreated(resource);
                                        getDao().update(resource);
                                    }
                                }
                            }
                            returnResults.add(resource);
                        }
                    }

                } catch (Server.NetworkException e) {
                    onNetworkException(e);
                } catch (SQLException e) {
                    onSQLException(e);
                } catch (JSONException e) {
                    onJSONException(e);
                } catch (IllegalAccessException e) {
                    throw new RuntimeException("Couldn't make new Resource " + getDataClass().getName(), e);
                } catch (InstantiationException e) {
                    throw new RuntimeException("Couldn't make new Resource " + getDataClass().getName(), e);
                }
                BackgroundThread.postMain(new Runnable() {
                    @Override
                    public void run() {
                        if (callback != null) {
                            callback.onResult(returnResults);
                        }
                        for (R r : returnResults) {
                            notifyObservers(r, false);
                        }
                    }
                });
            }
        });
    }


    public void sync(@Nullable final OperationCallback<List<R>> callback) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                List<R> synced = new ArrayList<>();
                try {
                    //updates
                    List<UnsyncedResource> unsyncedResources = UnsyncedResource.getUnsynced(getDataClass(), unsyncedResourceDao);
                    for (UnsyncedResource unsyncedResource : unsyncedResources) {
                        LK localId = getDataClass().newInstance().localIdFromString(unsyncedResource.getLocalId()); //TODO haxxxxxx
                        ResourceCache.CacheResult<R> result = networkResourceCache.get(localId, new ResourceCache.CacheMissCallback<R, LK>() {
                            @Nullable
                            @Override
                            public R onCacheMiss(LK localId) {
                                try {
                                    return getDao().queryForId(localId);
                                } catch (SQLException e) {
                                    onSQLException(e);
                                }
                                return null;
                            }
                        });
                        if (result.getItem() == null || !getPermissions().canRead(result.getItem())) {
                            unsyncedResourceDao.delete(unsyncedResource);
                            continue;
                        }
                        final R resource = result.getItem();
                        JSONObject data;
                        try {
                            if (unsyncedResource.needsCreate() && getPermissions().canCreate(resource)) {
                                data = server.createItem(getDataClass(), toJSON(resource));
                            } else if (!unsyncedResource.needsCreate() && getPermissions().canUpdate(resource)) {
                                data = server.updateItem(getDataClass(), String.valueOf(resource.getServerId()), toJSON(resource));
                            } else {
                                //no permissions, so don't bother
                                unsyncedResourceDao.delete(unsyncedResource);
                                continue;
                            }
                            boolean changed = updateFromJSON(resource, data);
                            if (changed) {
                                onBeforeUpdated(resource);
                                getDao().update(resource);
                                getObservable(resource).setChanged();
                                synced.add(resource);
                                BackgroundThread.postMain(new Runnable() {
                                    @Override
                                    public void run() {
                                        notifyObservers(resource, false);
                                    }
                                });
                            }
                            unsyncedResourceDao.delete(unsyncedResource); //done now
                        } catch (Server.NetworkException e) {
                            //oh well, still no network
                            onNetworkException(e);
                        }
                    }

                    //deletes
                    for (DeletedResource deletedResource : DeletedResource.getDeletedResources(deletedResourceDao, getDataClass())) {
                        boolean deleted = deletedResource.attemptDelete(server);
                        if (deleted) {
                            deletedResourceDao.delete(deletedResource); //done
                        }
                    }
                } catch (SQLException e) {
                    onSQLException(e);
                } catch (JSONException e) {
                    onJSONException(e);
                } catch (IllegalAccessException e) {
                    throw new RuntimeException("Couldn't make new Resource " + getDataClass().getName(), e);
                } catch (InstantiationException e) {
                    throw new RuntimeException("Couldn't make new Resource " + getDataClass().getName(), e);
                }

                if (callback != null) {
                    callback.onResult(synced);
                }
            }
        }, "NetworkSync:"+getClass().getName()).start();
    }

    /**
     * Override this method to catch {@link JSONException}s thrown while trying to convert {@link Resource}s to json.
     * The default implementation is to throw a {@link RuntimeException}.
     */
    public void onJSONException(JSONException e){
        throw new RuntimeException(e);
    }

    /**
     * Override this method to catch {@link Server.NetworkException}s thrown while doing network
     * transactions (e.g. for logging). The default implementation is to do nothing.
     */
    public void onNetworkException(Server.NetworkException e){
        //default: no-op
    }

    @Override
    protected void onAfterCacheAdd(final R resource) {
        super.onAfterCreated(resource);
        if (resource.getServerId() != null && getPermissions().canUpdate(resource)) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    //has a server id, so see if network has update
                    try {
                        JSONObject data = server.getItem(getDataClass(), String.valueOf(resource.getServerId()));
                        synchronized (resource) {
                            boolean changed = updateFromJSON(resource, data);
                            onBeforeUpdated(resource);
                            getObservable(resource).setChanged();
                            if (changed) {
                                getDao().update(resource);
                            }
                            BackgroundThread.postMain(new Runnable() {
                                @Override
                                public void run() {
                                    notifyObservers(resource, false);
                                }
                            });
                        }
                    } catch (Server.NetworkException e) {
                        onNetworkException(e);
                    } catch (SQLException e) {
                        onSQLException(e);
                    } catch (JSONException e) {
                        onJSONException(e);
                    }
                }
            }, "NetworkRead:" + resource.getClass().getName() + ":" + resource.hashCode()).start();
        }
    }

    @Override
    protected void onAfterCreated(final R resource) {
        super.onAfterCreated(resource);
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    JSONObject data = server.createItem(getDataClass(), toJSON(resource));
                    synchronized (resource) {
                        boolean changed = updateFromJSON(resource, data);
                        if (changed) {
                            onBeforeUpdated(resource);
                            getDao().update(resource);
                            getObservable(resource).setChanged();
                            BackgroundThread.postMain(new Runnable() {
                                @Override
                                public void run() {
                                    notifyObservers(resource, false);
                                }
                            });
                        }
                    }
                } catch (Server.NetworkException e) {
                    try {
                        UnsyncedResource.createIfNeeded(unsyncedResourceDao, resource, true);
                    } catch (SQLException ex) {
                        onSQLException(ex);
                    }
                    onNetworkException(e);
                } catch (JSONException e){
                    onJSONException(e);
                } catch (SQLException e){
                    onSQLException(e);
                }
            }
        }, "NetworkCreate:" + resource.getClass().getName() + ":" + resource.hashCode()).start();
    }

    @Override
    protected void onAfterUpdated(final R resource) {
        super.onAfterUpdated(resource);
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    JSONObject data;
                    if (resource.getServerId() == null && getPermissions().canCreate(resource)) {
                        data = server.createItem(getDataClass(), toJSON(resource));
                    } else if (resource.getServerId() != null && getPermissions().canUpdate(resource)) {
                        data = server.updateItem(getDataClass(), String.valueOf(resource.getServerId()), toJSON(resource));
                    } else {
                        //NO permissions!
                        return;
                    }
                    synchronized (resource) {
                        boolean changed = updateFromJSON(resource, data);
                        if (changed) {
                            onBeforeUpdated(resource);
                            getDao().update(resource);
                            getObservable(resource).setChanged();
                            BackgroundThread.postMain(new Runnable() {
                                @Override
                                public void run() {
                                    notifyObservers(resource, false);
                                }
                            });
                        }
                    }
                } catch (Server.NetworkException e) {
                    try {
                        UnsyncedResource.createIfNeeded(unsyncedResourceDao, resource, false);
                    } catch (SQLException ex) {
                        onSQLException(ex);
                    }
                    onNetworkException(e);
                } catch (JSONException e) {
                    onJSONException(e);
                } catch (SQLException e){
                    onSQLException(e);
                }
            }
        }, "NetworkUpdate:" + resource.getClass().getName() + ":" + resource.hashCode()).start();
    }

    @Override
    protected void onAfterDeleted(final R resource) {
        super.onAfterDeleted(resource);
        if (resource.getServerId() != null) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        JSONObject data = server.deleteItem(getDataClass(), String.valueOf(resource.getServerId()));
                        synchronized (resource) {
                            boolean changed = updateFromJSON(resource, data);
                            if (changed) {
                                onBeforeUpdated(resource);
                                getObservable(resource).setChanged();
                                BackgroundThread.postMain(new Runnable() {
                                    @Override
                                    public void run() {
                                        notifyObservers(resource, true);
                                    }
                                });
                            }
                        }
                    } catch (Server.NetworkException e) {
                        try {
                            deletedResourceDao.create(new DeletedResource(resource));
                            onNetworkException(e);
                        } catch (SQLException ex) {
                            onSQLException(ex);
                        }
                    } catch (JSONException e) {
                        onJSONException(e);
                    }
                }
            }, "NetworkDelete:" + resource.getClass().getName() + ":" + resource.hashCode()).start();
        }
    }

    private Field serverIdField = null;
    private Field getServerIdField(){
        if(serverIdField==null) {
            for (Class<?> classWalk = getDataClass(); classWalk != null; classWalk = classWalk.getSuperclass()) {
                for (Field field : classWalk.getDeclaredFields()) {
                    NetworkId networkId = field.getAnnotation(NetworkId.class);
                    if (networkId != null) {
                        serverIdField = field;
                        break;
                    }
                }
            }
        }
        return serverIdField;
    }

    private String getServerIdFieldKey(){
        Field f = getServerIdField();
        DatabaseField d = f.getAnnotation(DatabaseField.class);
        if(d != null && !d.columnName().isEmpty()){
            return d.columnName();
        }else{
            return f.getName();
        }
    }
}