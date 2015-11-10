package audio.rabid.dev.roe.models;

import com.j256.ormlite.dao.Dao;

/**
 * Created by charles on 11/4/15.
 *
 * A {@link Resource} which has an HTTP API backing it
 */
public interface NetworkResource<LK, SK> extends Resource<LK> {

    /**
     * The public key of the item for the network. This can (and almost surely will) be different than
     * the local id, since if a new item is created without network availability, it must have a local
     * id, but it doesn't yet have a server id.
     */
    SK getServerId();

    boolean hasServerId();
}
