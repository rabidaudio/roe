package audio.rabid.dev.roe.models.cache;

import android.support.annotation.Nullable;

/**
 * Created by charles on 10/29/15.
 * <p/>
 * An object cache for keeping track of Resources (either by localId or serverId). You should make
 * your cache thread-safe by making all your methods synchronized for atomicity.
 */
public interface ResourceCache<R, LK> {

    R put(R object);

//    R putIfMissing(R object);

    R delete(R object);

    void clear();

    int size();

    CacheResult<R> get(LK id, CacheMissCallback<R, LK> cacheMissCallback);

    /**
     * This block is called if the item wasn't in the cache. Return an item to be put in the cache
     * in its place.
     *
     * @param <T>
     */
    interface CacheMissCallback<T, ID> {
        @Nullable
        T onCacheMiss(ID id);
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