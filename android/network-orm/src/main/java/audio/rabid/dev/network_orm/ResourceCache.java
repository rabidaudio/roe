package audio.rabid.dev.network_orm;

import android.util.SparseArray;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.Callable;

/**
 * Created by charles on 10/29/15.
 *
 * An object cache for keeping track of Resources (either by localId or serverId). You should make
 * your cache thread-safe by making all your methods synchronized for atomicity.
 */
public interface ResourceCache<T extends Resource> {

    T put(T object);

    T putIfMissing(T object);

    T delete(T object);

    T getByLocalId(int localId, @NotNull CacheMissCallback<T> callback);

    T getByServerId(int serverId, @NotNull CacheMissCallback<T> callback);

    /**
     * This block is called if the item wasn't in the cache. Return an item to be put in the cache
     * in its place.
     * @param <T>
     */
    interface CacheMissCallback<T> {
        @Nullable
        T onCacheMiss(int id);
    }
}