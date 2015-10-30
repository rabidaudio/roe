package audio.rabid.dev.network_orm;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

/**
 * Created by charles on 10/28/15.
 *
 * An abstract builder for {@link Resource}s so that a {@link Source} can create them without knowing
 * what they are. It should *NOT* do any database hits or anything fancy. It should simply create a new
 * instance, populate values, and return it.
 *
 * TODO a lot of this can be done by reflection by default
 */
public interface ResourceFactory<T extends Resource> {

    /**
     * Create a new Resource from the raw server response. Do not save! Simply make a new instance
     * and populate fields.
     * @param json server response body
     * @return a new instance with fields populated
     * @throws JSONException
     */
    T createFromJSON(JSONObject json) throws JSONException;

    /**
     *
     * @param item the item in question
     * @param data the raw server response body
     * @return true if any of the fields have changed
     * @throws JSONException
     */
    boolean updateItem(T item, JSONObject data) throws JSONException;

    /**
     * Take in a raw server response which should be of multiple Resources, and split it into
     * a list of JSONObjects which represent a Resource to be created or updated.
     *
     * @param data the raw server response body
     * @return a list of JSONObjects representing a Resource
     * @throws JSONException
     */
    List<JSONObject> splitMultipleNetworkQuery(JSONObject data) throws JSONException;
}
