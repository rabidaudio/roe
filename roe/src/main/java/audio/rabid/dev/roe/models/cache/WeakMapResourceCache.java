package audio.rabid.dev.roe.models.cache;

import java.lang.ref.WeakReference;
import java.util.HashMap;

import audio.rabid.dev.roe.models.Resource;

/**
 * Created by charles on 11/5/15.
 */
public class WeakMapResourceCache<R extends Resource<LK>, LK> implements ResourceCache<R, LK> {

    private HashMap<LK, WeakReference<R>> instanceCache;

    public WeakMapResourceCache(int initialSize) {
        instanceCache = new HashMap<>(initialSize);
    }

    public WeakMapResourceCache() {
        this(50);
    }

    protected HashMap<LK, WeakReference<R>> getInstanceCache() {
        return instanceCache;
    }

    @Override
    public synchronized R put(R object) {
        if (object.getId() == null) return null;
        if (instanceCache.get(object.getId()) != null) {
            throw new RuntimeException("Tried to double-cache " + object.toString());
        }
        instanceCache.put(object.getId(), new WeakReference<>(object));
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
    public synchronized R delete(R object) {
        if (object.getId() != null) {
            instanceCache.remove(object.getId());
        }
        return object;
    }

    @Override
    public synchronized CacheResult<R> get(LK id, CacheMissCallback<R, LK> cacheMissCallback) {
        WeakReference<R> ref = instanceCache.get(id);
        if (ref == null) {
            R cached = cacheMissCallback.onCacheMiss(id);
            if (cached != null && cached.getId() != null) {
                instanceCache.put(cached.getId(), new WeakReference<>(cached));
            }
            return new CacheResult<>(cached, false);
        } else {
            return new CacheResult<>(ref.get(), true);
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
