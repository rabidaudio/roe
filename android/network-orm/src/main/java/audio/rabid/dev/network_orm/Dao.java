package audio.rabid.dev.network_orm;

import android.os.AsyncTask;

import com.j256.ormlite.dao.RuntimeExceptionDao;

import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Created by charles on 10/23/15.
 */
public class Dao<T extends Resource> extends RuntimeExceptionDao<T, Integer> {

    public Dao(com.j256.ormlite.dao.Dao<T, Integer> dao) {
        super(dao);
        setObjectCache(true);
    }

    public void findByLocalId(final int id, SingleQueryCallback<T> callback){
        (new SingleItemQuery(callback){
            @Override
            protected T doInBackground(Void... params) {
                return queryForId(id);
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
                List<T> results = queryForEq(field, value);
                return results.size() > 0 ? results.get(0) : null;
            }
        }).execute();
    }

    public void all(MultipleQueryCallback<T> callback){
        (new MultipleItemQuery(callback){
            @Override
            protected List<T> doInBackground(Void... params) {
                return queryForAll();
            }
        }).execute();
    }

    public void unSynced(MultipleQueryCallback<T> callback){
        (new MultipleItemQuery(callback){
            @Override
            protected List<T> doInBackground(Void... params) {
                return queryForEq("synced", false);
            }
        }).execute();
    }

    public void save(final T object, @Nullable SingleQueryCallback<T> callback){
        (new SingleItemQuery(callback){
            @Override
            protected T doInBackground(Void... params) {
                createOrUpdate(object);
                return object;
            }
        }).execute();
    }

    public void delete(final T object, @Nullable SingleQueryCallback<T> callback){
        (new SingleItemQuery(callback){
            @Override
            protected T doInBackground(Void... params) {
                delete(object);
                return object;
            }
        }).execute();
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
                callback.onResult(result);
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
