package audio.rabid.dev.roe.models.cache;

import java.lang.ref.WeakReference;
import java.util.HashMap;

/**
 * Created by charles on 11/5/15.
 */
public class WeakMapResourceCache implements ResourceCache {

    private HashMap<String, WeakReference<Object>> instanceCache;

    public WeakMapResourceCache(int initialSize) {
        instanceCache = new HashMap<>(initialSize);
    }

    public WeakMapResourceCache() {
        this(50);
    }

    protected HashMap<String, WeakReference<Object>> getInstanceCache() {
        return instanceCache;
    }

    @Override
    public synchronized <T> T put(String key, T object) {
        if (instanceCache.get(key) != null) {
            throw new RuntimeException("Tried to double-cache " + object.toString());
        }
        instanceCache.put(key, new WeakReference<Object>(object));
        return object;
    }

//    @Override
//    public synchronized R putIfMissing(R object) {
//        if (object.getId() == null) return null;
//        WeakReference<R> ref = instanceCache.get(object.getId());
//        if (ref == null) {
//            return put(object);
//        } else {
//            return ref.get();
//        }
//    }

    @Override
    public synchronized <T> T delete(String key, T object) {
        if (key != null) {
            instanceCache.remove(key);
        }
        return object;
    }

    @Override
    public boolean has(String key) {
        return instanceCache.containsKey(key);
    }

    @SuppressWarnings("unchecked")
    @Override
    public synchronized <T> T get(String key){
        WeakReference<Object> ref = instanceCache.get(key);
        if (ref == null) {
            return null;
        } else {
            return (T) ref.get();
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public synchronized <T> CacheResult<T> get(String key, CacheMissCallback<T> cacheMissCallback) {
        WeakReference<Object> ref = instanceCache.get(key);
        if (ref == null) {
            T cached = cacheMissCallback.onCacheMiss(key);
            if (cached != null && key != null) {
                instanceCache.put(key, new WeakReference<Object>(cached));
            }
            return new CacheResult<>(cached, false);
        } else {
            return new CacheResult<>((T) ref.get(), true);
        }
    }

    @Override
    public synchronized void clear(){
        getInstanceCache().clear();
    }

    @Override
    public synchronized int size(){
        return getInstanceCache().size();
    }
}
