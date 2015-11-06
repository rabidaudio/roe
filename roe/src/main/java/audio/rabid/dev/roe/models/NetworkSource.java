package audio.rabid.dev.roe.models;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.j256.ormlite.android.apptools.OrmLiteSqliteOpenHelper;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.TableUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.sql.SQLException;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import audio.rabid.dev.roe.BackgroundThread;
import audio.rabid.dev.roe.models.cache.GenericKeyNetworkResourceCache;
import audio.rabid.dev.roe.models.cache.NetworkResourceCache;
import audio.rabid.dev.roe.models.cache.ResourceCache;

/**
 * Created by charles on 11/4/15.
 */
public class NetworkSource<R extends NetworkResource<R, LK, SK>, LK, SK> extends Source<R, LK> {

    private Server server;
    private ResourceFactory<R, SK> resourceFactory;
    private Dao<DeletedResource, Integer> deletedResourceDao;

    private NetworkResourceCache<R, LK, SK> networkResourceCache;

    /**
     * Create a new NetworkSource
     *
     * @param server          the server instance to use for network operations
     * @param dao             the dao instance to use for database operations
     * @param connectionSource the ConnectionSource for the database
     * @param resourceFactory the factory for generating new Resources
     * @param permissions     the operations allowed to be done on the resource
     * @param dateFormat      the formatter used to map dates to json (defaults to unix timestamp)
     */
    public NetworkSource(Server server, Dao<R, LK> dao, ConnectionSource connectionSource, @Nullable ResourceFactory<R, SK> resourceFactory,
                         @Nullable NetworkResourceCache<R, LK, SK> resourceCache,
                         @Nullable PermissionsManager<R> permissions, @Nullable DateFormat dateFormat) {
        super(dao, resourceCache == null ? new GenericKeyNetworkResourceCache<R, LK, SK>(50) : resourceCache, permissions, dateFormat);
        this.server = server;
        if (resourceFactory == null) {
            this.resourceFactory = new GenericResourceFactory<>(getDataClass());
        } else {
            this.resourceFactory = resourceFactory;
        }
        this.networkResourceCache = (NetworkResourceCache<R, LK, SK>) getResourceCache();

        try {
            TableUtils.createTableIfNotExists(connectionSource, DeletedResource.class);
            deletedResourceDao = DaoManager.createDao(connectionSource, DeletedResource.class);
        }catch (SQLException e){
            onSQLException(e);
        }
    }

    public static class Builder<T extends NetworkResource<T, LK, SK>, LK, SK> {
        ConnectionSource connectionSource;
        Dao<T, LK> dao;
        NetworkResourceCache<T, LK, SK> resourceCache;
        PermissionsManager<T> permissionsManager = new SimplePermissionsManager<T>().all();
        DateFormat dateFormat;
        Server server;
        ResourceFactory<T, SK> resourceFactory;

        public Builder() {

        }

        public Builder(OrmLiteSqliteOpenHelper database, Class<T> tClass, Server server) {
            setDatabase(database, tClass);
            setServer(server);
        }

        public Builder(Dao<T, LK> dao, ConnectionSource connectionSource, Server server) {
            setDao(dao);
            setConnectionSource(connectionSource);
            setServer(server);
        }

        public Builder<T, LK, SK> setServer(Server server) {
            this.server = server;
            return this;
        }

        public Builder<T, LK, SK> setResourceFactory(ResourceFactory<T, SK> resourceFactory) {
            this.resourceFactory = resourceFactory;
            return this;
        }


        public Builder<T, LK, SK> setDatabase(@NonNull OrmLiteSqliteOpenHelper database, Class<T> tClass) {
            this.connectionSource = database.getConnectionSource();
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

        public Builder<T, LK, SK> setConnectionSource(@NonNull ConnectionSource connectionSource) {
            this.connectionSource = connectionSource;
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
            if (connectionSource == null || dao == null)
                throw new IllegalArgumentException("Must supply either a Dao and ConnectionSource or a Database instance");
            if(server == null || resourceFactory == null)
                throw new IllegalArgumentException("Must supply a Server and ResourceFactory");

            return new NetworkSource<>(server, dao, connectionSource, resourceFactory, resourceCache, permissionsManager, dateFormat);
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
                    public R onCacheMiss(SK serverId) {
                        try {
                            List<R> items = getDao().queryForEq("serverId", serverId);
                            if (!items.isEmpty()) {
                                return items.get(0);
                            }
                        }catch (SQLException e){
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
                    Server.Response response = server.getItems(getDataClass(), search);
                    if (!server.isErrorResponse(response)) {
                        //got valid network response
                        List<JSONObject> newInstances = resourceFactory.splitMultipleNetworkQuery(response.getResponseBody());
                        for (final JSONObject data : newInstances) {
                            SK serverId = resourceFactory.getServerKeyFromJSON(data);
                            ResourceCache.CacheResult<R> result = networkResourceCache.getByServerId(serverId, new ResourceCache.CacheMissCallback<R, SK>() {
                                @Nullable
                                @Override
                                public R onCacheMiss(SK serverId) {
                                    R newInstance = null;
                                    try {
                                        List<R> results = getDao().queryForEq("serverId", serverId);
                                        if (results.isEmpty()) {
                                            //new item was also not in the database
                                            newInstance = resourceFactory.createFromJSON(data);
                                            if (getPermissions().can(Op.CREATE, newInstance)) {
                                                //create it from data
                                                newInstance.synced = true;
                                                newInstance.createdAt = newInstance.updatedAt = new Date();
                                                getDao().create(newInstance);
                                            }
                                            //otherwise, can't create so oh well
                                        } else {
                                            newInstance = results.get(0);
                                        }
                                    } catch (SQLException e) {
                                        onSQLException(e);
                                    } catch (JSONException e) {
                                        onJSONException(e);
                                    }
                                    return newInstance;
                                }
                            });
                            R item = result.getItem();
                            if (item != null) {
                                if(getPermissions().canUpdate(item)) {
                                    boolean changed;
                                    synchronized (item) {
                                        changed = resourceFactory.updateItemDirect(item, data);
                                        if (changed) {
                                            item.synced = true;
                                            item.updatedAt = new Date();
                                            item.setChanged();
                                            getDao().update(item);
                                        }
                                    }
                                }
                                returnResults.add(item);
                            }
                        }
                    }
                }catch (Server.NetworkException e){
                    onNetworkException(e);
                }catch (JSONException e){
                    onJSONException(e);
                }catch (SQLException e){
                    onSQLException(e);
                }
                BackgroundThread.postMain(new Runnable() {
                    @Override
                    public void run() {
                        if(callback != null) {
                            callback.onResult(returnResults);
                        }
                        for (R r : returnResults) {
                            r.notifyObservers();
                        }
                    }
                });
            }
        });
    }


    public void sync(@Nullable final OperationCallback<Void> callback) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                final List<R> changed = new ArrayList<R>();
                try {
                    List<R> unsynced = getDao().queryForEq("synced", false);
                    for (R u : unsynced) {
                        R item = networkResourceCache.putIfMissing(u);
                        try {
                            Server.Response response;
                            if (item.getServerId() == null && getPermissions().canCreate(item)) {
                                response = server.createItem(getDataClass(), resourceFactory.turnItemIntoValidServerPayload(item));
                            } else if(getPermissions().canUpdate(item)) {
                                response = server.updateItem(getDataClass(), String.valueOf(item.getServerId()), resourceFactory.turnItemIntoValidServerPayload(item));
                            } else {
                                continue;
                            }
                            if (!server.isErrorResponse(response)) {
                                synchronized (item) {
                                    resourceFactory.updateItem(item, response.getResponseBody());
                                    item.synced = true;
                                    item.updatedAt = new Date();
                                    item.setChanged();
                                    changed.add(item);
                                    getDao().update(item);
                                }
                            }
                        } catch (Server.NetworkException e) {
                            //oh well, still no network
                            onNetworkException(e);
                        }
                    }
                    for (DeletedResource deleted : deletedResourceDao.queryForAll()) {
                        try {
                            Server.Response response = deleted.attemptDelete(server);
                            if (!server.isErrorResponse(response)) {
                                deletedResourceDao.delete(deleted);
                            }
                        } catch (Server.NetworkException e) {
                            onNetworkException(e);
                        }
                    }
                } catch (SQLException e) {
                    onSQLException(e);
                } catch (JSONException e) {
                    onJSONException(e);
                }
                BackgroundThread.postMain(new Runnable() {
                    @Override
                    public void run() {
                        if (callback != null) {
                            callback.onResult(null);
                        }
                        for (R c : changed) {
                            c.notifyObservers();
                        }
                    }
                });
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
                        Server.Response response = server.getItem(getDao().getDataClass(), String.valueOf(resource.getServerId()));
                        if (!server.isErrorResponse(response)) {
                            try {
                                boolean changed;
                                synchronized (resource) {
                                    changed = resourceFactory.updateItem(resource, response.getResponseBody());
                                    if (changed) {
                                        resource.synced = true;
                                        resource.updatedAt = new Date();
                                        resource.setChanged();
                                    }
                                }
                                if (changed) {
                                    getDao().update(resource); //save changes to database
                                }
                            } catch (JSONException e) {
                                onJSONException(e);
                            } catch (SQLException e) {
                                onSQLException(e);
                            }
                        }
                    } catch (Server.NetworkException e) {
                        //just put in cache as it is
                        onNetworkException(e);
                    }

                    if(resource.hasChanged()) {
                        BackgroundThread.postMain(new Runnable() {
                            @Override
                            public void run() {
                                resource.notifyObservers();
                            }
                        });
                    }
                }
            }, "NetworkRead:" + resource.getClass().getName() + ":" + resource.hashCode()).start();
        }
    }

    @Override
    protected void onBeforeCreated(R created) {
        super.onBeforeCreated(created);
        created.synced = false;
    }

    @Override
    protected void onAfterCreated(final R resource) {
        super.onAfterCreated(resource);
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Server.Response response = server.createItem(getDataClass(), resourceFactory.turnItemIntoValidServerPayload(resource));
                    if (!server.isErrorResponse(response) && getPermissions().canUpdate(resource)) {
                        synchronized (resource) {
                            boolean changed = resourceFactory.updateItem(resource, response.getResponseBody());
                            if (changed) {
                                resource.updatedAt = new Date();
                                resource.synced = true;
                                getDao().update(resource);
                                resource.setChanged();
                            }
                        }
                    }
                } catch (Server.NetworkException e) {
                    //oh well, item stays unsynced
                    onNetworkException(e);
                } catch (JSONException e){
                    onJSONException(e);
                } catch (SQLException e){
                    onSQLException(e);
                }
                if(resource.hasChanged()) {
                    BackgroundThread.postMain(new Runnable() {
                        @Override
                        public void run() {
                            resource.notifyObservers();
                        }
                    });
                }
            }
        }, "NetworkCreate:" + resource.getClass().getName() + ":" + resource.hashCode()).start();
    }

    @Override
    protected void onBeforeUpdated(R updated) {
        super.onBeforeDeleted(updated);
        updated.synced = false;
    }

    @Override
    protected void onAfterUpdated(final R resource) {
        super.onAfterUpdated(resource);
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Server.Response response = null;
                    try {
                        if (resource.getServerId() == null) {
                            if (getPermissions().canCreate(resource)) {
                                response = server.createItem(getDataClass(), resourceFactory.turnItemIntoValidServerPayload(resource));
                            }
                        } else {
                            if (getPermissions().canUpdate(resource)) {
                                response = server.updateItem(getDataClass(), String.valueOf(resource.getServerId()), resourceFactory.turnItemIntoValidServerPayload(resource));
                            }
                        }
                    } catch (Server.NetworkException e) {
                        //oh well, try sync again later
                        onNetworkException(e);
                        return;
                    }
                    synchronized (resource) {
                        if (response != null && !server.isErrorResponse(response)) {
                            resourceFactory.updateItem(resource, response.getResponseBody());
                            resource.synced = true;
                        }
                        resource.updatedAt = new Date();
                        getDao().update(resource);
                        resource.setChanged();
                    }
                }catch (JSONException e){
                    onJSONException(e);
                } catch (SQLException e){
                    onSQLException(e);
                }
                if(resource.hasChanged()){
                    BackgroundThread.postMain(new Runnable() {
                        @Override
                        public void run() {
                            resource.notifyObservers();
                        }
                    });
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
                        try {
                            Server.Response response = server.deleteItem(getDataClass(), String.valueOf(resource.getServerId()));
                            if (!server.isErrorResponse(response)) {
                                return; //all good
                            }
                        } catch (Server.NetworkException e) {
                            onNetworkException(e);
                        }
                        deletedResourceDao.create(new DeletedResource(resource));
                    } catch (SQLException e) {
                        onSQLException(e);
                    }
                }
            }, "NetworkDelete:" + resource.getClass().getName() + ":" + resource.hashCode()).start();
        }
    }
}
