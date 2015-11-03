package audio.rabid.dev.network_orm.models;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.TableUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.sql.SQLException;
import java.text.DateFormat;
import java.text.FieldPosition;
import java.text.ParsePosition;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Callable;

import audio.rabid.dev.network_orm.models.cache.ResourceCache;

/**
 * Created by charles on 10/28/15.
 * <p/>
 * This stores methods for interacting with particular {@link Resource}s. It assumes all calls are from the
 * main thread, and handles fetching off the main thread and returning on it. It also handles instance
 * caches and deciding when to update values. A resource can override this to handle these interactions
 * in a custom way or add more.
 * <p/>
 * Use {@link #doSingleOperation(SingleSourceOperation, OperationCallback)}  and
 * {@link #doMultipleOperation(MultipleSourceOperation, OperationCallback)}  for your public facing methods.
 * This will handle running operations in the background and returning on main.
 */
public class Source<T extends Resource> {

    private Server server;
    private Dao<T, Integer> dao;
    private ResourceCache<T> resourceCache;
    private ResourceFactory<T> resourceFactory;
    private PermissionsManager<T> permissions;
    private DateFormat dateFormat;
    private Dao<DeletedResource, Integer> deletedResourceDao;

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
                  @NonNull ResourceFactory<T> resourceFactory, @NonNull PermissionsManager<T> permissions,
                  @Nullable DateFormat dateFormat, @NonNull ConnectionSource connectionSource) {
        this.server = server;
        this.dao = dao;
        this.resourceCache = resourceCache;
        this.resourceFactory = resourceFactory;
        this.permissions = permissions;
        if(dateFormat==null){
            this.dateFormat = new DateFormat() {
                @Override
                public StringBuffer format(Date date, StringBuffer buffer, FieldPosition field) {
                    return new StringBuffer(String.valueOf(date.getTime()));
                }

                @Override
                public Date parse(String string, ParsePosition position) {
                    try {
                        return new Date(Long.parseLong(string));
                    } catch (NumberFormatException e) {
                        position.setErrorIndex(0);
                        return null;
                    }
                }
            };
        }else{
            this.dateFormat = dateFormat;
        }
        try {
            TableUtils.createTableIfNotExists(connectionSource, DeletedResource.class);
            deletedResourceDao = DaoManager.createDao(connectionSource, DeletedResource.class);
        }catch (SQLException e){
            onDatabaseException(e);
        }
    }

    /**
     * Return the {@link Dao} used for database operations. You shouldn't need to access this, as
     * using the <code>doOperation</code> methods should give you the instance to use.
     */
    protected Dao<T, Integer> getDao() {
        return dao;
    }

    /**
     * Return the {@link Server} used for network operations. You shouldn't need to access this, as
     * using the <code>doOperation</code> methods should give you the instance to use.
     */
    protected Server getServer() {
        return server;
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
    public void onDatabaseException(SQLException e){
        throw new RuntimeException(e);
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

    /**
     * Method for encoding a date in JSON. Default implementation is to use Unix time
     * (milliseconds since Jan. 1, 1970, midnight GMT). Override for custom date format.
     */
    public DateFormat getDateFormat() {
        return dateFormat;
    }

    /**
     * Get a resource by it's local id. Method will try the cache, otherwise resorting to the database
     * and checking for updates on the network if possible.
     */
    public void getLocal(final @NonNull Integer localId, @NonNull OperationCallback<T> callback) {
        doSingleOperation(new SingleSourceOperation<T>() {
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
            public PermissionsManager.Op[] requiredPermissions() {
                return new PermissionsManager.Op[]{PermissionsManager.Op.READ};
            }
        }, callback);
    }

    /**
     * Get a resource by it's server id. Method will try the cache, otherwise resorting to the database
     * and checking for updates on the network if possible.
     */
    public void getByServerId(final @NonNull Integer serverId, @NonNull OperationCallback<T> callback) {
        doSingleOperation(new SingleSourceOperation<T>() {
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
            public PermissionsManager.Op[] requiredPermissions() {
                return new PermissionsManager.Op[]{PermissionsManager.Op.READ};
            }
        }, callback);
    }

    /**
     * Get all the instances stored locally. Will default to the cached versions, and update from the
     * network if necessary.
     */
    public void getAllLocal(@NonNull OperationCallback<List<T>> callback) {
        doMultipleLocalQuery(new MultipleLocalQuery<T>() {
            @Override
            public List<T> query(Dao<T, Integer> dao) throws SQLException {
                return dao.queryForAll();
            }
        }, callback);
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
    public void createOrUpdateManyFromNetwork(@Nullable final JSONObject search, @Nullable final OperationCallback<List<T>> callback) {
        doMultipleOperation(new MultipleSourceOperation<T>() {
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
            public PermissionsManager.Op[] requiredPermissions() {
                return new PermissionsManager.Op[]{PermissionsManager.Op.UPDATE};
            }
        }, callback);
    }

    public void create(final T resource, @Nullable OperationCallback<T> callback) {
        doSingleOperation(new SingleSourceOperation<T>() {
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
            public PermissionsManager.Op[] requiredPermissions() {
                return new PermissionsManager.Op[]{PermissionsManager.Op.CREATE};
            }
        }, callback);
    }

    public void update(final T resource, @Nullable OperationCallback<T> callback) {
        doSingleOperation(new SingleSourceOperation<T>() {
            @Override
            public T doInBackground(Dao<T, Integer> dao, Server server, ResourceCache<T> cache, ResourceFactory<T> factory) {
                cache.getByLocalId(resource.getId(), new ResourceCache.CacheMissCallback<T>() {
                    @Nullable
                    @Override
                    public T onCacheMiss(int id) {
                        throw new IllegalArgumentException("Instance must be in the cache before calling update");
                    }
                });
                try {
                    Server.Response response = null;
                    try {
                        if(resource.getServerId()==null){
                            response = server.createItem(dao.getDataClass(), factory.turnItemIntoValidServerPayload(resource));
                        }else {
                            response = server.updateItem(dao.getDataClass(), resource.getServerId(), factory.turnItemIntoValidServerPayload(resource));
                        }
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
                } catch (JSONException e) {
                    onJSONException(e);
                }
                return null;
            }

            @Override
            public PermissionsManager.Op[] requiredPermissions() {
                return new PermissionsManager.Op[]{PermissionsManager.Op.UPDATE};
            }
        }, callback);
    }

    public void createOrUpdate(T resource, @Nullable OperationCallback<T> callback) {
        if (resource.getId() != null) {
            update(resource, callback);
        } else {
            create(resource, callback);
        }
    }

    public void deleteLocal(final T resource, @Nullable OperationCallback<T> callback) {
        doSingleOperation(new SingleSourceOperation<T>() {
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
            public PermissionsManager.Op[] requiredPermissions() {
                return new PermissionsManager.Op[]{PermissionsManager.Op.DELETE};
            }
        }, callback);
    }

    public void deleteBoth(final T resource, @Nullable OperationCallback<T> callback) {
        doSingleOperation(new SingleSourceOperation<T>() {
            @Override
            public T doInBackground(Dao<T, Integer> dao, Server server, ResourceCache<T> cache, ResourceFactory<T> factory) {
                if (resource.getServerId() != null) {
                    try {
                        try {
                            Server.Response response = server.deleteItem(dao.getDataClass(), resource.getServerId());
                            if (server.isErrorResponse(response)) {
                                deletedResourceDao.create(new DeletedResource(resource));
                            }
                        } catch (Server.NetworkException e) {
                            deletedResourceDao.create(new DeletedResource(resource));
                            onNetworkException(e);
                        }
                        dao.delete(resource);
                        cache.delete(resource);
                        synchronized (resource) {
                            resource.deleted = true;
                            resource.setChanged();
                        }
                    } catch (SQLException e) {
                        onDatabaseException(e);
                    }
                }
                return resource;
            }

            @Override
            public PermissionsManager.Op[] requiredPermissions() {
                return new PermissionsManager.Op[]{PermissionsManager.Op.DELETE};
            }
        }, callback);
    }

    public void sync(@Nullable OperationCallback<List<T>> callback) {
        doMultipleOperation(new MultipleSourceOperation<T>() {
            @Override
            public List<T> doInBackground(Dao<T, Integer> dao, Server server, ResourceCache<T> cache, ResourceFactory<T> factory) {
                try {
                    List<T> unsynced = dao.queryForEq("synced", false);
                    List<T> returnResults = new ArrayList<T>(unsynced.size());
                    for (T item : unsynced) {
                        item = cache.putIfMissing(item);
                        try {
                            Server.Response response;
                            if (item.getServerId() == null && getPermissions().canCreate()) {
                                response = server.createItem(dao.getDataClass(), factory.turnItemIntoValidServerPayload(item));
                            } else if (getPermissions().canUpdate(item)) {
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
                    for (DeletedResource deleted : deletedResourceDao.queryForAll()) {
                        try {
                            Server.Response response = deleted.attemptDelete(server);
                            if (!server.isErrorResponse(response)) {
                                deletedResourceDao.delete(deleted);
                                //TODO not returning items that we finally deleted on the server
                            }
                        }catch (Server.NetworkException e){
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
            public PermissionsManager.Op[] requiredPermissions() {
                return new PermissionsManager.Op[0];
            }
        }, callback);
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
                if (resource.getServerId() != null && getPermissions().canUpdate(resource)) {
                    //has a server id, so see if network has update
//                    BackgroundThread.postBackground(new Runnable() {
//                        @Override
//                        public void run() {
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
//                                            BackgroundThread.postMain(new Runnable() {
//                                                @Override
//                                                public void run() {
//                                                    resource.notifyObservers();
//                                                }
//                                            });
                                        }
                                    } catch (JSONException e) {
                                        onJSONException(e);
                                    } catch (SQLException e){
                                        onDatabaseException(e);
                                    }
                                }
                            } catch (Server.NetworkException e) {
                                //just put in cache as it is
                                onNetworkException(e);
                            }
                        }
//                    });
//                }
                return resource;
            }
        });
    }

    private void checkPermissions(PermissionsManager.Op[] required, T object) {
        for (PermissionsManager.Op r : required) {
            if (!getPermissions().can(r, object)) {
                throw new RuntimeException("Permission " + r.toString() + " denied for " + dao.getDataClass().toString());
            }
        }
    }

    /**
     * A utility method for getting a resource by database query. In your {@link SingleLocalQuery},
     * use the provided {@link Dao} to run a database query and return the result. This method will
     * handle all the other stuff (caching, updating from network, etc). Use this for getting data
     * available locally only. To run a query for more than one result, use
     * {@link #doMultipleLocalQuery(MultipleLocalQuery, OperationCallback)}.
     *
     * This assumes that the only permission required for your query is {@link PermissionsManager.Op#READ}.
     * If you are doing anything that results in a SQL query other than SELECT, DO NOT use this, use
     * {@link #doSingleOperation(SingleSourceOperation, OperationCallback)} instead.
     */
    protected void doSingleLocalQuery(@NonNull final SingleLocalQuery<T> query, @Nullable OperationCallback<T> callback) {
        doSingleOperation(new SingleSourceOperation<T>() {
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
            public PermissionsManager.Op[] requiredPermissions() {
                return new PermissionsManager.Op[]{PermissionsManager.Op.READ};
            }
        }, callback);
    }

    /**
     * The same as {@link #doSingleLocalQuery(SingleLocalQuery, OperationCallback)}, but for queries
     * that return multiple items from the database.
     *
     * This assumes that the only permission required for your query is {@link PermissionsManager.Op#READ}.
     * If you are doing anything that results in a SQL query other than SELECT, DO NOT use this, use
     * {@link #doMultipleOperation(MultipleSourceOperation, OperationCallback)} instead.
     *
     * @see #doSingleLocalQuery(SingleLocalQuery, OperationCallback)
     */
    protected void doMultipleLocalQuery(@NonNull final MultipleLocalQuery<T> query, @Nullable OperationCallback<List<T>> callback) {
        doMultipleOperation(new MultipleSourceOperation<T>() {
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
            public PermissionsManager.Op[] requiredPermissions() {
                return new PermissionsManager.Op[]{PermissionsManager.Op.READ};
            }
        }, callback);
    }

    /**
     * Run some operation for one item off the main thread, calling the callback back on the main thread
     *
     * @param operation the operation to run
     * @param callback  the callback to return results on main
     */
    protected void doSingleOperation(@NonNull final SingleSourceOperation<T> operation, @Nullable OperationCallback<T> callback) {
        (new SourceAsyncTask.SingleSourceAsyncTask<T>(callback) {
            @Override
            protected T runInBackground() {
                checkPermissions(operation.requiredPermissions(), null); //TODO better way to handle permissions
                return operation.doInBackground(dao, server, resourceCache, resourceFactory);
            }
        }).execute();
    }

    /**
     * Run some operation for many items off the main thread, calling the callback back on the main thread
     *
     * @param operation the operation to run
     * @param callback  the callback to return results on main
     */
    protected void doMultipleOperation(@NonNull final MultipleSourceOperation<T> operation, @Nullable OperationCallback<List<T>> callback) {
        (new SourceAsyncTask.MultipleSourceAsyncTask<T>(callback) {
            @Override
            protected List<T> runInBackground() {
                checkPermissions(operation.requiredPermissions(), null); //TODO better way to handle permissions
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

    /**
     * Used to run a CRUD operation on a single {@link Resource}.
     */
    protected interface SingleSourceOperation<Q extends Resource> {

        /**
         * The method you use to do the operation
         * @param dao for accessing the local database
         * @param server for making api calls
         * @param cache for accessing the instance cache
         * @param factory for creating new instances
         * @return the item
         */
        Q doInBackground(Dao<Q, Integer> dao, Server server, ResourceCache<Q> cache, ResourceFactory<Q> factory);

        /**
         * @return An array of permissions that must be had for the operation to be allowed
         */
        PermissionsManager.Op[] requiredPermissions();
    }

    /**
     * Used to run a CRUD operation on a collection of Resources
     * @see SingleSourceOperation
     * @param <Q>
     */
    protected interface MultipleSourceOperation<Q extends Resource> {
        List<Q> doInBackground(Dao<Q, Integer> dao, Server server, ResourceCache<Q> cache, ResourceFactory<Q> factory);

        PermissionsManager.Op[] requiredPermissions();
    }

    /**
     * Used to run a query for a single local {@link Resource}.
     * @see #doSingleLocalQuery(SingleLocalQuery, OperationCallback)
     */
    protected interface SingleLocalQuery<Q extends Resource> {
        Q query(Dao<Q, Integer> dao) throws SQLException;
    }

    /**
     * Used to run a query for multiple local {@link Resource}s.
     * @see #doMultipleLocalQuery(MultipleLocalQuery, OperationCallback)
     */
    protected interface MultipleLocalQuery<Q extends Resource> {
        List<Q> query(Dao<Q, Integer> dao) throws SQLException;
    }

    /**
     * The general callback for operations done by {@link Source} in the background. Result could be
     * null if the item could not be found.
     */
    public interface OperationCallback<Q> {
        void onResult(@Nullable Q result);
    }
}
