package audio.rabid.dev.roe.models.rails;

import android.support.annotation.Nullable;

import com.alibaba.fastjson.JSON;
import com.j256.ormlite.table.DatabaseTable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Field;
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
 *
 * TODO make abstract rails server, plus one that uses annotations for everything
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

    public boolean isErrorResponse(Response response) {
        return (response.getResponseCode() / 100 != 2) || !response.getResponseBody().isNull("error");
    }

    @Override
    public <T, ID> JSONObject getItem(Class<T> clazz, ID id) throws NetworkException {
        Response r = show(getEndpoint(clazz), String.valueOf(id));
        try {
            return r.getResponseBody().getJSONObject(getSingularKey(clazz));
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public <T> JSONObject createItem(Class<T> clazz, T item) throws NetworkException {
        Response r = create(getEndpoint(clazz), (JSONObject) JSON.toJSON(item));
        try {
            return r.getResponseBody().getJSONObject(getSingularKey(clazz));
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public <T> List<JSONObject> getItems(Class<T> clazz, JSONObject search) throws NetworkException {
        Response r = index(getEndpoint(clazz), search);
        try {
            JSONArray array = r.getResponseBody().getJSONArray(getPluralKey(clazz));
            List<JSONObject> objects = new ArrayList<>(array.length());
            for (int i = 0; i < array.length(); i++) {
                objects.add(array.getJSONObject(i));
            }
            return objects;
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public <T> JSONObject updateItem(Class<T> clazz, T item) throws NetworkException {
        Response r = update(getEndpoint(clazz), getModelKey(clazz, item), (JSONObject) JSON.toJSON(item));
        try {
            return r.getResponseBody().getJSONObject(getSingularKey(clazz));
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public <T> JSONObject deleteItem(Class<T> clazz, T item) throws NetworkException {
        Response r = destroy(getEndpoint(clazz), getModelKey(clazz, item));
        try {
            return r.getResponseBody().getJSONObject(getSingularKey(clazz));
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    private String getEndpoint(Class<?> clazz) {
        String endpoint = endpoints.get(clazz);
        if (endpoint == null) {
            try {
                //try to get from annotation
                RailsModelKey railsModelKey = clazz.getAnnotation(RailsModelKey.class);
                if (railsModelKey != null) {
                    if (!railsModelKey.endpoint().isEmpty()) {
                        endpoint = railsModelKey.endpoint();
                    } else if (!railsModelKey.pluralJSONKey().isEmpty()) {
                        endpoint = railsModelKey.pluralJSONKey();
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

    public <T> String getModelKey(Class<T> tClass, T item){
        try {
            Field f = getModelKeyField(tClass);
            if(f != null){
                return String.valueOf(f.get(item));
            }
        }catch (IllegalAccessException e){
            throw new RuntimeException("Unable to determine which key to use as server key for "+tClass.getSimpleName()+". Either annotate the field with @RailsModelKey or subclass your own RailsServer.", e);
        }
        throw new RuntimeException("Unable to determine which key to use as server key for "+tClass.getSimpleName()+". Either annotate the field with @RailsModelKey or subclass your own RailsServer.");
    }

    private <T> Field getModelKeyField(Class<T> tClass){
        Field f = serverKeys.get(tClass);
        if (f == null) {
            for (Class<?> classWalk = tClass; classWalk != null; classWalk = classWalk.getSuperclass()) {
                for (Field field : classWalk.getDeclaredFields()) {
                    RailsModelKey key = field.getAnnotation(RailsModelKey.class);
                    if (key != null) {
                        serverKeys.put(tClass, field);
                        return field;
                    }
                }
            }
            return null;
        }else {
            return f;
        }
    }

    private HashMap<Class, Field> serverKeys = new HashMap<>();

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

    private <T> void populateKeyArrays(Class<T> clazz) {
        String singleKey, pluralKey;
        RailsModelKey railsModelKey = getModelKeyField(clazz).getAnnotation(RailsModelKey.class);
        if (railsModelKey == null || railsModelKey.singularJSONKey() == null || railsModelKey.pluralJSONKey() == null) {
            singleKey = clazz.getSimpleName().toLowerCase();
            pluralKey = singleKey.concat("s"); //TODO janky pluralization
        } else {
            singleKey = railsModelKey.singularJSONKey();
            pluralKey = railsModelKey.pluralJSONKey();
        }
        singularKeys.put(clazz, singleKey);
        pluralKeys.put(clazz, pluralKey);
    }
}
