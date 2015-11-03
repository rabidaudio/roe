package audio.rabid.dev.network_orm.models.rails;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import audio.rabid.dev.network_orm.models.Resource;
import audio.rabid.dev.network_orm.models.ResourceFactory;

/**
 * Created by charles on 11/3/15.
 */
public class RailsResourceFactory<R extends Resource> implements ResourceFactory<R> {

    private String jsonSingleObjectKey;
    private String jsonArrayObjectKey;
    private Class<R> rClass;

    public RailsResourceFactory(Class<R> clazz, String jsonSingleObjectKey, String jsonArrayObjectKey) {
        this.jsonSingleObjectKey = jsonSingleObjectKey;
        this.jsonArrayObjectKey = jsonArrayObjectKey;
        rClass = clazz;
    }

    public RailsResourceFactory(Class<R> clazz) {
        RailsResource railsResource = clazz.getAnnotation(RailsResource.class);
        if (railsResource == null || railsResource.singularJSONKey() == null || railsResource.pluralJSONKey() == null) {
            throw new IllegalArgumentException("No annotation @RailsResource on Resource "
                    + clazz.getName() + ". Either add the annotation specifying singular and plural JSON keys, " +
                    "or pass a custom ResourceFactory to your Source.");
        }
        jsonSingleObjectKey = railsResource.singularJSONKey();
        jsonArrayObjectKey = railsResource.pluralJSONKey();
        rClass = clazz;
    }

    /**
     * Create a new Resource from the raw server response. Do not save! Simply make a new instance
     * and populate fields.
     *
     * @param json server response body
     * @return a new instance with fields populated
     * @throws JSONException
     */
    @Override
    public R createFromJSON(JSONObject json) throws JSONException {
        R newInstance;
        try {
            newInstance = rClass.getConstructor().newInstance();
        } catch (Exception e) {
            throw new RuntimeException("Unable to create a new instance of " + rClass.getName() +
                    " - missing default constructor.", e);
        }
        newInstance.updateFromJSON(json);
        return newInstance;
    }

    @Override
    public boolean updateItem(R item, JSONObject data) throws JSONException {
        return item.updateFromJSON(data.getJSONObject(jsonSingleObjectKey));
    }

    @Override
    public boolean updateItemDirect(R item, JSONObject data) throws JSONException {
        return item.updateFromJSON(data);
    }

    @Override
    public List<JSONObject> splitMultipleNetworkQuery(JSONObject data) throws JSONException {
        JSONArray array = data.getJSONArray(jsonArrayObjectKey);
        List<JSONObject> result = new ArrayList<>(array.length());
        for (int i = 0; i < array.length(); i++) {
            result.add(array.getJSONObject(i));
        }
        return result;
    }

    @Override
    public JSONObject turnItemIntoValidServerPayload(R item) throws JSONException {
        return new JSONObject().put(jsonSingleObjectKey, item.toJSON());
    }
}