package audio.rabid.dev.roe.models.resource;

import org.json.JSONException;
import org.json.JSONObject;

import audio.rabid.dev.roe.Manager;
import audio.rabid.dev.roe.models.json.JSONifyable;

/**
 * Created by  charles  on 3/15/16.
 */
public abstract class JSONifyableResource<T extends JSONifyable> extends AbstractResource<T> {

    @SuppressWarnings("unchecked")
    @Override
    public <ID> ID getID(T object) throws Exception {
        return (ID) object.asJSON().get(getIdentificationAttribute());
    }

    public T fromJSON(JSONObject data) throws JSONException {
        T t = newInstance();
        t.fromJSON(data);
        return t;
    }

    public JSONObject toJSON(T instance) throws JSONException {
        return instance.asJSON();
    }
}
