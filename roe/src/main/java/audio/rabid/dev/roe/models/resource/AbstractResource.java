package audio.rabid.dev.roe.models.resource;

import android.support.annotation.Nullable;

import com.j256.ormlite.dao.Dao;

import org.jdeferred.Promise;
import org.jdeferred.android.DeferredAsyncTask;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import audio.rabid.dev.roe.Manager;
import audio.rabid.dev.roe.Operation;

/**
 * Created by  charles  on 3/15/16.
 */
public abstract class AbstractResource<T> implements Resource<T> {

    private Manager manager;

    public void setManager(Manager manager){
        this.manager = manager;
    }

    @Override
    public boolean allowsMethod(Operation operation) {
        return true;
    }

    @Override
    public <ID> Promise<T, Throwable, Void> get(final ID id) {
        return execute(new Callable<T>() {
            @Override
            public T call() throws Exception {
                List<T> results = getDao().queryForEq(getIdentificationAttribute(), id);
                return results.size() > 0 ? results.get(0) : null;
            }
        });
    }

    @Override
    public Promise<List<T>, Throwable, Void> getObjectsLocal(final Map<String, Object> query) {
        return execute(new Callable<List<T>>() {
            @Override
            public List<T> call() throws Exception {
                return getDao().queryForFieldValuesArgs(query);
            }
        });
    }

    @Override
    public Promise<List<T>, Throwable, Void> getObjectsRemote(Map<String, Object> query) {
        return execute(new Callable<List<T>>() {
            @Override
            public List<T> call() throws Exception {
                throw new RuntimeException("oh shit");
            }
        });
    }

    @Override
    public Promise<T, Throwable, Void> create(final T object) {
        return execute(new Callable<T>() {
            @Override
            public T call() throws Exception {
                getDao().create(object);
                return object;
            }
        });
    }


    @Override
    public Promise<T, Throwable, Void> update(final T object) {
        return execute(new Callable<T>() {
            @Override
            public T call() throws Exception {
                getDao().update(object);
                return object;
            }
        });
    }

    @Override
    public Promise<T, Throwable, Void> delete(final T object) {
        return execute(new Callable<T>() {
            @Override
            public T call() throws Exception {
                getDao().delete(object);
                return object;
            }
        });
    }

    private <ID> Dao<T, ID> getDao() throws SQLException {
        return manager.getDao(getModelClass());
    }

    private <A> Promise<A, Throwable, Void> execute(final Callable<A> callable){
        DeferredAsyncTask<Void, Void, A> t = new DeferredAsyncTask<Void, Void, A>() {
            @Override
            protected A doInBackgroundSafe(Void... params) throws Exception {
                return callable.call();
            }
        };
        t.execute();
        return manager.deferredManager.when(t.promise());
    }
}
