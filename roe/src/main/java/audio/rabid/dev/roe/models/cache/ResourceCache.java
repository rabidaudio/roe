package audio.rabid.dev.roe.models.cache;

import android.support.annotation.Nullable;
import android.util.Pair;

import audio.rabid.dev.roe.models.Resource;

/**
 * Created by charles on 10/29/15.
 * <p/>
 * An object cache for keeping track of Resources (either by localId or serverId). You should make
 * your cache thread-safe by making all your methods synchronized for atomicity.
 */
public interface ResourceCache<R extends Resource<R, LK>, LK> {

    R put(R object);

    R putIfMissing(R object);

    R delete(R object);

    CacheResult<R> get(LK id, CacheMissCallback<R, LK> cacheMissCallback);

    /**
     * This block is called if the item wasn't in the cache. Return an item to be put in the cache
     * in its place.
     *
     * @param <T>
     */
    interface CacheMissCallback<T, K> {
        @Nullable
        T onCacheMiss(K id);
    }

    class CacheResult<T> extends Pair<T, Boolean> {

        public CacheResult(T item, boolean wasInCache) {
            super(item, wasInCache);
        }

        public T getItem(){
            return first;
        }

        public boolean wasInCache(){
            return second;
        }
    }
}