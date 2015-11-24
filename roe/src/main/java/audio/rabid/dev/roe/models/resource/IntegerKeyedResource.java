package audio.rabid.dev.roe.models.resource;

import com.j256.ormlite.field.DatabaseField;

import audio.rabid.dev.roe.models.json.JSONField;
import audio.rabid.dev.roe.models.json.SimpleJSONifyable;

/**
 * Created by charles on 11/11/15.
 */
public abstract class IntegerKeyedResource extends SimpleJSONifyable implements Resource<Integer, Integer> {

    @DatabaseField(generatedId = true)
    protected Integer id;

    @JSONField(key = "id")
    @DatabaseField(index = true)
    protected Integer serverId;

    @Override
    public Integer getServerId() {
        return serverId;
    }

    @Override
    public boolean hasServerId() {
        return serverId != null;
    }

    public Integer getId(){
        return id;
    }
}
