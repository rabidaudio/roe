package audio.rabid.dev.network_orm;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

/**
 * Created by charles on 10/28/15.
 *
 * An abstract builder for {@link Resource}s so that a {@link Source} can create them without knowing
 * what they are. It should *NOT* do any database hits or anything fancy. It should simply create a new
 * instance and return it.
 */
public abstract class ResourceCreator<T extends Resource> {

//    public abstract T newInstance();

    public abstract T createFromJSON(JSONObject json) throws JSONException;

    public abstract List<T> createArrayFromJSON(JSONObject json) throws JSONException;

    public abstract T copyFromNew(T main, T newValues);
}
