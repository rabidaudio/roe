package audio.rabid.dev.roe.models.cache;

import audio.rabid.dev.roe.models.NetworkResource;

/**
 * Created by charles on 11/4/15.
 */
public interface NetworkResourceCache<T extends NetworkResource> extends ResourceCache<T> {

    CacheResult<T> getByServerId(int id, CacheMissCallback<T> cacheMissCallback);

}
