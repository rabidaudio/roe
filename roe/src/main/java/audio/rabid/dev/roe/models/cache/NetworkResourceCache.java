package audio.rabid.dev.roe.models.cache;

import audio.rabid.dev.roe.models.NetworkResource;

/**
 * Created by charles on 11/4/15.
 */
public interface NetworkResourceCache<R extends NetworkResource<R, LK, SK>, LK, SK> extends ResourceCache<R, LK> {

    CacheResult<R> getByServerId(SK serverId, CacheMissCallback<R, SK> cacheMissCallback);

}
