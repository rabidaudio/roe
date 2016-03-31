package audio.rabid.dev.roe;

import android.util.Pair;

import org.jdeferred.DeferredManager;
import org.jdeferred.Promise;
import org.jdeferred.impl.DeferredObject;

import java.util.concurrent.Callable;

import audio.rabid.dev.roe.models.cache.ResourceCache;

/**
 * Created by  charles  on 3/30/16.
 *
 * Simple handler for ISCUD logic.
 */
public class AManager {

    private DeferredManager manager;

    private ResourceCache cache;

    public AManager(DeferredManager manager, ResourceCache cache){
        this.manager = manager;
        this.cache = cache;
    }

    public <T> Promise<T, Throwable, Void> show(final AModel<T> model, final String key){
        if(cache.has(key)){
            T cacheResult = cache.get(key);
            return new DeferredObject<T, Throwable, Void>().resolve(cacheResult);
        }
        return manager.when(new Callable<T>() {
            @Override
            public T call() throws Exception {
                T networkModel = model.showNetwork(key);
                model.updateDatabase(key, networkModel);
                return cache.put(key, networkModel);
            }
        });
    }

    public <T> Promise<T, Throwable, Void> create(final AModel<T> model, final T object){
        return manager.when(new Callable<T>() {
            @Override
            public T call() throws Exception {
                Pair<String, T> result = model.createNetwork(object);
                model.updateDatabase(result.first, result.second);
                return cache.put(result.first, result.second);
            }
        });
    }

    public <T> Promise<T, Throwable, Void> update(final AModel<T> model, final String key, final T object){
        return manager.when(new Callable<T>() {
            @Override
            public T call() throws Exception {
                T newInstance = model.updateNetwork(key, object);
                model.updateDatabase(key, newInstance);
                return cache.put(key, newInstance);
            }
        });
    }

    public <T> Promise<Void, Throwable, Void> destroy(final AModel<T> model, final String key){
        return manager.when(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                model.deleteNetwork(key);
                model.deleteDatabase(key);
                cache.put(key, null);
                return null;
            }
        });
    }
}
