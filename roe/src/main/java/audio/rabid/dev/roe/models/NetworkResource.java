package audio.rabid.dev.roe.models;

import android.support.annotation.Nullable;

import com.j256.ormlite.field.DatabaseField;

/**
 * Created by charles on 11/4/15.
 *
 * A {@link Resource} which has an HTTP API backing it
 */
public abstract class NetworkResource<T extends NetworkResource> extends Resource<T> {

    @JSONField(key = "id")
    @DatabaseField(index = true)
    protected Integer serverId = null;

    @DatabaseField
    protected boolean synced = false;

    /**
     * The public key of the item for the network. This can (and almost surely will) be different than
     * the local id, since if a new item is created without network availability, it must have a local
     * id, but it doesn't yet have a server id.
     */
    @Nullable
    public Integer getServerId() {
        return serverId;
    }

    /**
     * If the item has pending changes that have not been saved to the network, then this will return
     * false.
     */
    public boolean isSynced() {
        return synced;
    }
}
