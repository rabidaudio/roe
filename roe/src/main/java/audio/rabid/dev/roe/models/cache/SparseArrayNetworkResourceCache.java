package audio.rabid.dev.roe.models.cache;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.SparseArray;

import audio.rabid.dev.roe.models.NetworkResource;
import audio.rabid.dev.roe.models.NetworkSource;
import audio.rabid.dev.roe.models.Resource;

/**
 * Created by charles on 10/29/15.
 * <p/>
 * A simple implementation of a {@link ResourceCache} which just uses a {@link SparseArray}.
 */
public class SparseArrayNetworkResourceCache<T extends NetworkResource> extends SparseArrayResourceCache<T> implements NetworkResourceCache<T> {

    private final SparseArray<Integer> serverLocalIDMap;

    public SparseArrayNetworkResourceCache(int initialSize) {
        super(initialSize);
        serverLocalIDMap = new SparseArray<>(initialSize);
    }

    @Override
    public synchronized T put(T object) {
        if (object.getServerId() != null) {
            serverLocalIDMap.put(object.getServerId(), object.getId());
        }
        return super.put(object);
    }

    @Override
    public synchronized T putIfMissing(T object) {
        if(object.getServerId() != null && serverLocalIDMap.get(object.getServerId())==null){
            serverLocalIDMap.put(object.getServerId(), object.getId());
        }
        return super.putIfMissing(object);
    }

    @Override
    public synchronized T delete(T object) {
        if(object.getServerId()!=null) serverLocalIDMap.delete(object.getServerId());
        return super.delete(object);
    }

    @Override
    public synchronized CacheResult<T> getByServerId(int id, CacheMissCallback<T> cacheMissCallback) {
        Integer localId = serverLocalIDMap.get(id);
        if (localId == null) {
            return new CacheResult<>(null, false);
        } else {
            return super.get(localId, cacheMissCallback);
        }
    }
}