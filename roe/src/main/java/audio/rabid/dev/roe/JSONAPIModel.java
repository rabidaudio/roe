package audio.rabid.dev.roe;

import android.util.Pair;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.HttpURLConnection;

import audio.rabid.dev.roe.models.network.Method;
import audio.rabid.dev.roe.models.network.NetworkOperation;
import audio.rabid.dev.roe.models.network.Response;

/**
 * Created by  charles  on 3/30/16.
 */
public abstract class JSONAPIModel<T> implements AModel<T>, NetworkOperation.RequestCallbacks {

    private String pathPattern;
    private String rootURL;

    public JSONAPIModel(String rootURL, String pathPattern){
        this.rootURL = rootURL;
        this.pathPattern = pathPattern;
    }

    public String generatePath(String id){
        return pathPattern.replace("/:key", id);
    }

    public String getPathPattern(){
        return pathPattern;
    }

    @Override
    public T showNetwork(String id) throws Exception {
        NetworkOperation op = new NetworkOperation(Method.GET, generatePath("/"+id), null);
        Response response = op.request(rootURL, 1000, this);
        return fromJSON(response.getResponseBody());
    }

    @Override
    public Pair<String, T> createNetwork(T object) throws Exception {
        NetworkOperation op = new NetworkOperation(Method.POST, generatePath(""), toJSON(object));
        Response response = op.request(rootURL, 1000, this);
        T result = fromJSON(response.getResponseBody());
        String key = getKey(result);
        return new Pair<>(key, result);
    }

    @Override
    public T updateNetwork(String id, T object) throws Exception {
        NetworkOperation op = new NetworkOperation(Method.PUT, generatePath(id), toJSON(object));
        Response response = op.request(rootURL, 1000, this);
        return fromJSON(response.getResponseBody());
    }

    @Override
    public void deleteNetwork(String id) throws Exception {
        NetworkOperation op = new NetworkOperation(Method.DELETE, generatePath(id), null);
        op.request(rootURL, 1000, this);
    }

    @Override
    public void onBeforeConnection(HttpURLConnection connection, JSONObject payload) {
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setRequestProperty("Accept", "application/json");
    }

    public abstract T fromJSON(JSONObject object) throws JSONException;

    public abstract JSONObject toJSON(T object) throws JSONException;

    public abstract String getKey(T object);
}
