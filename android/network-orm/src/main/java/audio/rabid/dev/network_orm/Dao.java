package audio.rabid.dev.network_orm;

import android.os.AsyncTask;
import android.util.Log;
import android.util.LruCache;

import com.j256.ormlite.dao.RuntimeExceptionDao;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Created by charles on 10/23/15.
 */
public class Dao<T extends Resource> extends RuntimeExceptionDao<T, Integer> {

    private final LruCache<Integer, T> instanceCache = new LruCache<Integer, T>(50); //TODO what limit?

    public Dao(com.j256.ormlite.dao.Dao<T, Integer> dao) {
        super(dao);
    }

    public void findByLocalId(final int id, SingleQueryCallback<T> callback){
        (new SingleItemQuery(callback){
            @Override
            protected T doInBackground(Void... params) {
                T cached = instanceCache.get(id);
                if(cached != null){
                    return cached;
                }else {
                    return queryForId(id);
                }
            }
        }).execute();
    }

    public void findByRemoteId(final int serverId, SingleQueryCallback<T> callback){
        findBy("serverId", serverId, callback);
    }

    public void findBy(final String field, final Object value, SingleQueryCallback<T> callback){
        (new SingleItemQuery(callback){
            @Override
            protected T doInBackground(Void... params) {
                List<T> results = checkCacheForElements(queryForEq(field, value));
                return results.size() > 0 ? results.get(0) : null;
            }
        }).execute();
    }

    public void all(MultipleQueryCallback<T> callback){
        (new MultipleItemQuery(callback){
            @Override
            protected List<T> doInBackground(Void... params) {
                return checkCacheForElements(queryForAll());
            }
        }).execute();
    }

    public void unSynced(MultipleQueryCallback<T> callback){
        (new MultipleItemQuery(callback){
            @Override
            protected List<T> doInBackground(Void... params) {
                return checkCacheForElements(queryForEq("synced", false));
            }
        }).execute();
    }

    public void save(final T object, @Nullable SingleQueryCallback<T> callback){
        (new SingleItemQuery(callback){
            @Override
            protected T doInBackground(Void... params) {
                createOrUpdate(object);
                return checkCacheForElement(object);
            }
        }).execute();
    }

    public void delete(final T object, @Nullable SingleQueryCallback<T> callback){
        (new SingleItemQuery(callback){
            @Override
            protected T doInBackground(Void... params) {
                instanceCache.remove(object.getId());
                delete(object);
                return object;
            }
        }).execute();
    }

    /**
     * This method checks the cache, and if an instance already exists with that id, it returns
     * the existing instance instead of the passed one. Otherwise it caches the passed one and
     * returns it.
     */
    private T checkCacheForElement(T element){
        synchronized (instanceCache) {
            T cached = instanceCache.get(element.getId());
            if (cached != null) {
                //used the cached version so instances are shared
                Log.d("Cache", "cached version "+cached.toString()+" used for " + element.toString());
                return cached;
            } else {
                //add the instance to the cache and return it
                instanceCache.put(element.getId(), element);
                Log.d("Cache", "added to cache" + element.toString());
                return element;
            }
        }
    }

    private List<T> checkCacheForElements(List<T> elements){
        ArrayList<T> copy = new ArrayList<>(elements.size());
        for (T element : elements) {
            copy.add(checkCacheForElement(element));
        }
        return copy;
    }

    /***************************************************************/

    private abstract class SingleItemQuery extends AsyncTask<Void, Void, T> {

        private SingleQueryCallback<T> callback;
        public SingleItemQuery(@Nullable SingleQueryCallback<T> callback){
            this.callback = callback;
        }

        @Override
        protected void onPostExecute(T result){
            if(callback!=null){
                callback.onResult(checkCacheForElement(result));
            }
        }
    }

    private abstract class MultipleItemQuery extends AsyncTask<Void, Void, List<T>> {

        private MultipleQueryCallback<T> callback;
        public MultipleItemQuery(@Nullable MultipleQueryCallback<T> callback){
            this.callback = callback;
        }

        @Override
        protected void onPostExecute(List<T> results){
            if(callback!=null){
                callback.onResult(results);
            }
        }
    }

    public interface SingleQueryCallback<Q> {
        void onResult(Q result);
    }

    public interface MultipleQueryCallback<Q> {
        void onResult(List<Q> results);
    }
}
