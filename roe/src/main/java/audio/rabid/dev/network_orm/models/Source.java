package audio.rabid.dev.network_orm.models;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.j256.ormlite.dao.Dao;

import org.json.JSONException;
import org.json.JSONObject;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Callable;

import audio.rabid.dev.network_orm.models.cache.ResourceCache;

/**
 * Created by charles on 10/28/15.
 * <p/>
 * This stores methods for interacting with particular Resources. It assumes all calls are from the
 * main thread, and handles fetching off the main thread and returning on it. It also handles instance
 * caches and deciding when to update values. A resource can override this to handle these interactions
 * in a custom way or add more.
 * <p/>
 * Use {@link #doSingleOperation(OperationCallback, SingleSourceOperation)}  and
 * {@link #doMultipleOperation(OperationCallback, MultipleSourceOperation)}  for your public facing methods.
 * This will handle running operations in the background and returning on main.
 */
public class Source<T extends Resource> {

    private Server server;
    private Dao<T, Integer> dao;
    private ResourceCache<T> resourceCache;
    private ResourceFactory<T> resourceFactory;
    private AllowedOps permissions;

    /**
     * Create a new Source
     *
     * @param server          the server instance to use for network operations
     * @param dao             the dao instance to use for database operations
     * @param resourceCache   the cache to use for keeping instances consistent
     * @param resourceFactory the factory for generating new Resources
     * @param permissions     the operations allowed to be done on the resource
     */
    public Source(@NonNull Server server, @NonNull Dao<T, Integer> dao, @NonNull ResourceCache<T> resourceCache,
                  @NonNull ResourceFactory<T> resourceFactory, @NonNull AllowedOps permissions) {
        this.server = server;
        this.dao = dao;
        this.resourceCache = resourceCache;
        this.resourceFactory = resourceFactory;
        this.permissions = permissions;
    }

    protected Dao<T, Integer> getDao() {
        return dao;
    }

    protected Server getServer() {
        return server;
    }

    public AllowedOps getPermissions() {
        return permissions;
    }

    public void onDatabaseException(SQLException e){
        throw new RuntimeException(e);
    }

    public void onJSONException(JSONException e){
        throw new RuntimeException(e);
    }

    public void onNetworkException(Server.NetworkException e){
        //default: no-op
    }

    public void getLocal(final int localId, @NonNull OperationCallback<T> callback) {
        doSingleOperation(callback, new SingleSourceOperation<T>() {
            @Override
            public T doInBackground(final Dao<T, Integer> dao, Server server, ResourceCache<T> cache, ResourceFactory<T> factory) {
                return cache.getByLocalId(localId, new ResourceCache.CacheMissCallback<T>() {
                    @Nullable
                    @Override
                    public T onCacheMiss(int id) {
                        try {
                            T resource = dao.queryForId(id);
                            return cacheGetNetworkUpdateOnMiss(resource);
                        } catch (SQLException e) {
                            onDatabaseException(e);
                            return null;
                        }
                    }
                });
            }

            @Override
            public AllowedOps.Op[] requiredPermissions() {
                return new AllowedOps.Op[]{AllowedOps.Op.READ};
            }
        });
    }

    public void getByServerId(final int serverId, @NonNull OperationCallback<T> callback) {
        doSingleOperation(callback, new SingleSourceOperation<T>() {
            @Override
            public T doInBackground(final Dao<T, Integer> dao, final Server server, ResourceCache<T> cache, final ResourceFactory<T> factory) {
                return resourceCache.getByServerId(serverId, new ResourceCache.CacheMissCallback<T>() {
                    @Nullable
                    @Override
                    public T onCacheMiss(int id) {
                        try {
                            List<T> results = dao.queryForEq("serverId", id);
                            if (results.isEmpty()) {
                                if (getPermissions().canCreate()) {
                                    //try and create from server
                                    try {
                                        Server.Response response = server.getItem(dao.getDataClass(), id);
                                        if (!server.isErrorResponse(response)) {
                                            T newResource = factory.createFromJSON(response.getResponseBody());
                                            newResource.synced = true;
                                            newResource.createdAt = newResource.updatedAt = new Date();
                                            dao.create(newResource);
                                            return newResource;
                                        }
                                    } catch (Server.NetworkException e) {
                                        //oh well, guess there's no network
                                        onNetworkException(e);
                                    }
                                }
                                return null;
                            } else {
                                return cacheGetNetworkUpdateOnMiss(results.get(0));
                            }
                        } catch (SQLException e) {
                            onDatabaseException(e);
                            return null;
                        } catch (JSONException e) {
                            onJSONException(e);
                            return null;
                        }
                    }
                });
            }

            @Override
            public AllowedOps.Op[] requiredPermissions() {
                return new AllowedOps.Op[]{AllowedOps.Op.READ};
            }
        });
    }

    public void getAllLocal(@NonNull OperationCallback<List<T>> callback) {
        doMultipleLocalQuery(callback, new MultipleLocalQuery<T>() {
            @Override
            public List<T> query(Dao<T, Integer> dao) throws SQLException {
                return dao.queryForAll();
            }
        });
    }

    public void createOrUpdateManyFromNetwork(@Nullable final JSONObject search, @Nullable final OperationCallback<List<T>> callback) {
        doMultipleOperation(callback, new MultipleSourceOperation<T>() {
            @Override
            public List<T> doInBackground(final Dao<T, Integer> dao, Server server, ResourceCache<T> cache, final ResourceFactory<T> factory) {
                try {
                    Server.Response response = server.getItems(dao.getDataClass(), search);
                    if (!server.isErrorResponse(response)) {
                        List<JSONObject> newInstances = factory.splitMultipleNetworkQuery(response.getResponseBody());
                        List<T> returnResults = new ArrayList<T>(newInstances.size());
                        for (final JSONObject data : newInstances) {
                            T newInstance = cache.getByServerId(data.getInt("id"), new ResourceCache.CacheMissCallback<T>() {
                                @Nullable
                                @Override
                                public T onCacheMiss(int id) {
                                    try {
                                        List<T> results = dao.queryForEq("serverId", id);
                                        if (results.isEmpty()) {
                                            if (getPermissions().canCreate()) {
                                                try {
                                                    T newItem = factory.createFromJSON(data);
                                                    newItem.synced = true;
                                                    newItem.createdAt = newItem.updatedAt = new Date();
                                                    dao.create(newItem);
                                                    return newItem;
                                                } catch (JSONException e) {
                                                    onJSONException(e);
                                                }
                                            }
                                            return null;
                                        } else {
                                            //update will happen after cache return
                                            return results.get(0);
                                        }
                                    } catch (SQLException e) {
                                        onDatabaseException(e);
                                        return null;
                                    }
                                }
                            });
                            boolean changed;
                            synchronized (newInstance) {
                                changed = factory.updateItemDirect(newInstance, data);
                                if (changed) {
                                    newInstance.synced = true;
                                    newInstance.updatedAt = new Date();
                                    newInstance.setChanged();
                                }
                            }
                            if (changed) {
                                dao.update(newInstance);
                            }
                            returnResults.add(newInstance);
                        }
                        return returnResults;
                    }
                } catch (JSONException e) {
                    onJSONException(e);
                } catch (SQLException e) {
                    onDatabaseException(e);
                } catch (Server.NetworkException e) {
                    //oh well, no items for you
                    onNetworkException(e);
                }
                return null;
            }

            @Override
            public AllowedOps.Op[] requiredPermissions() {
                return new AllowedOps.Op[]{AllowedOps.Op.UPDATE};
            }
        });
    }

    public void create(final T resource, @Nullable OperationCallback<T> callback) {
        doSingleOperation(callback, new SingleSourceOperation<T>() {
            @Override
            public T doInBackground(Dao<T, Integer> dao, Server server, ResourceCache<T> cache, ResourceFactory<T> factory) {
                try {
                    resource.createdAt = resource.updatedAt = new Date();
                    resource.synced = false;
                    try {
                        Server.Response response = server.createItem(dao.getDataClass(), factory.turnItemIntoValidServerPayload(resource));
                        if (!server.isErrorResponse(response)) {
                            boolean changed = resourceFactory.updateItem(resource, response.getResponseBody());
                            if (changed) {
                                resource.updatedAt = new Date();
                                resource.synced = true;
                            }
                        }
                    } catch (Server.NetworkException e) {
                        //oh well, item stays unsynced
                        onNetworkException(e);
                    }
                    dao.create(resource);
                    return cache.put(resource);
                } catch (SQLException e) {
                    onDatabaseException(e);
                    return null;
                } catch (JSONException e) {
                    onJSONException(e);
                    return null;
                }
            }

            @Override
            public AllowedOps.Op[] requiredPermissions() {
                return new AllowedOps.Op[]{AllowedOps.Op.CREATE};
            }
        });
    }

    public void update(final T resource, @Nullable OperationCallback<T> callback) {
        doSingleOperation(callback, new SingleSourceOperation<T>() {
            @Override
            public T doInBackground(Dao<T, Integer> dao, Server server, ResourceCache<T> cache, ResourceFactory<T> factory) {
                cache.getByLocalId(resource.getId(), new ResourceCache.CacheMissCallback<T>() {
                    @Nullable
                    @Override
                    public T onCacheMiss(int id) {
                        throw new IllegalArgumentException("Instance must be in the cache before calling update");
                    }
                });;
                try {
                    Server.Response response = null;
                    try {
                        response = server.updateItem(dao.getDataClass(), resource.getServerId(), factory.turnItemIntoValidServerPayload(resource));
                    } catch (Server.NetworkException e) {
                        //oh well, try sync again later
                        onNetworkException(e);
                    }
                    synchronized (resource) {
                        if (response != null && !server.isErrorResponse(response)) {
                            factory.updateItem(resource, response.getResponseBody());
                            resource.synced = true;
                        }
                        resource.updatedAt = new Date();
                        resource.setChanged();
                    }
                    dao.update(resource);
                    return resource;
                } catch (SQLException e) {
                    onDatabaseException(e);
                } catch (JSONException e){
                    onJSONException(e);
                }
                return null;
            }

            @Override
            public AllowedOps.Op[] requiredPermissions() {
                return new AllowedOps.Op[]{AllowedOps.Op.UPDATE};
            }
        });
    }

    public void createOrUpdate(T resource, @Nullable OperationCallback<T> callback) {
        if (resource.getId() > 0) {
            update(resource, callback);
        } else {
            create(resource, callback);
        }
    }

    public void deleteLocal(final T resource, @Nullable OperationCallback<T> callback) {
        doSingleOperation(callback, new SingleSourceOperation<T>() {
            @Override
            public T doInBackground(Dao<T, Integer> dao, Server server, ResourceCache<T> cache, ResourceFactory<T> factory) {
                try {
                    dao.delete(resource);
                    cache.delete(resource);
                    synchronized (resource) {
                        resource.deleted = true;
                        resource.setChanged();
                    }
                    return resource;
                } catch (SQLException e) {
                    onDatabaseException(e);
                    return null;
                }
            }

            @Override
            public AllowedOps.Op[] requiredPermissions() {
                return new AllowedOps.Op[]{AllowedOps.Op.DELETE};
            }
        });
    }

    @Deprecated
    public void deleteBoth(final T resource, @Nullable OperationCallback<T> callback) {
        doSingleOperation(callback, new SingleSourceOperation<T>() {
            @Override
            public T doInBackground(Dao<T, Integer> dao, Server server, ResourceCache<T> cache, ResourceFactory<T> factory) {
                try {
                    Server.Response response = server.deleteItem(dao.getDataClass(), resource.getServerId());
                    if (server.isErrorResponse(response)) {
                        //TODO what to do if network delete fails?
                    }
                } catch (Server.NetworkException e) {
                    //TODO what to do if network delete fails?
                    onNetworkException(e);
                }
                try {
                    dao.delete(resource);
                    cache.delete(resource);
                    synchronized (resource) {
                        resource.deleted = true;
                        resource.setChanged();
                    }
                    return resource;
                } catch (SQLException e) {
                    onDatabaseException(e);
                    return null;
                }
            }
            @Override
            public AllowedOps.Op[] requiredPermissions() {
                return new AllowedOps.Op[]{AllowedOps.Op.DELETE};
            }
        });
    }

    public void sync(@Nullable OperationCallback<List<T>> callback) {
        if (!(getPermissions().canUpdate() || getPermissions().canCreate())) return; //nothing to do
        doMultipleOperation(callback, new MultipleSourceOperation<T>() {
            @Override
            public List<T> doInBackground(Dao<T, Integer> dao, Server server, ResourceCache<T> cache, ResourceFactory<T> factory) {
                try {
                    List<T> unsynced = dao.queryForEq("synced", false);
                    List<T> returnResults = new ArrayList<T>(unsynced.size());
                    for (T item : unsynced) {
                        item = cache.putIfMissing(item);
                        try {
                            Server.Response response;
                            if (item.getServerId() < 0 && getPermissions().canCreate()) {
                                response = server.createItem(dao.getDataClass(), factory.turnItemIntoValidServerPayload(item));
                            } else if (getPermissions().canUpdate()) {
                                response = server.updateItem(dao.getDataClass(), item.getServerId(), factory.turnItemIntoValidServerPayload(item));
                            } else {
                                continue;
                            }
                            if (!server.isErrorResponse(response)) {
                                synchronized (item) {
                                    factory.updateItem(item, response.getResponseBody());
                                    item.synced = true;
                                    item.updatedAt = new Date();
                                    item.setChanged();
                                }
                                dao.update(item);
                                returnResults.add(item);
                            }
                        } catch (Server.NetworkException e) {
                            //oh well, still no network
                            onNetworkException(e);
                        }
                    }
                    return returnResults;
                } catch (SQLException e) {
                    onDatabaseException(e);
                } catch (JSONException e) {
                    onJSONException(e);
                }
                return null;
            }

            @Override
            public AllowedOps.Op[] requiredPermissions() {
                return new AllowedOps.Op[0];
            }
        });
    }

    /******************************************************************************************/

    /**
     * Try and get a matching instance from the cache, and if it isn't there, try a network update
     * for the resource if possible before putting it in the cache.
     */
    protected T cacheGetNetworkUpdateOnMiss(final T resource) {
        if (resource == null) {
            return null;
        }
        return resourceCache.getByLocalId(resource.getId(), new ResourceCache.CacheMissCallback<T>() {
            @Override
            public T onCacheMiss(int id) {
                if (resource.getServerId() > 0 && getPermissions().canUpdate()) {
                    //has a server id, so see if network has update
                    try {
                        Server.Response response = server.getItem(dao.getDataClass(), resource.getServerId());
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
                                if(changed){
                                    dao.update(resource); //save changes to database
                                }
                            } catch (JSONException e) {
                                onJSONException(e);
                                return null;
                            } catch (SQLException e){
                                onDatabaseException(e);
                                return null;
                            }
                        }
                    } catch (Server.NetworkException e) {
                        //just put in cache as it is
                        onNetworkException(e);
                    }
                }
                return resource;
            }
        });
    }

    private void checkPermissions(AllowedOps.Op[] required) {
        for (AllowedOps.Op r : required) {
            if (!getPermissions().can(r)) {
                throw new RuntimeException("Permission " + r.toString() + " denied for " + dao.getDataClass().toString());
            }
        }
    }

    protected void doSingleLocalQuery(@Nullable OperationCallback<T> callback, @NonNull final SingleLocalQuery<T> query) {
        doSingleOperation(callback, new SingleSourceOperation<T>() {
            @Override
            public T doInBackground(Dao<T, Integer> dao, Server server, ResourceCache<T> cache, ResourceFactory<T> factory) {
                try {
                    return cacheGetNetworkUpdateOnMiss(query.query(dao));
                } catch (SQLException e) {
                    onDatabaseException(e);
                    return null;
                }
            }

            @Override
            public AllowedOps.Op[] requiredPermissions() {
                return new AllowedOps.Op[]{AllowedOps.Op.READ};
            }
        });
    }

    protected void doMultipleLocalQuery(@Nullable OperationCallback<List<T>> callback, @NonNull final MultipleLocalQuery<T> query) {
        doMultipleOperation(callback, new MultipleSourceOperation<T>() {
            @Override
            public List<T> doInBackground(Dao<T, Integer> dao, Server server, ResourceCache<T> cache, ResourceFactory<T> factory) {
                try {
                    List<T> results = query.query(dao);
                    List<T> returnResults = new ArrayList<T>(results.size());
                    for (T r : results) {
                        returnResults.add(cacheGetNetworkUpdateOnMiss(r));
                    }
                    return returnResults;
                } catch (SQLException e) {
                    onDatabaseException(e);
                    return null;
                }
            }

            @Override
            public AllowedOps.Op[] requiredPermissions() {
                return new AllowedOps.Op[]{AllowedOps.Op.READ};
            }
        });
    }

    /**
     * Run some operation for one item off the main thread, calling the callback back on the main thread
     *
     * @param callback  the callback to return results on main
     * @param operation the operation to run
     */
    protected void doSingleOperation(@Nullable OperationCallback<T> callback, @NonNull final SingleSourceOperation<T> operation) {
        (new SourceAsyncTask.SingleSourceAsyncTask<T>(callback) {
            @Override
            protected T runInBackground() {
                checkPermissions(operation.requiredPermissions());
                return operation.doInBackground(dao, server, resourceCache, resourceFactory);
            }
        }).execute();
    }

    /**
     * Run some operation for many items off the main thread, calling the callback back on the main thread
     *
     * @param callback  the callback to return results on main
     * @param operation the operation to run
     */
    protected void doMultipleOperation(@Nullable OperationCallback<List<T>> callback, @NonNull final MultipleSourceOperation<T> operation) {
        checkPermissions(operation.requiredPermissions());
        (new SourceAsyncTask.MultipleSourceAsyncTask<T>(callback) {
            @Override
            protected List<T> runInBackground() {
                checkPermissions(operation.requiredPermissions());
                try {
                    //by running operation as a batch task, all database hits happen as one transaction, which should improve performance
                    return dao.callBatchTasks(new Callable<List<T>>() {
                        @Override
                        public List<T> call() throws Exception {
                            return operation.doInBackground(dao, server, resourceCache, resourceFactory);
                        }
                    });
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        }).execute();
    }


    protected interface SingleSourceOperation<Q extends Resource> {
        Q doInBackground(Dao<Q, Integer> dao, Server server, ResourceCache<Q> cache, ResourceFactory<Q> factory);

        AllowedOps.Op[] requiredPermissions();
    }

    protected interface MultipleSourceOperation<Q extends Resource> {
        List<Q> doInBackground(Dao<Q, Integer> dao, Server server, ResourceCache<Q> cache, ResourceFactory<Q> factory);

        AllowedOps.Op[] requiredPermissions();
    }

    protected interface SingleLocalQuery<Q extends Resource> {
        Q query(Dao<Q, Integer> dao) throws SQLException;
    }

    protected interface MultipleLocalQuery<Q extends Resource> {
        List<Q> query(Dao<Q, Integer> dao) throws SQLException;
    }
}
