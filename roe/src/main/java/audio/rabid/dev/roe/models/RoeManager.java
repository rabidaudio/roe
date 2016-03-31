package audio.rabid.dev.roe.models;

import android.util.Pair;

import org.jdeferred.DeferredManager;
import org.jdeferred.Promise;
import org.jdeferred.impl.DeferredObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import audio.rabid.dev.roe.models.cache.ResourceCache;
import audio.rabid.dev.roe.models.network.NetworkException;

/**
 * Created by  charles  on 3/30/16.
 *
 * Simple handler for ISCUD logic.
 */
public class RoeManager {

    private DeferredManager manager;

    private ResourceCache cache;

    private Map<Class, Model> models = new HashMap<>();

    public RoeManager(DeferredManager manager, ResourceCache cache){
        this.manager = manager;
        this.cache = cache;
    }

    public <T, ID> void addModel(Class<T> tClass, Model<T, ID> model){
        models.put(tClass, model);
    }

    @SuppressWarnings("unchecked")
    public <T, ID> Model<T, ID> getModel(Class<T> tClass){
        return models.get(tClass);
    }

    public <T, ID> Promise<T, Throwable, Void> find(final Class<T> tClass, final ID key){
        if(cache.has(String.valueOf(key))){
            T cacheResult = cache.get(String.valueOf(key));
            return new DeferredObject<T, Throwable, Void>().resolve(cacheResult);
        }
        return manager.when(new Callable<T>() {
            @Override
            public T call() throws Exception {
                Model<T, ID> model = getModel(tClass);
                try {
                    T item = model.find(key);
                    model.saveToPersistentStorage(model.getID(item), item);
                    return cache.put(String.valueOf(key), item);
                }catch (NetworkException e){
                    // Network source unavailable
                    T item = model.findInPersistentStorage(key);
                    return cache.put(String.valueOf(key), item);
                }
            }
        });
    }

    @SuppressWarnings("unchecked")
    public <T, ID> Promise<T, Throwable, Void> create(final T object){
        return manager.when(new Callable<T>() {
            @Override
            public T call() throws Exception {
                Model<T, ID> model = getModel((Class<T>) object.getClass());
                T result = model.create(object); //allow to throw if network unavailable
                ID id = model.getID(result);
                model.saveToPersistentStorage(id, result);
                return cache.put(String.valueOf(id), result);
            }
        });
    }

    @SuppressWarnings("unchecked")
    public <T, ID> Promise<T, Throwable, Void> update(final ID key, final T object){
        return manager.when(new Callable<T>() {
            @Override
            public T call() throws Exception {
                Model<T, ID> model = getModel((Class<T>) object.getClass());
                T newInstance = model.update(key, object); //allow to throw if network unavailable
                return cache.put(String.valueOf(key), newInstance);
            }
        });
    }

    public <T, ID> Promise<Void, Throwable, Void> destroy(final Class<T> tClass, final T object){
        return manager.when(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                Model<T, ID> model = getModel(tClass);
                ID id = model.getID(object);
                model.delete(object); //allow to throw if network unavailable
                cache.put(String.valueOf(id), null);
                return null;
            }
        });
    }

    // queries skip the cache, because we don't know which results will match
    public <T, ID> Promise<List<T>, Throwable, Void> query(final Class<T> tClass, final Map<String, String> query){
        return manager.when(new Callable<List<T>>() {
            @Override
            public List<T> call() throws Exception {
                Model<T, ID> model = getModel(tClass);
                List<T> results;
                try {
                    results = model.query(query);
                }catch (NetworkException e){
                    // Network unavailable
                    results = model.queryPersistentStorage(query);
                }
                for (T result : results) {
                    cache.put(String.valueOf(model.getID(result)), result);
                }
                return results;
            }
        });
    }
}