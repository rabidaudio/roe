package audio.rabid.dev.network_orm.models.cache;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import audio.rabid.dev.network_orm.models.Resource;

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

    T getByLocalId(int localId, @NonNull CacheMissCallback<T> callback);

    T getByServerId(int serverId, @NonNull CacheMissCallback<T> callback);

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
}