//package audio.rabid.dev.roe.models.cache;
//
//import android.util.SparseArray;
//
//import audio.rabid.dev.roe.models.Resource;
//
///**
// * Created by charles on 11/4/15.
// */
//public class IntegerKeyResourceCache<R extends Resource<Integer>> implements ResourceCache<R, Integer> {
//
//    private final SparseArray<R> instanceCache;
//
//    public IntegerKeyResourceCache(int size) {
//        instanceCache = new SparseArray<>(size);
//    }
//
//    public IntegerKeyResourceCache() {
//        this(50);
//    }
//
//    protected SparseArray<R> getInstanceCache() {
//        return instanceCache;
//    }
//
//    @Override
//    public synchronized R put(R object) {
//        if(object.getId() == null) return null;
//        if(instanceCache.get(object.getId())!=null){
//            throw new RuntimeException("Tried to double-cache "+object.toString());
//        }
//        instanceCache.put(object.getId(), object);
//        return object;
//    }
//
//    @Override
//    public synchronized R putIfMissing(R object) {
//        if(object.getId() == null) return null;
//        R cached = instanceCache.get(object.getId());
//        if(cached == null){
//            return put(object);
//        }else{
//            return cached;
//        }
//    }
//
//    @Override
//    public synchronized R delete(R object) {
//        if(object.getId() != null) {
//            instanceCache.delete(object.getId());
//        }
//        return object;
//    }
//
//    @Override
//    public synchronized CacheResult<R> get(Integer id, CacheMissCallback<R, Integer> cacheMissCallback) {
//        R cached = instanceCache.get(id);
//        if(cached == null){
//            cached = cacheMissCallback.onCacheMiss(id);
//            if(cached!=null && cached.getId()!=null) {
//                instanceCache.put(cached.getId(), cached);
//            }
//            return new CacheResult<>(cached, false);
//        }else{
//            return new CacheResult<>(cached, true);
//        }
//    }
//}
