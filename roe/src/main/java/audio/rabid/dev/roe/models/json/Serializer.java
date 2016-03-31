package audio.rabid.dev.roe.models.json;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by charles on 11/11/15.
 *
 * Supply your own serializer/deserializer for a particular field. Generic T is the type your object
 * actually is, not it's type when converted to JSON.
 *
 * Be sure that you have a default no-arg constructor that is publicly accessible!
 */
public interface Serializer<T> {

    /**
     * Convert item to JSON
     * @param item the item
     * @return some object which is a valid argument to {@link JSONObject#put(String, Object)}
     * @throws JSONException
     */
    Object serialize(T item) throws JSONException;

    /**
     * Convert JSON to item
     * @param object the value from {@link JSONObject#get(String)}
     * @return the item
     * @throws JSONException
     */
    T deserialize(Object object) throws JSONException;
}