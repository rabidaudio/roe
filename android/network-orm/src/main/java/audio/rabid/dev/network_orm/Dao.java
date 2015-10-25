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
        (new SingleItemQuery<T>(callback){
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
        (new SingleItemQuery<T>(callback){
            @Override
            protected T doInBackground(Void... params) {
                List<T> results = queryForEq(field, value);
                return results.size() > 0 ? results.get(0) : null;
            }
        }).execute();
    }

    public void all(MultipleQueryCallback<T> callback){
        (new MultipleItemQuery<T>(callback){
            @Override
            protected List<T> doInBackground(Void... params) {
                return queryForAll();
            }
        }).execute();
    }

    public void unSynced(MultipleQueryCallback<T> callback){
        (new MultipleItemQuery<T>(callback){
            @Override
            protected List<T> doInBackground(Void... params) {
                return queryForEq("synced", false);
            }
        }).execute();
    }

    public void save(final T object, @Nullable SingleQueryCallback<T> callback){
        AllowedOps a = object.getAllowedOps();
        if(object.isNew() && !a.canCreate()){
            throw new RuntimeException("Create operation not allowed on class "+object.getClass().getSimpleName());
        }else if(!object.isNew() && !a.canUpdate()){
            throw new RuntimeException("Update operation not allowed on class "+object.getClass().getSimpleName());
        }else {
            (new SingleItemQuery<T>(callback) {
                @Override
                protected T doInBackground(Void... params) {
                    createOrUpdate(object);
                    return object;
                }
            }).execute();
        }
    }

    public void delete(final T object, @Nullable SingleQueryCallback<T> callback){
        if(!object.getAllowedOps().canDelete()){
            throw new RuntimeException("Delete operation not allowed on class "+object.getClass().getSimpleName());
        }else {
            (new SingleItemQuery<T>(callback) {
                @Override
                protected T doInBackground(Void... params) {
                    delete(object);
                    return object;
                }
            }).execute();
        }
    }

    public void customSingleQuery(final CustomSingleQuery<T> callbacks){
        (new SingleItemQuery<T>(callbacks){
            @Override
            protected T doInBackground(Void... params) {
                return callbacks.executeQuery(Dao.this);
            }
        }).execute();
    }

    public void customMultipleQuery(final CustomMultipleQuery<T> callbacks){
        (new MultipleItemQuery<T>(callbacks){
            @Override
            protected List<T> doInBackground(Void... params) {
                return callbacks.executeQuery(Dao.this);
            }
        }).execute();
    }

    /***************************************************************/

    private static abstract class SingleItemQuery<V> extends AsyncTask<Void, Void, V> {
        private SingleQueryCallback<V> callback;
        public SingleItemQuery(@Nullable SingleQueryCallback<V> callback){
            this.callback = callback;
        }

        @Override
        protected void onPostExecute(V result){
            if(callback!=null){
                callback.onResult(result);
            }
        }
    }

    private static abstract class MultipleItemQuery<V> extends AsyncTask<Void, Void, List<V>> {
        private MultipleQueryCallback<V> callback;
        public MultipleItemQuery(@Nullable MultipleQueryCallback<V> callback){
            this.callback = callback;
        }

        @Override
        protected void onPostExecute(List<V> results){
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

    public interface CustomSingleQuery<Q extends Resource> extends SingleQueryCallback<Q> {
        Q executeQuery(Dao<Q> dao);
    }

    public interface CustomMultipleQuery<Q extends Resource> extends MultipleQueryCallback<Q> {
        List<Q> executeQuery(Dao<Q> dao);
    }
}
