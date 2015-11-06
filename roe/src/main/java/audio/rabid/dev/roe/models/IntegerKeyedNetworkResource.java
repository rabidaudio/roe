package audio.rabid.dev.roe.models;

import android.support.annotation.Nullable;

import com.j256.ormlite.field.DatabaseField;

/**
 * Created by charles on 11/5/15.
 */
public abstract class IntegerKeyedNetworkResource<R extends IntegerKeyedNetworkResource<R>> extends NetworkResource<R, Integer, Integer> {

    @DatabaseField(generatedId = true)
    @JSONField(export = false, accept = false)
    protected Integer id;

    @JSONField(key = "id")
    @DatabaseField(index = true)
    protected Integer serverId = null;

    @Override
    public Integer getId() {
        return id;
    }

    @Override
    public boolean isNew() {
        return id == null;
    }

    @Nullable
    @Override
    public Integer getServerId() {
        return serverId;
    }
}
