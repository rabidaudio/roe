package audio.rabid.dev.roe.models.resource;

import com.j256.ormlite.field.DatabaseField;

import audio.rabid.dev.roe.models.ServerKey;
import audio.rabid.dev.roe.models.json.SimpleJSONifyable;

/**
 * Created by charles on 11/11/15.
 */
public abstract class SameStringKeyedResource extends SimpleJSONifyable implements Resource<String, String> {

    @ServerKey
    @DatabaseField(id = true)
    protected String id;

    @Override
    public String getServerId() {
        return id;
    }

    @Override
    public boolean hasServerId() {
        return id != null && !id.isEmpty();
    }
}
