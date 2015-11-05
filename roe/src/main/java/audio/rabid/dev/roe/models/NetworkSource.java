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
import audio.rabid.dev.roe.models.cache.NetworkResourceCache;
import audio.rabid.dev.roe.models.cache.ResourceCache;
import audio.rabid.dev.roe.models.cache.SparseArrayNetworkResourceCache;
import audio.rabid.dev.roe.models.rails.Op;

/**
 * Created by charles on 11/4/15.
 */
public class NetworkSource<T extends NetworkResource> extends Source<T> {

    private Server server;
    private ResourceFactory<T> resourceFactory;
    private Dao<DeletedResource, Integer> deletedResourceDao;

    private NetworkResourceCache<T> networkResourceCache;

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
    public NetworkSource(Server server, Dao<T, Integer> dao, ConnectionSource connectionSource, ResourceFactory<T> resourceFactory,
                         @NonNull NetworkResourceCache<T> resourceCache,
                  @Nullable PermissionsManager<T> permissions, @Nullable DateFormat dateFormat) {
        super(dao, connectionSource, resourceCache, permissions, dateFormat);
        this.server = server;
        this.resourceFactory = resourceFactory;

        this.networkResourceCache = resourceCache;

        try {
            TableUtils.createTableIfNotExists(connectionSource, DeletedResource.class);
            deletedResourceDao = DaoManager.createDao(connectionSource, DeletedResource.class);
        }catch (SQLException e){
            onSQLException(e);
        }
    }
    public static class Builder<T extends NetworkResource> {
        ConnectionSource connectionSource;
        Dao<T, Integer> dao;
        NetworkResourceCache<T> resourceCache;
        PermissionsManager<T> permissionsManager = new SimplePermissionsManager<T>().all();
        DateFormat dateFormat;
        Server server;
        ResourceFactory<T> resourceFactory;

        public Builder() {

        }

        public Builder(OrmLiteSqliteOpenHelper database, Class<T> tClass, Server server, ResourceFactory<T> resourceFactory) {
            setDatabase(database, tClass);
            setServer(server, resourceFactory);
        }

        public Builder(Dao<T, Integer> dao, ConnectionSource connectionSource, Server server, ResourceFactory<T> resourceFactory) {
            setDao(dao);
            setConnectionSource(connectionSource);
            setServer(server, resourceFactory);
        }

        public Builder<T> setServer(Server server, ResourceFactory<T> resourceFactory){
            this.server = server;
            this.resourceFactory = resourceFactory;
            return this;
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

        public Builder<T> setResourceCache(NetworkResourceCache<T> resourceCache) {
            this.resourceCache = resourceCache;
            return this;
        }

        public Builder<T> setDateFormat(DateFormat dateFormat){
            this.dateFormat = dateFormat;
            return this;
        }

        public NetworkSource<T> build() {
            if (connectionSource == null || dao == null)
                throw new IllegalArgumentException("Must supply either a Dao and ConnectionSource or a Database instance");
            if(server == null || resourceFactory == null)
                throw new IllegalArgumentException("Must supply a Server and ResourceFactory");
            if(resourceCache==null){
                resourceCache = new SparseArrayNetworkResourceCache<>(50);
            }
            return new NetworkSource<>(server, dao, connectionSource, resourceFactory, resourceCache, permissionsManager, dateFormat);
        }
    }


    /**
     * Get a resource by it's server id. Method will try the cache, otherwise resorting to the database
     * and checking for updates on the network if possible.
     */
    public void findByServerId(final @NonNull Integer serverId, final @NonNull OperationCallback<T> callback) {
        BackgroundThread.postBackground(new Runnable() {
            @Override
            public void run() {
                ResourceCache.CacheResult<T> result = networkResourceCache.getByServerId(serverId, new ResourceCache.CacheMissCallback<T>() {
                    @Nullable
                    @Override
                    public T onCacheMiss(int id) {
                        try {
                            List<T> items = getDao().queryForEq("serverId", serverId);
                            if (!items.isEmpty()) {
                                return items.get(0);
                            }
                        }catch (SQLException e){
                            onSQLException(e);
                        }
                        return null;
                    }
                });
                final T item = getPermissions().can(Op.READ, result.getItem()) ? result.getItem() : null;
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
    public void getManyFromNetwork(@Nullable final JSONObject search, @Nullable final OperationCallback<List<T>> callback) {
        BackgroundThread.postBackground(new Runnable() {
            @Override
            public void run() {
                final List<T> returnResults = new ArrayList<>();
                try {
                    Server.Response response = server.getItems(getDataClass(), search);
                    if (!server.isErrorResponse(response)) {
                        //got valid network response
                        List<JSONObject> newInstances = resourceFactory.splitMultipleNetworkQuery(response.getResponseBody());
                        for (final JSONObject data : newInstances) {
                            int serverId = data.getInt("id");
                            ResourceCache.CacheResult<T> result = networkResourceCache.getByServerId(serverId, new ResourceCache.CacheMissCallback<T>() {
                                @Nullable
                                @Override
                                public T onCacheMiss(int id) {
                                    T newInstance = null;
                                    try {
                                        List<T> results = getDao().queryForEq("serverId", id);
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
                            T item = result.getItem();
                            if (item != null && getPermissions().canUpdate(item)) {
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
                        for(T r : returnResults){
                            r.notifyObservers();
                        }
                    }
                });
            }
        });
    }


    public void sync(@Nullable final OperationCallback<Void> callback) {
        BackgroundThread.postBackground(new Runnable() {
            @Override
            public void run() {
                final List<T> changed = new ArrayList<T>();
                try {
                    List<T> unsynced = getDao().queryForEq("synced", false);
                    for (T item : unsynced) {
                        item = networkResourceCache.putIfMissing(item);
                        try {
                            Server.Response response;
                            if (item.getServerId() == null && getPermissions().canCreate(item)) {
                                response = server.createItem(getDataClass(), resourceFactory.turnItemIntoValidServerPayload(item));
                            } else if(getPermissions().canUpdate(item)) {
                                response = server.updateItem(getDataClass(), item.getServerId(), resourceFactory.turnItemIntoValidServerPayload(item));
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
                                }
                                getDao().update(item);
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
                        for (T c : changed) {
                            c.notifyObservers();
                        }
                    }
                });
            }
        });
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
    protected void onAfterCacheAdd(final T resource) {
        super.onAfterCreated(resource);
        if (resource.getServerId() != null && getPermissions().canUpdate(resource)) {
            //has a server id, so see if network has update
            try {
                Server.Response response = server.getItem(getDao().getDataClass(), resource.getServerId());
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

    @Override
    protected void onBeforeCreated(T created){
        super.onBeforeCreated(created);
        created.synced = false;
    }

    @Override
    protected void onAfterCreated(final T resource){
        super.onAfterCreated(resource);
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

    @Override
    protected void onBeforeUpdated(T updated){
        super.onBeforeDeleted(updated);
        updated.synced = false;
    }

    @Override
    protected void onAfterUpdated(final T resource){
        super.onAfterUpdated(resource);
        try {
            Server.Response response = null;
            try {
                if (resource.getServerId() == null) {
                    if (getPermissions().canCreate(resource)) {
                        response = server.createItem(getDataClass(), resourceFactory.turnItemIntoValidServerPayload(resource));
                    }
                } else {
                    if (getPermissions().canUpdate(resource)) {
                        response = server.updateItem(getDataClass(), resource.getServerId(), resourceFactory.turnItemIntoValidServerPayload(resource));
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

    @Override
    protected void onAfterDeleted(T resource){
        super.onAfterDeleted(resource);
        if (resource.getServerId() != null) {
            try {
                try {
                    Server.Response response = server.deleteItem(getDataClass(), resource.getServerId());
                    if (server.isErrorResponse(response)) {
                        deletedResourceDao.create(new DeletedResource(resource));
                    }
                } catch (Server.NetworkException e) {
                    deletedResourceDao.create(new DeletedResource(resource));
                    onNetworkException(e);
                }
            } catch (SQLException e) {
                onSQLException(e);
            }
        }
    }
}
