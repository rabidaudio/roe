package audio.rabid.dev.network_orm;

import android.util.Log;
import android.util.SparseArray;

import com.j256.ormlite.dao.Dao;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.JSONArray;
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
 */
public class Source<T extends Resource> {

    private Server server;
    private Dao<T, Integer> dao;
    private String endpoint;
    private String jsonSingleObjectKey;
    private String jsonArrayObjectKey;
    private ResourceCreator<T> resourceCreator;
    private AllowedOps permissions;

    private final SparseArray<T> instanceCache = new SparseArray<>(50);

    public Source(@NotNull Server server, @NotNull Dao<T, Integer> dao, @NotNull String endpoint,
                  @NotNull String jsonSingleObjectKey, @NotNull String jsonArrayObjectKey,
                  @NotNull ResourceCreator<T> resourceCreator, @NotNull AllowedOps permissions){
        this.server = server;
        this.dao = dao;
        this.endpoint = endpoint;
        this.jsonSingleObjectKey = jsonSingleObjectKey;
        this.jsonArrayObjectKey = jsonArrayObjectKey;
        this.resourceCreator = resourceCreator;
        this.permissions = permissions;
    }

    protected Dao<T, Integer> getDao(){
        return dao;
    }

    protected Server getServer(){
        return server;
    }

    /**
     * To get a resource by it's local id, we don't know it's server id. so we should start
     * by checking to see if it is in the cache. If it is, it must be up-to-date, so return it.
     * Otherwise, look for one in the local database.
     *
     * @param localId the id to search for
     * @param callback which supplies the data
     */
    public void getLocal(final int localId, @NotNull QueryCallback<T> callback){
        checkPermissions(AllowedOps.Op.READ);
        (new SourceAsyncTask<T>(callback){
            @Override
            protected T runInBackground() {
                T result;
                try {
                    result = dao.queryForId(localId);
                }catch (SQLException e){
                    return null;
                }
                if(result == null){
                    return null; //not found
                }else{
                    return cacheGetNetworkUpdateOnMiss(result);
                }
            }
        }).execute();
    }

    /**
     * Grab all of the local items, and for each one, attempt a cache hit and server sync.
     * @param callback
     */
    public void getAllLocal(@NotNull QueryCallback<List<T>> callback){
        checkPermissions(AllowedOps.Op.READ);
        (new SourceAsyncTask<List<T>>(callback){
            @Override
            protected List<T> runInBackground() {
                List<T> results;
                try {
                    results = dao.queryForAll();
                }catch (SQLException e){
                    return null;
                }
                if(results==null || results.isEmpty()) return null;
                ArrayList<T> returnedResults = new ArrayList<T>(results.size());
                for(T r : results){
                    returnedResults.add(cacheGetNetworkUpdateOnMiss(r));
                }
                return returnedResults;
            }
        }).execute();
    }

    public void getByServerId(final int serverId, @NotNull QueryCallback<T> callback){
        checkPermissions(AllowedOps.Op.READ, AllowedOps.Op.CREATE);
        (new SourceAsyncTask<T>(callback){
            @Override
            protected T runInBackground() {
                T result;
                try {
                    List<T> q = dao.queryForEq("server_id", serverId);
                    if(q==null || q.isEmpty()){
                        result = null;
                    }else{
                        result = q.get(0);
                    }
                }catch (SQLException e){
                    result = null;
                }
                if(result==null){
                    //try to create from server
                    return cacheAddWithNetworkCreate(serverId);
                }else{
                    //get from cache, updating if required
                    return cacheGetNetworkUpdateOnMiss(result);
                }
            }
        }).execute();
    }

    /**
     * Run a search for multiple items on the server. For each returned, createOrUpdate local values.
     * @param payload
     * @param callback
     */
    public void remoteSearch(@Nullable final JSONObject payload, @Nullable QueryCallback<List<T>> callback){
        checkPermissions(AllowedOps.Op.READ, AllowedOps.Op.CREATE);
        (new SourceAsyncTask<List<T>>(callback){
            @Override
            protected List<T> runInBackground() {
                try {
                    Server.Response r = server.index(endpoint, payload);
                    if(r.wasError()) {
                        return null;
                    }else{
                        List<T> newInstances = new ArrayList<T>();
                        JSONArray array = r.getResponseBody().getJSONArray(jsonArrayObjectKey);
                        for(int i=0; i<array.length(); i++){
                            JSONObject o = array.getJSONObject(i);
                            T n = resourceCreator.createFromJSON(o);

                            List<T> fromServerId = dao.queryForEq("server_id", n.getServerId());
                            if(fromServerId != null && !fromServerId.isEmpty()){
                                //update
                                T existing = atomicCachePutIfMissing(fromServerId.get(0));
                                existing.updateFromJSON(o);
                                existing.synced = true;
                                existing.updatedAt = new Date();
                                dao.update(existing);
                                newInstances.add(existing);
                            }else{
                                //create
                                n.synced = true;
                                n.createdAt = n.updatedAt = new Date();
                                dao.create(n);
                                newInstances.add(atomicCachePut(n));
                            }
                        }
                        return newInstances;
                    }
                }catch (Server.NetworkException e){
                    Log.w(this.toString(), "Network issue", e);
                    //oh no, no items...
                    return null;
                }catch (SQLException | JSONException e){
                    throw new RuntimeException(e);
                }
            }
        }).execute();
    }

    public void createOrUpdate(T object, @Nullable QueryCallback<T> callback){
        checkPermissions(AllowedOps.Op.CREATE, AllowedOps.Op.UPDATE);
        if(object.getId()>0){
            update(object, callback);
        }else{
            create(object, callback);
        }
    }

    /**
     * On a create, we save the object to the database, and then try and save remotely as well.
     * We also need to add it to the object cache.
     */
    public void create(final T object, @Nullable QueryCallback<T> callback){
        checkPermissions(AllowedOps.Op.CREATE);
        (new SourceAsyncTask<T>(callback){
            @Override
            protected T runInBackground() {
                try {
                    object.createdAt = object.updatedAt = new Date();
                    dao.create(object);
                    try{
                        Server.Response r = server.create(endpoint, jsonSingleObjectKey, object);
                        updateFromResponse(object, r);
                    }catch (Server.NetworkException e){
                        Log.w(this.toString(), "Network issue", e);
                        //well, shoot. it's already marked as unsynced
                    }
                }catch (SQLException e){
                    throw new IllegalArgumentException("Couldn't create "+object.toString(), e);
                }
                return atomicCachePut(object);
            }
        }).execute();
    }

    /**
     * On an update, we can assume the object passed is already in the cache, so nothing to do there.
     * We update in the database, then update on the server,
     */
    public void update(final T object, @Nullable QueryCallback<T> callback){
        checkPermissions(AllowedOps.Op.UPDATE);
        (new SourceAsyncTask<T>(callback){
            @Override
            protected T runInBackground() {
                synchronized (object) { //don't let two update operations happen simultaneously for same object
                    try {
                        object.updatedAt = new Date();
                        dao.update(object);
                        try {
                            Server.Response r = server.update(endpoint, jsonSingleObjectKey, object);
                            updateFromResponse(object, r);
                        } catch (Server.NetworkException e) {
                            Log.w(this.toString(), "Network issue", e);
                            //well, shoot. it's already marked as unsynced
                        }
                    } catch (SQLException e) {
                        throw new IllegalArgumentException("Couldn't update " + object.toString(), e);
                    }
                    return atomicCachePutIfMissing(object);
                }
            }
        }).execute();
    }

    public void delete(final T object, final boolean deleteRemotely, @Nullable QueryCallback<T> callback){
        checkPermissions(AllowedOps.Op.DELETE);
        (new SourceAsyncTask<T>(callback){
            @Override
            protected T runInBackground() {
                synchronized (object) { //don't let two delete operations happen simultaneously for same object
                    if (deleteRemotely) {
                        try {
                            server.destroy(endpoint, object);
                        } catch (Server.NetworkException e) {
                            Log.w(this.toString(), "Network issue", e);
                            //TODO what do we do if remote delete fails?
                        }
                    }
                    try {
                        dao.delete(object);
                        object.deleted = true;
                    } catch (SQLException e) {
                        throw new RuntimeException(e);
                    }
                    return atomicCacheRemove(object);
                }
            }
        }).execute();
    }

    /**
     * Go through all unsynced items in the database, trying to update them remotely. Go ahead and store them
     * in the cache, too
     */
    public void sync(){
        BackgroundThread.postBackground(new Runnable() {
            @Override
            public void run() {
                try {
                    List<T> unsynced = dao.queryForEq("synced", false);
                    for (T u : unsynced) {
                        try {
                            Server.Response r;
                            if (u.serverId < 0) {
                                r = server.create(endpoint, jsonSingleObjectKey, u);
                            } else {
                                r = server.update(endpoint, jsonSingleObjectKey, u);
                            }
                            updateFromResponse(u, r);
                        } catch (Server.NetworkException e) {
                            Log.w(this.toString(), "Network issue", e);
                            //oh well, still no network
                        }
                        atomicCachePutIfMissing(u);
                    }
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            }
        });
    }

    /**
     * If the item has a serverId, update it with the latest server data and save it before
     * putting it in the cache and returning it.
     *
     * If it is there but doesn't have a server id, attempt a server sync before putting it in the
     * cache and returning it.
     *
     * If it isn't available locally, we have no way to look for it on the server, so just return null;
     */
    protected T cacheGetNetworkUpdateOnMiss(T item){
        //is it in the cache?
        T cached = atomicCacheFetch(item.getId());
        if(cached == null){
            //not in cache
            if(item.getServerId()>0){
                //has a server id, so see if network has update
                try {
                    Server.Response r = server.show(endpoint, item);
                    if(!r.wasError()){
                        boolean changed;
                        try {
                            changed = item.updateFromJSON(r.getResponseBody().getJSONObject(jsonSingleObjectKey)); //update values
                        }catch (JSONException e){
                            throw new RuntimeException(e);
                        }
                        if(changed) {
                            try {
                                item.synced = true;
                                item.updatedAt = new Date();
                                dao.update(item); //save changes to database
                            } catch (SQLException e) {
                                throw new RuntimeException(e);
                            }
                        }
                    }
                }catch (Server.NetworkException e){
                    Log.w(this.toString(), "Network issue", e);
                    //just put in cache as it is
                }
            }
            return atomicCachePut(item);
        }else{
            //in cache so must be up to date
            return cached;
        }
    }

    protected T cacheAddWithNetworkCreate(int serverId){
        try {
            Server.Response r = server.show(endpoint, serverId);
            if(!r.wasError()) {
                //create object
                T newInstance = resourceCreator.createFromJSON(r.getResponseBody().getJSONObject(jsonSingleObjectKey));
                //save local
                newInstance.synced = true;
                newInstance.createdAt = newInstance.updatedAt = new Date();
                dao.create(newInstance);
                //store in cache
                return atomicCachePut(newInstance);
            }else{
                return null;
            }
        }catch (Server.NetworkException e){
            Log.w(this.toString(), "Network issue", e);
            return null;
        }catch (SQLException | JSONException e){
            throw new RuntimeException(e);
        }
    }

    protected void updateFromResponse(T object, Server.Response r) throws SQLException {
        if (r.wasError()) {
            if(r.getErrorStatus().equals("not_found")){
                //try a create instead
                try {
                    Server.Response create = server.create(endpoint, jsonSingleObjectKey, object);
                    updateFromResponse(object, create);
                }catch (Server.NetworkException e){
                    //uggh what an annoying time to lose the network. keep sync = false
                    Log.w(this.toString(), "Network issue", e);
                }
            }
        }else{
            try {
                object.updateFromJSON(r.getResponseBody().getJSONObject(jsonSingleObjectKey));
            }catch (JSONException e){
                throw new RuntimeException(e);
            }
            object.synced = true;
            object.updatedAt = new Date();
            dao.update(object);
        }
    }

    protected void checkPermissions(AllowedOps.Op... required){
        for(AllowedOps.Op r : required) {
            if (!permissions.can(r)) {
                throw new RuntimeException("Permission " + r.toString() + " denied for " + dao.getDataClass().toString());
            }
        }
    }

    private T atomicCacheFetch(int id){
        synchronized (instanceCache){
            return instanceCache.get(id);
        }
    }

    private T atomicCachePut(T object){
        synchronized (instanceCache){
            instanceCache.put(object.getId(), object);
            return object;
        }
    }

    private T atomicCachePutIfMissing(T object){
        synchronized (instanceCache){
            T cached = instanceCache.get(object.getId());
            if(cached == null){
                instanceCache.put(object.getId(), object);
                return object;
            }else {
                return cached;
            }
        }
    }

    private T atomicCacheRemove(T object){
        synchronized (instanceCache){
            instanceCache.delete(object.getId());
            return object;
        }
    }

    /**
     * Handles running queries in background and callbacks on main thread
     * @param <A> the data type returned by the query
     */
    protected abstract static class SourceAsyncTask<A> implements Runnable {

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
}
