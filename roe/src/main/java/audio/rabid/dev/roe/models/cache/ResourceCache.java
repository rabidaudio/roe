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
public interface ResourceCache<T extends Resource> {

    T put(T object);

    T putIfMissing(T object);

    T delete(T object);

     CacheResult<T> get(int id, CacheMissCallback<T> cacheMissCallback);

    /**
     * This block is called if the item wasn't in the cache. Return an item to be put in the cache
     * in its place.
     *
     * @param <T>
     */
    interface CacheMissCallback<T> {
        @Nullable
        T onCacheMiss(int id);
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