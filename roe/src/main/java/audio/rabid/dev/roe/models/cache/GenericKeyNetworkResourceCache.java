package audio.rabid.dev.roe.models.cache;

import android.util.SparseArray;

import java.util.HashMap;

import audio.rabid.dev.roe.models.NetworkResource;

/**
 * Created by charles on 10/29/15.
 * <p/>
 * A simple implementation of a {@link ResourceCache} which just uses a {@link SparseArray}.
 */
public class GenericKeyNetworkResourceCache<R extends NetworkResource<R, LK, SK>, LK, SK> extends GenericKeyResourceCache<R, LK> implements NetworkResourceCache<R, LK, SK> {

    private final HashMap<SK, LK> serverLocalIDMap;

    public GenericKeyNetworkResourceCache(int initialSize) {
        super(initialSize);
        serverLocalIDMap = new HashMap<>(initialSize);
    }

    @Override
    public synchronized R put(R object) {
        if (object.getServerId() != null) {
            serverLocalIDMap.put(object.getServerId(), object.getId());
        }
        return super.put(object);
    }

    @Override
    public synchronized R putIfMissing(R object) {
        if(object.getServerId() != null && serverLocalIDMap.get(object.getServerId())==null){
            serverLocalIDMap.put(object.getServerId(), object.getId());
        }
        return super.putIfMissing(object);
    }

    @Override
    public synchronized R delete(R object) {
        if (object.getServerId() != null) serverLocalIDMap.remove(object.getServerId());
        return super.delete(object);
    }

    @Override
    public synchronized CacheResult<R> get(LK localId, CacheMissCallback<R, LK> cacheMissCallback) {
        CacheResult<R> result = super.get(localId, cacheMissCallback);
        if(result.getItem() != null && result.getItem().getServerId()!=null){
            serverLocalIDMap.put(result.getItem().getServerId(), result.getItem().getId());
        }
        return result;
    }

    @Override
    public synchronized CacheResult<R> getByServerId(SK serverId, CacheMissCallback<R, SK> cacheMissCallback) {
        LK localId = serverLocalIDMap.get(serverId);
        if (localId == null) {
            return new CacheResult<>(cacheMissCallback.onCacheMiss(serverId), false);
        } else {
            //if we have a localId, we know the item must be in the cache
            return new CacheResult<>(getInstanceCache().get(localId), true);
        }
    }

    public synchronized int size(){
        return getInstanceCache().size();
    }
}