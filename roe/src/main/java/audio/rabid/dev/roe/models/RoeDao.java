package audio.rabid.dev.roe.models;

import com.j256.ormlite.dao.BaseDaoImpl;
import com.j256.ormlite.support.ConnectionSource;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import audio.rabid.dev.roe.BackgroundThread;

/**
 * Created by charles on 11/10/15.
 * <p/>
 * A Dao which adds support for observers, and uses a custom resource cache for getting callbacks when
 * an item is added to the cache.
 */
public class RoeDao<T, ID> extends BaseDaoImpl<T, ID> implements CallbackReferenceObjectCache.OnCacheAddListener<T, ID> {

    private Map<T, TypedObservable<T>> observers = new WeakHashMap<>();

    public RoeDao(ConnectionSource connectionSource, Class<T> dataClass) throws SQLException {
        super(connectionSource, dataClass);
        CallbackReferenceObjectCache objectCache = new CallbackReferenceObjectCache(true);
        setObjectCache(objectCache);
        objectCache.setListener(this);

    }

    protected TypedObservable<T> getObservable(T item) {
        TypedObservable<T> o = observers.get(item);
        if (o == null) {
            o = new TypedObservable<>();
            observers.put(item, o);
        }
        return o;
    }

    public void addObserver(T item, TypedObserver<T> observer) {
        getObservable(item).addObserver(observer);
    }

    protected void notifyObservers(final T item, final boolean deleted) {
        BackgroundThread.postMain(new Runnable() {
            @Override
            public void run() {
                getObservable(item).notifyObservers(item, deleted);
            }
        });
    }

    @Override
    public void onCacheItemAdded(Class<T> rClass, ID id, T data) {

    }

    @Override
    public void onCacheItemIdUpdated(Class<T> tClass, ID id, T data) {
        onUpdated(data);
    }

    @Override
    public void onCacheItemDeleted(Class<T> tClass, ID id, T data) {
        onDeleted(data);
    }

    protected void onCreated(T item) {
        getObservable(item).setChanged();
        notifyObservers(item, false);
    }

    protected void onUpdated(T item) {
        getObservable(item).setChanged();
        notifyObservers(item, false);
    }

    protected void onDeleted(T item) {
        getObservable(item).setChanged();
        notifyObservers(item, true);
    }

    @Override
    public int refresh(T item) throws SQLException {
        int result = super.refresh(item);
        getObservable(item).setChanged();
        notifyObservers(item, false);
        return result;
    }

    @Override
    public int create(T item) throws SQLException {
        int result = super.create(item);
        onCreated(item);
        return result;
    }

    @Override
    public int update(T item) throws SQLException {
        int result = super.update(item);
        onUpdated(item);
        return result;
    }

    @Override
    public int updateId(T data, ID newId) throws SQLException {
        int result = super.updateId(data, newId);
        onUpdated(data);
        return result;
    }

    @Override
    public int delete(T item) throws SQLException {
        int result = super.delete(item);
        onDeleted(item);
        return result;
    }

    @Override
    public int deleteById(ID id) throws SQLException {
        T item = getObjectCache().get(getDataClass(), id);
        int result = super.deleteById(id);
        if (item != null) { //otherwise, if it wasn't in the cache there shouldn't be any observers
            onDeleted(item);
        }
        return result;
    }

    @Override
    public int delete(Collection<T> datas) throws SQLException {
        int result = super.delete(datas);
        for (T item : datas) {
            onDeleted(item);
        }
        return result;
    }

    @Override
    public int deleteIds(Collection<ID> ids) throws SQLException {
        List<T> deleted = new ArrayList<>(ids.size());
        for (ID id : ids) {
            T item = getObjectCache().get(getDataClass(), id);
            if (item != null) {
                deleted.add(item);
            }
        }
        int result = super.deleteIds(ids);
        for (T item : deleted) {
            onDeleted(item);
        }
        return result;
    }
}
