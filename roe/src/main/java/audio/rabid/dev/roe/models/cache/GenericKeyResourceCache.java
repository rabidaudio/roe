package audio.rabid.dev.roe.models.cache;

import java.util.HashMap;

import audio.rabid.dev.roe.models.Resource;

/**
 * Created by charles on 11/5/15.
 */
public class GenericKeyResourceCache<R extends Resource<R, LK>, LK> implements ResourceCache<R, LK> {

    private HashMap<LK, R> instanceCache;

    public GenericKeyResourceCache(int initialSize) {
        instanceCache = new HashMap<>(initialSize);
    }

    public GenericKeyResourceCache() {
        this(50);
    }

    protected HashMap<LK, R> getInstanceCache() {
        return instanceCache;
    }

    @Override
    public synchronized R put(R object) {
        if (object.getId() == null) return null;
        if (instanceCache.get(object.getId()) != null) {
            throw new RuntimeException("Tried to double-cache " + object.toString());
        }
        instanceCache.put(object.getId(), object);
        return object;
    }

    @Override
    public synchronized R putIfMissing(R object) {
        if (object.getId() == null) return null;
        R cached = instanceCache.get(object.getId());
        if (cached == null) {
            return put(object);
        } else {
            return cached;
        }
    }

    @Override
    public synchronized R delete(R object) {
        if (object.getId() != null) {
            instanceCache.remove(object.getId());
        }
        return object;
    }

    @Override
    public synchronized CacheResult<R> get(LK id, CacheMissCallback<R, LK> cacheMissCallback) {
        R cached = instanceCache.get(id);
        if (cached == null) {
            cached = cacheMissCallback.onCacheMiss(id);
            if (cached != null && cached.getId() != null) {
                instanceCache.put(cached.getId(), cached);
            }
            return new CacheResult<>(cached, false);
        } else {
            return new CacheResult<>(cached, true);
        }
    }
}
