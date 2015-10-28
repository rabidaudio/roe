package audio.rabid.dev.network_orm;

import android.util.SparseArray;

import com.j256.ormlite.dao.Dao;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.JSONException;
import org.json.JSONObject;

import java.sql.SQLException;
import java.util.ArrayList;
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
    private ResourceCreator<T> resourceCreator;

    private final SparseArray<T> instanceCache = new SparseArray<>(50);

    public Source(@NotNull Server server, @NotNull Dao<T, Integer> dao, @NotNull String endpoint,
                  @NotNull ResourceCreator<T> resourceCreator){
        this.server = server;
        this.dao = dao;
        this.endpoint = endpoint;
        this.resourceCreator = resourceCreator;
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
        (new SourceAsyncTask<List<T>>(callback){
            @Override
            protected List<T> runInBackground() {
                try {
                    Server.Response r = server.index(endpoint, payload);
                    if(r.wasError()) {
                        return null;
                    }else{
                        List<T> newInstances = resourceCreator.createArrayFromJSON(r.getResponseBody());
                        ArrayList<T> returnValues = new ArrayList<T>(newInstances.size());
                        for(T n : newInstances){
                            List<T> fromServerId = dao.queryForEq("server_id", n.getServerId());
                            if(fromServerId != null && !fromServerId.isEmpty()){
                                //update
                                T existing = atomicCachePutIfMissing(fromServerId.get(0));
                                resourceCreator.copyFromNew(existing, n);
                                existing.synced = true;
                                dao.update(existing);
                                returnValues.add(existing);
                            }else{
                                //create
                                n.synced = true;
                                dao.create(n);
                                returnValues.add(atomicCachePut(n));
                            }
                        }
                        return returnValues;
                    }
                }catch (Server.NetworkException e){
                    //oh no, no items...
                    return null;
                }catch (SQLException | JSONException e){
                    throw new RuntimeException(e);
                }
            }
        }).execute();
    }

    public void createOrUpdate(T object, @Nullable QueryCallback<T> callback){
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
        (new SourceAsyncTask<T>(callback){
            @Override
            protected T runInBackground() {
                try {
                    dao.create(object);
                    try{
                        Server.Response r = server.create(endpoint, object);
                        updateFromResponse(object, r);
                    }catch (Server.NetworkException e){
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
        (new SourceAsyncTask<T>(callback){
            @Override
            protected T runInBackground() {
                synchronized (object) { //don't let two update operations happen simultaneously for same object
                    try {
                        dao.update(object);
                        try {
                            Server.Response r = server.update(endpoint, object);
                            updateFromResponse(object, r);
                        } catch (Server.NetworkException e) {
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
        (new SourceAsyncTask<T>(callback){
            @Override
            protected T runInBackground() {
                synchronized (object) { //don't let two delete operations happen simultaneously for same object
                    if (deleteRemotely) {
                        try {
                            server.destroy(endpoint, object);
                        } catch (Server.NetworkException e) {
                            //TODO what do we do if remote delete fails?
                        }
                    }
                    try {
                        dao.delete(object);
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
                            Server.Response r = server.update(endpoint, u);
                            updateFromResponse(u, r);
                        } catch (Server.NetworkException e) {
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
                        boolean changed = item.updateFromJSON(r.getResponseBody()); //update values
                        if(changed) {
                            try {
                                item.synced = true;
                                dao.update(item); //save changes to database
                            } catch (SQLException e) {
                                //just put in cache as it is
                            }
                        }
                    }
                }catch (Server.NetworkException e){
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
                T newInstance = resourceCreator.createFromJSON(r.getResponseBody());
                //save local
                newInstance.synced = true;
                dao.create(newInstance);
                //store in cache
                return atomicCachePut(newInstance);
            }else{
                return null;
            }
        }catch (Server.NetworkException e){
            return null;
        }catch (SQLException | JSONException e){
            throw new RuntimeException(e);
        }
    }

    protected void updateFromResponse(T object, Server.Response r) throws SQLException {
        if (!r.wasError()) {
            object.updateFromJSON(r.getResponseBody());
            object.synced = true;
            dao.update(object);
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
}
