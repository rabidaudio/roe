package audio.rabid.dev.roe.models;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.j256.ormlite.dao.BaseDaoImpl;
import com.j256.ormlite.support.ConnectionSource;

import org.json.JSONException;
import org.json.JSONObject;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

import audio.rabid.dev.roe.ThreadHandler;
import audio.rabid.dev.roe.models.resource.Resource;

/**
 * Created by charles on 11/11/15.
 * <p/>
 * By using a NetworkSyncableDao, you get a guarantee that:
 * <p/>
 * 1. only one instance exists for a particular row item
 * 2. that network operations will be attempted in the background after the item is returned, and will
 * be cached for a later attempt if failed
 * 3. after a network operation completes, any observers will be notified on the main thread
 * <p/>
 * This will only work for sure with object-based queries. In particular, using {@link #executeRaw(String, String...)}
 * or {@link java.sql.PreparedStatement}s will likely skip the cache and network updates, causing undefined behavior.
 * <p/>
 * If you use {@link Database} as your {@link com.j256.ormlite.android.apptools.OrmLiteSqliteOpenHelper},
 * it will automatically initialize your Dao with the proper server (obtained from {@link Database#getServer(Class)}.
 * <p/>
 * Your data type must implement {@link Resource} and must also set `@DatabaseTable(daoClass = NetworkSyncableDao.class)`
 * (or your own subclass).
 * <p/>
 * By default, JSON and SQL exceptions while syncing will rethrow as {@link RuntimeException}s. To change this,
 * Override {@link #onSQLException(SQLException)} and {@link #onJSONException(JSONException)}.
 * <p/>
 * Since all Dao operations are blocking, you should run operations off the UI thread. You can use an {@link android.os.AsyncTask}
 * for this, or you can use the {@link #doAsync(Callable, OperationCallback)} provided by this library.
 */
public class NetworkSyncableDao<T extends Resource<LK, SK>, LK, SK> extends BaseDaoImpl<T, LK> implements CallbackReferenceObjectCache.OnCacheAddListener<T, LK> {

    private Map<T, TypedObservable<T>> observables = new WeakHashMap<>();

    private CollectionObservable collectionObservable = new CollectionObservable();

    private boolean massCollectionChange = false;

    private Server server;

    private Database database;

    public enum Op {
        READ,
        CREATE,
        UPDATE,
        DELETE
    }

    /**
     * We detect when an item is added to the cache, and when this happens we launch a show request to the server
     * to update it with the latest. The problem is when an item is created, it also gets added to the cache, causing
     * multiple calls for the same item. If the cache add completes before the network, the item will not have an SK
     * so it won't attempt a SHOW, but if for some reason the create executes fast enough, it might have an SK by the
     * time it gets there, so we don't want to launch a get in that case
     */
    private List<SK> alreadyUpToDate = new ArrayList<>();

    public NetworkSyncableDao(ConnectionSource connectionSource, Class<T> dataClass) throws SQLException {
        super(connectionSource, dataClass);
        CallbackReferenceObjectCache cache = new CallbackReferenceObjectCache(true);
        setObjectCache(cache);
        cache.setListener(this);
    }

    public void initialize(Server server, Database database) throws SQLException {
        this.server = server;
        this.database = database;
    }

    protected TypedObservable<T> getObservable(T item) {
        TypedObservable<T> o = observables.get(item);
        if (o == null) {
            o = new TypedObservable<>();
            observables.put(item, o);
        }
        return o;
    }

    /**
     * Listen to a particular instance of T
     */
    public void addObserver(TypedObserver<T> observer, T item) {
        getObservable(item).addObserver(observer);
    }

    public boolean hasObservers(T item){
        return observables.containsKey(item) && getObservable(item).countObservers()>0;
    }

    protected void notifyObservers(final T item, final boolean deleted) {
        if (hasObservers(item)) {
            getObservable(item).setChanged();
            ThreadHandler.postMain(new Runnable() {
                @Override
                public void run() {
                    getObservable(item).notifyObservers(item, deleted);
                }
            });
        }
    }

    public void addCollectionObserver(CollectionObserver collectionObserver){
        collectionObservable.addObserver(collectionObserver);
    }

    /**
     * This gets called when an item is created or destroyed, which is great for single items. But if
     * we create a group of items at once, e.g. {@link #getFromNetwork(String, JSONObject, OperationCallback)},
     * we want one update call for all of them. So for that we disable individual calls with
     * {@link #startMassCollectionChange()} which blocks each insertion from notifying until finally calling
     * {@link #completeMassCollectionChange()}.
     */
    protected void notifyCollectionObservers(){
        if(collectionObservable.countObservers()>0 && !massCollectionChange) {
            collectionObservable.setChanged();
            ThreadHandler.postMain(new Runnable() {
                @Override
                public void run() {
                    collectionObservable.notifyObservers();
                }
            });
        }
    }

    private synchronized void startMassCollectionChange(){
        massCollectionChange = true;
    }

    private synchronized void completeMassCollectionChange(){
        massCollectionChange = false;
        notifyCollectionObservers();
    }

    public boolean isNew(T item) throws SQLException {
        LK id = extractId(item);
        return id == null || !idExists(id);
    }

    @Override
    public void onCacheItemAdded(Class<T> rClass, LK id, T item) {
        if(!rClass.equals(dataClass)) return;
        //at this point we know this item has been added from the local database to the cache, which
        // means a network update has not occurred yet, so do that
        if (item.hasServerId() && !alreadyUpToDate.contains(item.getServerId())) {
            checkUpdatesNetwork(item);
            alreadyUpToDate.add(item.getServerId());
        }
        notifyCollectionObservers();
    }

    protected Future<Boolean> checkUpdatesNetwork(final T item) {
        return ThreadHandler.postBackground(new Callable<Boolean>() {
            @Override
            public Boolean call() {
                try {
                    JSONObject data = server.getItem(getDataClass(), item.getServerId());
                    synchronized (item) {
                        item.fromJSON(data);
                        getObservable(item).setChanged();
                        superUpdate(item);
                    }
                    return true;
                } catch (SQLException e) {
                    onSQLException(e);
                } catch (JSONException e) {
                    onJSONException(e);
                } catch (Server.NetworkException e) {
                    //well, the item is not necessarily up to date, but nothing we can do
                }
                return false;
            }
        });
    }

    public void onSQLException(SQLException e) {
        throw new RuntimeException(e);
    }

    public void onJSONException(JSONException e) {
        throw new RuntimeException(e);
    }

    @Override
    public void onCacheItemIdUpdated(Class<T> tClass, LK id, T data) {
        onUpdated(data);
    }

    @Override
    public void onCacheItemDeleted(Class<T> tClass, LK id, T data) {
        onDeleted(data);
        notifyCollectionObservers();
    }

    protected void onCreated(T item) {
        notifyObservers(item, false);
        createNetwork(item);
    }

    protected Future<Boolean> createNetwork(final T item) {
        return ThreadHandler.postBackground(new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                //if up-to-date, or a sync works, and now we're up to date
                if(!database.isSyncRequired() || database.syncWithServerSynchronous()){
                    //can create
                    synchronized (item) {
                        try {
                            JSONObject data = server.createItem(getDataClass(), item);
                            item.fromJSON(data);
                            superUpdate(item);
                            notifyObservers(item, false);
                            return true;
                        } catch (Server.NetworkException e) {
                            UnsyncedResource.create(database, NetworkSyncableDao.this, item, Op.CREATE);
                            return false;
                        }
                    }
                } else{
                    //still no network, save for later
                    UnsyncedResource.create(database, NetworkSyncableDao.this, item, Op.CREATE);
                    return false;
                }
            }
        });
    }

    protected void onUpdated(final T item) {
        notifyObservers(item, false);
        updateNetwork(item);
    }

    protected Future<Boolean> updateNetwork(final T item) {
        return ThreadHandler.postBackground(new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                //if up-to-date, or a sync works, and now we're up to date
                if(!database.isSyncRequired() || database.syncWithServerSynchronous()){
                    //can update
                    synchronized (item) {
                        try {
                            JSONObject data = server.updateItem(getDataClass(), item, String.valueOf(item.getServerId()));
                            item.fromJSON(data);
                            superUpdate(item);
                            notifyObservers(item, false);
                            return true;
                        } catch (Server.NetworkException e) {
                            UnsyncedResource.create(database, NetworkSyncableDao.this, item, Op.UPDATE);
                            return false;
                        }
                    }
                } else{
                    //still no network, save for later
                    UnsyncedResource.create(database, NetworkSyncableDao.this, item, Op.UPDATE);
                    return false;
                }
            }
        });
    }

    protected void onDeleted(T item) {
        notifyObservers(item, true);
        deleteNetwork(item);
    }

    protected Future<Boolean> deleteNetwork(final T item) {
        return ThreadHandler.postBackground(new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                //if up-to-date, or a sync works, and now we're up to date
                if(!database.isSyncRequired() || database.syncWithServerSynchronous()){
                    //can delete
                    synchronized (item) {
                        try {
                            server.deleteItem(getDataClass(), item, String.valueOf(item.getServerId()));
                            //no need to notify observers, to the client the thing is already gone
                            return true;
                        } catch (Server.NetworkException e) {
                            UnsyncedResource.create(database, NetworkSyncableDao.this, item, Op.DELETE);
                            return false;
                        }
                    }
                } else{
                    //still no network, save for later
                    UnsyncedResource.create(database, NetworkSyncableDao.this, item, Op.DELETE);
                    return false;
                }
            }
        });
    }

    @Override
    public int refresh(T item) throws SQLException {
        int result = super.refresh(item);
        notifyObservers(item, false);
        return result;
    }

    @Override
    public int create(T item) throws SQLException {
        if(item.hasServerId()) {
            alreadyUpToDate.add(item.getServerId());
        }
        int result = super.create(item); //this will cause cache insert, which will guarantee that
        // onCacheItemAdded will be called before this line completes
        onCreated(item);
        return result;
    }

    @Override
    public int delete(Collection<T> datas) throws SQLException {
        startMassCollectionChange();
        int result = super.delete(datas);
        completeMassCollectionChange();
        return result;
    }

    @Override
    public int deleteIds(Collection<LK> ids) throws SQLException {
        startMassCollectionChange();
        int result = super.deleteIds(ids);
        completeMassCollectionChange();
        return result;
    }

    @Override
    public int update(T item) throws SQLException {
        int result = super.update(item); // will not call any cache methods, because the item should already be in the cache
        onUpdated(item);
        return result;
    }

    // hack to route around our overridden listener update method to avoid circular calls
    private int superUpdate(T item) throws SQLException {
        return super.update(item);
    }

    private int superCreate(T item) throws SQLException {
        if(item.hasServerId()) {
            alreadyUpToDate.add(item.getServerId());
        }
        return super.create(item);
    }

    @Override
    public int updateId(T data, LK newId) throws SQLException {
        int result = super.updateId(data, newId);
        onUpdated(data);
        return result;
    }

    public T getByServerId(final String serverFieldName, final SK serverId) throws SQLException {
        List<T> results = queryForEq(serverFieldName, serverId);
        if(results.isEmpty()){
            //pull from network
            try {
                JSONObject data = server.getItem(getDataClass(), serverId);
                T newInstance = getDataClass().newInstance();
                newInstance.fromJSON(data);
                create(newInstance);
                return newInstance;
            }catch (Server.NetworkException e){
                //oh well
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            } catch (InstantiationException e) {
                throw new RuntimeException(e);
            } catch (JSONException e){
                onJSONException(e);
            }
            return null;
        }else{
            return results.get(0);
        }
    }

    @Override
    public void clearObjectCache() {
        alreadyUpToDate.clear();
        super.clearObjectCache();
    }

    //TODO for async things, could probably make a future with an attachable callback?

    public Future<T> queryForIdAsync(final LK localId, @Nullable OperationCallback<T> callback){
        return doAsync(new Callable<T>() {
            @Override
            public T call() throws Exception {
                return queryForId(localId);
            }
        }, callback);
    }

    public Future<List<T>> queryForAllAsync(@Nullable OperationCallback<List<T>> callback){
        return doAsync(new Callable<List<T>>() {
            @Override
            public List<T> call() throws Exception {
                return queryForAll();
            }
        },callback);
    }

    public Future<List<T>> queryForEqAsync(final String name, final Object value, @Nullable OperationCallback<List<T>> callback){
        return doAsync(new Callable<List<T>>() {
            @Override
            public List<T> call() throws Exception {
                return queryForEq(name, value);
            }
        },callback);
    }

    public Future<List<T>> queryForFieldValuesAsync(@NonNull final Map<String, Object> values, @Nullable OperationCallback<List<T>> callback) {
        return doAsync(new Callable<List<T>>() {
            @Override
            public List<T> call() throws Exception {
                return queryForFieldValues(values);
            }
        },callback);
    }

    public Future<T> saveAsync(final T item, @Nullable OperationCallback<T> callback){
        return doAsync(new Callable<T>() {
            @Override
            public T call() throws Exception {
                createOrUpdate(item);
                return item;
            }
        },callback);
    }

    public Future<T> deleteAsync(final T item, @Nullable OperationCallback<T> callback){
        return doAsync(new Callable<T>() {
            @Override
            public T call() throws Exception {
                delete(item);
                return item;
            }
        },callback);
    }

    public Future<Collection<T>> deleteAsync(final Collection<T> items, @Nullable OperationCallback<Collection<T>> callback){
        return doAsync(new Callable<Collection<T>>() {
            @Override
            public Collection<T> call() throws Exception {
                delete(items);
                return items;
            }
        },callback);
    }

    public Future<T> getByServerIdAsync(final String serverFieldName, final SK serverId, @Nullable OperationCallback<T> callback){
        return doAsync(new Callable<T>() {
            @Override
            public T call() throws Exception {
                return getByServerId(serverFieldName, serverId);
            }
        }, callback);
    }

    public Future<List<T>> getFromNetwork(final String serverFieldName, final JSONObject search, @Nullable OperationCallback<List<T>> callback){
        return doAsync(new Callable<List<T>>() {
            @Override
            public List<T> call() throws Exception {
                try {
                    List<JSONObject> data = server.getItems(getDataClass(), search);
                    List<T> results = new ArrayList<>(data.size());
                    startMassCollectionChange();
                    for (JSONObject o : data) {
                        //create a new instance from the data
                        T newInstance = getDataClass().newInstance();
                        newInstance.fromJSON(o);
                        if (alreadyUpToDate.contains(newInstance.getServerId())) {
                            //already up to date, so no worries
                            continue;
                        }
                        //look for an instance with the same serverId
                        List<T> current = queryForEq(serverFieldName, newInstance.getServerId());
                        if (current.isEmpty()) {
                            //if none exists, create a new one
                            create(newInstance);
                            results.add(newInstance);
                        } else {
                            T c = current.get(0);
                            c.fromJSON(o);
                            alreadyUpToDate.add(c.getServerId());
                            update(c);
                            results.add(c);
                            notifyObservers(c, false);
                        }
                    }
                    return results;
                } catch (Server.NetworkException e) {
                    //oh well, no network
                } catch (SQLException e) {
                    onSQLException(e);
                } catch (JSONException e) {
                    onJSONException(e);
                } finally {
                    completeMassCollectionChange();
                }
                return new ArrayList<>(0);
            }
        }, callback);
    }

    /**
     * This can be used for blocking query operations as an alternative to {@link android.os.AsyncTask}.
     * <p/>
     * Create a new instance, and in your call method, run your blocking dao operation. You can either
     * supply a {@link OperationCallback} to receive the result
     * on the main thread, or use the instance directly as a {@link Future}.
     */
    public static <T> Future<T> doAsync(final @NonNull Callable<T> callable, final @Nullable OperationCallback<T> callback) {
        return ThreadHandler.postBackground(new Callable<T>() {
            @Override
            public T call() throws Exception {
                final T result = callable.call();
                if (callback != null) {
                    ThreadHandler.postMain(new Runnable() {
                        @Override
                        public void run() {
                            callback.onResult(result);
                        }
                    });
                }
                return result;
            }
        });
    }

    public static Future doAsync(final @NonNull Runnable runnable){
        return ThreadHandler.postBackground(runnable);
    }

    public interface OperationCallback<T> {
        void onResult(T result);
    }
}