package audio.rabid.dev.roe.models.cache;

import android.util.SparseArray;

import audio.rabid.dev.roe.models.Resource;

/**
 * Created by charles on 11/4/15.
 */
public class SparseArrayResourceCache<T extends Resource> implements ResourceCache<T> {

    private final SparseArray<T> instanceCache;

    public SparseArrayResourceCache(int size){
        instanceCache = new SparseArray<>(size);
    }

    public SparseArrayResourceCache(){
        this(50);
    }

    protected SparseArray<T> getInstanceCache(){
        return instanceCache;
    }

    @Override
    public synchronized T put(T object) {
        if(object.getId() == null) return null;
        if(instanceCache.get(object.getId())!=null){
            throw new RuntimeException("Tried to double-cache "+object.toString());
        }
        instanceCache.put(object.getId(), object);
        return object;
    }

    @Override
    public synchronized T putIfMissing(T object) {
        if(object.getId() == null) return null;
        return instanceCache.get(object.getId(), object);
    }

    @Override
    public synchronized T delete(T object) {
        if(object.getId() != null) {
            instanceCache.delete(object.getId());
        }
        return object;
    }

    @Override
    public synchronized CacheResult<T> get(int id, CacheMissCallback<T> cacheMissCallback) {
        T cached = instanceCache.get(id);
        if(cached == null){
            cached = cacheMissCallback.onCacheMiss(id);
            if(cached!=null && cached.getId()!=null) {
                instanceCache.put(cached.getId(), cached);
            }
            return new CacheResult<>(cached, false);
        }else{
            return new CacheResult<>(cached, true);
        }
    }
}
