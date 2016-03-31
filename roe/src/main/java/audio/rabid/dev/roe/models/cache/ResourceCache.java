package audio.rabid.dev.roe.models.cache;

import android.support.annotation.Nullable;

/**
 * Created by charles on 10/29/15.
 * <p/>
 * An object cache for keeping track of Resources (either by localId or serverId). You should make
 * your cache thread-safe by making all your methods synchronized for atomicity.
 */
public interface ResourceCache {

    <T> T put(String key, T object);

//    R putIfMissing(R object);

    <T> T delete(String key, T object);

    boolean has(String key);

    void clear();

    int size();

    <T> T get(String id);

    <T> CacheResult<T> get(String id, CacheMissCallback<T> cacheMissCallback);

    /**
     * This block is called if the item wasn't in the cache. Return an item to be put in the cache
     * in its place.
     *
     * @param <T>
     */
    interface CacheMissCallback<T> {
        @Nullable
        T onCacheMiss(String id);
    }

    class CacheResult<T> {

        private T item;
        private boolean wasInCache;

        public CacheResult(T item, boolean wasInCache) {
            this.item = item;
            this.wasInCache = wasInCache;
        }

        public T getItem(){
            return item;
        }

        public boolean wasInCache(){
            return wasInCache;
        }
    }
}