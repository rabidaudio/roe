package audio.rabid.dev.network_orm;

import android.util.Log;

import com.j256.ormlite.dao.Dao;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.JSONException;
import org.json.JSONObject;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by charles on 10/28/15.
 *
 * This stores methods for interacting with particular Resources. It assumes all calls are from the
 * main thread, and handles fetching off the main thread and returning on it. It also handles instance
 * caches and deciding when to update values. A resource can override this to handle these interactions
 * in a custom way or add more.
 *
 * Use {@link #doSingleOperation(QueryCallback, SingleSourceOperation)}  and
 * {@link #doMultipleOperation(QueryCallback, MultipleSourceOperation)}  for your public facing methods.
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
     * @param server the server instance to use for network operations
     * @param dao the dao instance to use for database operations
     * @param resourceCache the cache to use for keeping instances consistent
     * @param resourceFactory the factory for generating new Resources
     * @param permissions the operations allowed to be done on the resource
     */
    public Source(@NotNull Server server, @NotNull Dao<T, Integer> dao, @NotNull ResourceCache<T> resourceCache,
                  @NotNull ResourceFactory<T> resourceFactory, @NotNull AllowedOps permissions){
        this.server = server;
        this.dao = dao;
        this.resourceCache = resourceCache;
        this.resourceFactory = resourceFactory;
        this.permissions = permissions;
    }

    public void getLocal(final int localId, @NotNull QueryCallback<T> callback){
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
                            throw new RuntimeException(e);
                        }
                    }
                });
            }

            @Override
            public AllowedOps requiredPermissions() {
                return new AllowedOps(AllowedOps.Op.READ);
            }
        });
    }

    public void getByServerId(final int serverId, @NotNull QueryCallback<T> callback){
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
                                if (permissions.canCreate()) {
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
                                    }
                                }
                                return null;
                            } else {
                                return cacheGetNetworkUpdateOnMiss(results.get(0));
                            }
                        } catch (SQLException | JSONException e) {
                            throw new RuntimeException(e);
                        }
                    }
                });
            }

            @Override
            public AllowedOps requiredPermissions() {
                return new AllowedOps(AllowedOps.Op.READ);
            }
        });
    }

    public void getAllLocal(@NotNull QueryCallback<List<T>> callback){
        doMultipleLocalQuery(callback, new MultipleLocalQuery<T>() {
            @Override
            public List<T> query(Dao<T, Integer> dao) throws SQLException {
                return dao.queryForAll();
            }
        });
    }

    public void createOrUpdateManyFromNetwork(@Nullable final JSONObject search, @Nullable final QueryCallback<List<T>> callback){
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
                                            if (permissions.canCreate()) {
                                                try {
                                                    T newItem = factory.createFromJSON(data);
                                                    newItem.synced = true;
                                                    newItem.createdAt = newItem.updatedAt = new Date();
                                                    dao.create(newItem);
                                                    return newItem;
                                                }catch (JSONException e){
                                                    throw new RuntimeException(e);
                                                }
                                            } else {
                                                return null;
                                            }
                                        } else {
                                            //update will happen after cache return
                                            return results.get(0);
                                        }
                                    } catch (SQLException e) {
                                        throw new RuntimeException(e);
                                    }
                                }
                            });
                            boolean changed = factory.updateItem(newInstance, data);
                            if (changed) {
                                newInstance.synced = true;
                                newInstance.updatedAt = new Date();
                                dao.update(newInstance);
                            }
                            returnResults.add(newInstance);
                        }
                        return returnResults;
                    }
                }catch(JSONException | SQLException e){
                    throw new RuntimeException(e);
                }catch (Server.NetworkException e){
                    //oh well, no items for you
                }
                return null;
            }

            @Override
            public AllowedOps requiredPermissions() {
                return new AllowedOps(AllowedOps.Op.UPDATE);
            }
        });
    }

    public void create(final T resource, @Nullable QueryCallback<T> callback){
        doSingleOperation(callback, new SingleSourceOperation<T>() {
            @Override
            public T doInBackground(Dao<T, Integer> dao, Server server, ResourceCache<T> cache, ResourceFactory<T> factory) {
                try {
                    resource.createdAt = resource.updatedAt = new Date();
                    resource.synced = false;
                    try {
                        Server.Response response = server.createItem(dao.getDataClass(), resource.toJSON());
                        if (!server.isErrorResponse(response)) {
                            boolean changed = resourceFactory.updateItem(resource, response.getResponseBody());
                            if (changed) {
                                resource.updatedAt = new Date();
                                resource.synced = true;
                            }
                        }
                    }catch (Server.NetworkException e){
                        //oh well, item stays unsynced
                    }
                    dao.create(resource);
                    return cache.put(resource);
                }catch (SQLException | JSONException e){
                    throw new RuntimeException(e);
                }
            }

            @Override
            public AllowedOps requiredPermissions() {
                return new AllowedOps(AllowedOps.Op.CREATE);
            }
        });
    }

    public void update(final T resource, @Nullable QueryCallback<T> callback){
        doSingleOperation(callback, new SingleSourceOperation<T>() {
            @Override
            public T doInBackground(Dao<T, Integer> dao, Server server, ResourceCache<T> cache, ResourceFactory<T> factory) {
                cache.getByLocalId(resource.getId(), new ResourceCache.CacheMissCallback<T>() {
                    @Nullable
                    @Override
                    public T onCacheMiss(int id) {
                        throw new IllegalArgumentException("Instance must be in the cache before calling update");
                    }
                });
                resource.synced = false;
                try {
                    try {
                        Server.Response response = server.updateItem(dao.getDataClass(), resource.getServerId(), resource.toJSON());
                        if (!server.isErrorResponse(response)) {
                            factory.updateItem(resource, response.getResponseBody());
                            resource.synced = true;
                        }
                    }catch (Server.NetworkException e){
                        //oh well, try sync again later
                    }
                    resource.updatedAt = new Date();
                    dao.update(resource);
                    return resource;
                }catch (SQLException | JSONException e){
                    throw new RuntimeException(e);
                }
            }

            @Override
            public AllowedOps requiredPermissions() {
                return new AllowedOps(AllowedOps.Op.UPDATE);
            }
        });
    }

    public void createOrUpdate(T resource, @Nullable QueryCallback<T> callback){
        if(resource.getId()>0){
            update(resource, callback);
        }else{
            create(resource, callback);
        }
    }

    public void deleteLocal(final T resource, @Nullable QueryCallback<T> callback){
        doSingleOperation(callback, new SingleSourceOperation<T>() {
            @Override
            public T doInBackground(Dao<T, Integer> dao, Server server, ResourceCache<T> cache, ResourceFactory<T> factory) {
                try {
                    dao.delete(resource);
                    resource.deleted = true;
                    cache.delete(resource);
                    return resource;
                }catch (SQLException e){
                    throw new RuntimeException(e);
                }
            }

            @Override
            public AllowedOps requiredPermissions() {
                return new AllowedOps(AllowedOps.Op.DELETE);
            }
        });
    }

    @Deprecated
    public void deleteBoth(final T resource, @Nullable QueryCallback<T> callback){
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
                }
                try {
                    dao.delete(resource);
                    resource.deleted = true;
                    cache.delete(resource);
                    return resource;
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            }

            @Override
            public AllowedOps requiredPermissions() {
                return new AllowedOps(AllowedOps.Op.DELETE);
            }
        });
    }

    public void sync(@Nullable QueryCallback<List<T>> callback){
        if(!(permissions.canUpdate() || permissions.canCreate())) return; //nothing to do
        doMultipleOperation(callback, new MultipleSourceOperation<T>() {
            @Override
            public List<T> doInBackground(Dao<T, Integer> dao, Server server, ResourceCache<T> cache, ResourceFactory<T> factory) {
                try {
                    List<T> unsynced = dao.queryForEq("synced", false);
                    for (T item : unsynced) {
                        item = cache.putIfMissing(item);
                        try {
                            Server.Response response;
                            if (item.getServerId() < 0 && permissions.canCreate()) {
                                response = server.createItem(dao.getDataClass(), item.toJSON());
                            } else if (permissions.canUpdate()) {
                                response = server.updateItem(dao.getDataClass(), item.getServerId(), item.toJSON());
                            } else {
                                continue;
                            }
                            if (!server.isErrorResponse(response)) {
                                factory.updateItem(item, response.getResponseBody());
                                item.synced = true;
                                item.updatedAt = new Date();
                                dao.update(item);
                            }
                        } catch (Server.NetworkException e) {
                            //oh well, still no network
                        }
                    }
                    return unsynced;
                } catch (SQLException | JSONException e) {
                    throw new RuntimeException(e);
                }
            }

            @Override
            public AllowedOps requiredPermissions() {
                return null;
            }
        });
    }

    /******************************************************************************************/

    /**
     * Try and get a matching instance from the cache, and if it isn't there, try a network update
     * for the item if possible before putting it in the cache.
     */
    protected T cacheGetNetworkUpdateOnMiss(final T item){
        if(item==null){
            return null;
        }
        return resourceCache.getByLocalId(item.getId(), new ResourceCache.CacheMissCallback<T>() {
            @Override
            public T onCacheMiss(int id) {
                if(item.getServerId()>0 && permissions.canUpdate()){
                    //has a server id, so see if network has update
                    try {
                        Server.Response response = server.getItem(dao.getDataClass(), item.getServerId());
                        if(!server.isErrorResponse(response)){
                            try {
                                boolean changed = resourceFactory.updateItem(item, response.getResponseBody());
                                if(changed) {
                                    item.synced = true;
                                    item.updatedAt = new Date();
                                    dao.update(item); //save changes to database
                                }
                            }catch (JSONException | SQLException e){
                                throw new RuntimeException(e);
                            }
                        }
                    }catch (Server.NetworkException e){
                        Log.w(this.toString(), "Network issue", e);
                        //just put in cache as it is
                    }
                }
                return item;
            }
        });
    }

    private void checkPermissions(AllowedOps required){
        for(AllowedOps.Op r : required.getOps()) {
            if (!permissions.can(r)) {
                throw new RuntimeException("Permission " + r.toString() + " denied for " + dao.getDataClass().toString());
            }
        }
    }

    protected void doSingleLocalQuery(@Nullable QueryCallback<T> callback, @NotNull final SingleLocalQuery<T> query){
        doSingleOperation(callback, new SingleSourceOperation<T>() {
            @Override
            public T doInBackground(Dao<T, Integer> dao, Server server, ResourceCache<T> cache, ResourceFactory<T> factory) {
                try {
                    return cacheGetNetworkUpdateOnMiss(query.query(dao));
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            }

            @Override
            public AllowedOps requiredPermissions() {
                return new AllowedOps(AllowedOps.Op.READ);
            }
        });
    }

    protected void doMultipleLocalQuery(@Nullable QueryCallback<List<T>> callback, @NotNull final MultipleLocalQuery<T> query){
        doMultipleOperation(callback, new MultipleSourceOperation<T>() {
            @Override
            public List<T> doInBackground(Dao<T, Integer> dao, Server server, ResourceCache<T> cache, ResourceFactory<T> factory) {
                try {
                    List<T> results = query.query(dao);
                    List<T> returnResults = new ArrayList<T>(results.size());
                    for(T r : results){
                        returnResults.add(cacheGetNetworkUpdateOnMiss(r));
                    }
                    return returnResults;
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            }

            @Override
            public AllowedOps requiredPermissions() {
                return new AllowedOps(AllowedOps.Op.READ);
            }
        });
    }

    /**
     * Run some operation for one item off the main thread, calling the callback back on the main thread
     * @param callback the callback to return results on main
     * @param operation the operation to run
     */
    protected void doSingleOperation(@Nullable QueryCallback<T> callback, @NotNull final SingleSourceOperation<T> operation){
        (new SourceAsyncTask<T>(callback){
            @Override
            protected T runInBackground() {
                checkPermissions(operation.requiredPermissions());
                return operation.doInBackground(dao, server, resourceCache, resourceFactory);
            }
        }).execute();
    }

    /**
     * Run some operation for many items off the main thread, calling the callback back on the main thread
     * @param callback the callback to return results on main
     * @param operation the operation to run
     */
    protected void doMultipleOperation(@Nullable QueryCallback<List<T>> callback, @NotNull final MultipleSourceOperation<T> operation){
        checkPermissions(operation.requiredPermissions());
        (new SourceAsyncTask<List<T>>(callback){
            @Override
            protected List<T> runInBackground() {
                checkPermissions(operation.requiredPermissions());
                return operation.doInBackground(dao, server, resourceCache, resourceFactory);
            }
        }).execute();
    }


    /**
     * Handles running queries in background and callbacks on main thread
     * @param <A> the data type returned by the query
     */
    private abstract static class SourceAsyncTask<A> implements Runnable {

        @Nullable
        private QueryCallback<A> callback;

        public SourceAsyncTask(@Nullable QueryCallback<A> callback){
            this.callback = callback;
        }

        public void execute(){
            BackgroundThread.postBackground(this);
        }

        public void run(){
            final A result = runInBackground();
            if(callback != null)
                BackgroundThread.postMain(new Runnable() {
                    @Override
                    public void run() {
                        callback.onResult(result);
                    }
                });
        }

        protected abstract A runInBackground();
    }

    public interface QueryCallback<Q> {
        void onResult(@Nullable Q result);
    }

    protected interface SingleSourceOperation<Q extends Resource> {
        Q doInBackground(Dao<Q, Integer> dao, Server server, ResourceCache<Q> cache, ResourceFactory<Q> factory);

        AllowedOps requiredPermissions();
    }

    protected interface MultipleSourceOperation<Q extends Resource> {
        List<Q> doInBackground(Dao<Q, Integer> dao, Server server, ResourceCache<Q> cache, ResourceFactory<Q> factory);

        AllowedOps requiredPermissions();
    }

    protected interface SingleLocalQuery<Q extends Resource> {
        Q query(Dao<Q, Integer> dao) throws SQLException;
    }

    protected interface MultipleLocalQuery<Q extends Resource> {
        List<Q> query(Dao<Q, Integer> dao) throws SQLException;
    }
}
