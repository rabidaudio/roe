package audio.rabid.dev.network_orm;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by charles on 10/28/15.
 *
 * An abstract builder for {@link Resource}s so that a {@link Source} can create them without knowing
 * what they are. It should *NOT* do any database hits or anything fancy. It should simply create a new
 * instance, populate values, and return it.
 */
public interface ResourceFactory<T extends Resource> {

    T createFromJSON(JSONObject json) throws JSONException;

}
