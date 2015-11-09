package audio.rabid.dev.roe.models.rails;

import android.support.annotation.Nullable;

import com.j256.ormlite.table.DatabaseTable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import audio.rabid.dev.roe.models.Server;

/**
 * Created by charles on 10/29/15.
 * <p>
 * A server which expects standard Rails `resources` endpoints. Specifically, the pluralized endpoints
 * except for `new` and `edit`.
 * </p>
 * <p>
 * E.g. `resources :posts`:
 * </p>
 * <pre>
 * index (return multiple posts, possibly with query parameters to filter) - GET /posts => { posts: [ {...}, ...] }
 * show (return single post by id) - GET /posts/:id => { post: {...} }
 * create (create new post) - POST /posts with { post: {...} } => { post: {...} }
 * update (update existing) - PUT/PATCH /posts/:id with { post: {...} } => { post: {...} }
 * delete (delete existing) - DELETE /posts/:id => { post: {...} }
 * </pre>
 */
public class RailsServer extends Server {

    private Map<Class, String> endpoints;

    public RailsServer(String rootURL) {
        super(rootURL);
        endpoints = new HashMap<>();
    }

    public RailsServer(String rootURL, Map<Class, String> endpoints) {
        super(rootURL);
        this.endpoints = endpoints;
    }

    public void addEndpoint(Class clazz, String endpoint) {
        endpoints.put(clazz, endpoint);
    }

    public Response index(String endpoint, @Nullable JSONObject search) throws NetworkException {
        return request(endpoint, Method.GET, search);
    }

    public Response show(String endpoint, String serverId) throws NetworkException {
        return request(endpoint + "/" + serverId, Method.GET, null);
    }

    public Response create(String endpoint, JSONObject data) throws NetworkException {
        return request(endpoint, Method.POST, data);
    }

    public Response update(String endpoint, String serverId, JSONObject data) throws NetworkException {
        return request(endpoint + "/" + serverId, Method.PUT, data);
    }

    public Response destroy(String endpoint, String serverId) throws NetworkException {
        return request(endpoint + "/" + serverId, Method.DELETE, null);
    }

    private boolean isErrorResponse(Response response) {
        return (response.getResponseCode() / 100 != 2) || !response.getResponseBody().isNull("error");
    }

    @Override
    public JSONObject getItem(Class<?> clazz, String serverId) throws NetworkException {
        Response r = show(getEndpoint(clazz), serverId);
        if (isErrorResponse(r)) {
            throw new NetworkException(r);
        } else {
            try {
                return r.getResponseBody().getJSONObject(getSingularKey(clazz));
            } catch (JSONException e) {
                throw new NetworkException(e);
            }
        }
    }

    @Override
    public JSONObject createItem(Class<?> clazz, JSONObject item) throws NetworkException {
        Response r = create(getEndpoint(clazz), item);
        if (isErrorResponse(r)) {
            throw new NetworkException(r);
        } else {
            try {
                return r.getResponseBody().getJSONObject(getSingularKey(clazz));
            } catch (JSONException e) {
                throw new NetworkException(e);
            }
        }
    }

    @Override
    public List<JSONObject> getItems(Class<?> clazz, JSONObject search) throws NetworkException {
        Response r = index(getEndpoint(clazz), search);
        if (isErrorResponse(r)) {
            throw new NetworkException(r);
        } else {
            try {
                JSONArray array = r.getResponseBody().getJSONArray(getPluralKey(clazz));
                List<JSONObject> objects = new ArrayList<>(array.length());
                for (int i = 0; i < array.length(); i++) {
                    objects.add(array.getJSONObject(i));
                }
                return objects;
            } catch (JSONException e) {
                throw new NetworkException(e);
            }
        }
    }

    @Override
    public JSONObject updateItem(Class<?> clazz, String serverId, JSONObject data) throws NetworkException {
        Response r = update(getEndpoint(clazz), serverId, data);
        if (isErrorResponse(r)) {
            throw new NetworkException(r);
        } else {
            try {
                return r.getResponseBody().getJSONObject(getSingularKey(clazz));
            } catch (JSONException e) {
                throw new NetworkException(e);
            }
        }
    }

    @Override
    public JSONObject deleteItem(Class<?> clazz, String serverId) throws NetworkException {
        Response r = destroy(getEndpoint(clazz), serverId);
        if (isErrorResponse(r)) {
            throw new NetworkException(r);
        } else {
            try {
                return r.getResponseBody().getJSONObject(getSingularKey(clazz));
            } catch (JSONException e) {
                throw new NetworkException(e);
            }
        }
    }

    private String getEndpoint(Class<?> clazz) {
        String endpoint = endpoints.get(clazz);
        if (endpoint == null) {
            try {
                //try to get from annotation
                RailsResource railsResource = clazz.getAnnotation(RailsResource.class);
                if (railsResource != null) {
                    if (!railsResource.endpoint().isEmpty()) {
                        endpoint = railsResource.endpoint();
                    } else if (!railsResource.pluralJSONKey().isEmpty()) {
                        endpoint = railsResource.pluralJSONKey();
                    }
                } else {
                    //try to get from Resource table name
                    DatabaseTable table = clazz.getAnnotation(DatabaseTable.class);
                    if (table != null) {
                        endpoint = table.tableName();
                    } else {
                        endpoint = clazz.getSimpleName().toLowerCase();
                    }
                }
                endpoints.put(clazz, endpoint);
            } catch (Exception e) {
                throw new NullPointerException("Endpoint was never sent to RailsServer and couldn't be inferred from Resource");
            }
        }
        if(endpoint==null){
            throw new NullPointerException("Endpoint was never sent to RailsServer and couldn't be inferred from Resource");
        }
        return endpoint;
    }

    private HashMap<Class, String> singularKeys = new HashMap<>();
    private HashMap<Class, String> pluralKeys = new HashMap<>();

    private String getPluralKey(Class<?> clazz) {
        String key = pluralKeys.get(clazz);
        if (key == null) {
            populateKeyArrays(clazz);
            key = pluralKeys.get(clazz);
        }
        return key;
    }

    private String getSingularKey(Class<?> clazz) {
        String key = singularKeys.get(clazz);
        if (key == null) {
            populateKeyArrays(clazz);
            key = singularKeys.get(clazz);
        }
        return key;
    }

    private void populateKeyArrays(Class<?> clazz) {
        String singleKey, pluralKey;
        RailsResource railsResource = clazz.getAnnotation(RailsResource.class);
        if (railsResource == null || railsResource.singularJSONKey() == null || railsResource.pluralJSONKey() == null) {
            singleKey = clazz.getSimpleName().toLowerCase();
            pluralKey = singleKey.concat("s"); //TODO janky pluralization
        } else {
            singleKey = railsResource.singularJSONKey();
            pluralKey = railsResource.pluralJSONKey();
        }
        singularKeys.put(clazz, singleKey);
        pluralKeys.put(clazz, pluralKey);
    }
}
