package audio.rabid.dev.roe.models;

import com.j256.ormlite.field.DatabaseField;

import java.util.Date;

/**
 * Created by charles on 11/5/15.
 */
public abstract class IntegerKeyedNetworkResource implements NetworkResource<Integer, Integer> {

    @DatabaseField(generatedId = true)
    @JSONField(export = false, accept = false)
    protected Integer id;

    @NetworkId
    @JSONField(key = "id")
    @DatabaseField(index = true)
    protected Integer serverId = null;

    @JSONField(key = "created_at", accept = false)
    @DatabaseField
    protected Date createdAt;

    @JSONField(key = "updated_at", accept = false)
    @DatabaseField
    protected Date updatedAt;

    @Override
    public Integer getId() {
        return id;
    }

    @Override
    public boolean isNew() {
        return id == null;
    }

    @Override
    public Integer getServerId() {
        return serverId;
    }

    @Override
    public boolean hasServerId() {
        return serverId != null;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public Date getUpdatedAt() {
        return updatedAt;
    }
}
