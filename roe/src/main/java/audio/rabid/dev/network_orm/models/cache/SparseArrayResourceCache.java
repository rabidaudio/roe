package audio.rabid.dev.network_orm.models.cache;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.SparseArray;

import audio.rabid.dev.network_orm.models.Resource;

/**
 * Created by charles on 10/29/15.
 * <p/>
 * A simple implementation of a {@link ResourceCache} which just uses a {@link SparseArray}.
 */
public class SparseArrayResourceCache<T extends Resource> implements ResourceCache<T> {

    private final SparseArray<T> instanceCache;
    private final SparseArray<Integer> serverLocalIDMap;

    public SparseArrayResourceCache(int initialSize) {
        instanceCache = new SparseArray<>(initialSize);
        serverLocalIDMap = new SparseArray<>(initialSize);
    }

    public synchronized T put(@Nullable T object) {
        if (object == null) {
            return null;
        } else {
            instanceCache.put(object.getId(), object);
            if (object.getServerId() > 0) {
                serverLocalIDMap.put(object.getServerId(), object.getId());
            }
            return object;
        }
    }

    public synchronized T putIfMissing(T object) {
        T cached = instanceCache.get(object.getId());
        if (cached == null) {
            instanceCache.put(object.getId(), object);
            return object;
        } else {
            return cached;
        }
    }

    @Override
    public synchronized T getByLocalId(int localId, @NonNull CacheMissCallback<T> callback) {
        T cached = instanceCache.get(localId);
        if (cached == null) {
            return put(callback.onCacheMiss(localId));
        } else {
            return cached;
        }
    }

    @Override
    public synchronized T getByServerId(int serverId, @NonNull CacheMissCallback<T> callback) {
        Integer localId = serverLocalIDMap.get(serverId);
        if (localId == null) {
            return put(callback.onCacheMiss(serverId));
        } else {
            return instanceCache.get(localId);
        }
    }

    @Override
    public synchronized T delete(T object) {
        serverLocalIDMap.delete(object.getServerId());
        instanceCache.delete(object.getId());
        return object;
    }
}