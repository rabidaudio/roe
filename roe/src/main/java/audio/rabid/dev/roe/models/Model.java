package audio.rabid.dev.roe.models;

import android.net.NetworkRequest;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.concurrent.Future;

import audio.rabid.dev.roe.models.network.NetworkOperation;

/**
 * Created by  charles  on 3/30/16.
 */
public interface Model<T> {

    Class<T> getModelClass();

    // TODO index

    NetworkOperation createShowRequest(String key);

    NetworkOperation createCreateRequest(T object);

    NetworkOperation createUpdateRequest(T object);

    NetworkOperation createDeleteRequest(T object);

    T fromJSON(JSONObject object) throws JSONException;

    JSONObject toJSON(T object) throws JSONException;


    /////////////////////////////////////
}
