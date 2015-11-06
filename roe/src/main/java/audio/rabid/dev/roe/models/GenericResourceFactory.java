package audio.rabid.dev.roe.models;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Constructor;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by charles on 11/5/15.
 * <p/>
 * The generic resource factory assumes single objects are sent as payloads directly, and arrays
 * are returned as <code>{results: []}</code>.
 */
public class GenericResourceFactory<R extends NetworkResource<R, LK, SK>, LK, SK> implements ResourceFactory<R, SK> {

    private Class<R> rClass;

    public GenericResourceFactory(Class<R> rClass) {
        this.rClass = rClass;
    }

    @Override
    public R createObject(Constructor<R> construcor, Class<R> dataClass) throws SQLException {
        try {
            return createObject();
        } catch (Exception e) {
            throw new SQLException("Unable to create a new instance of " + rClass.getName() +
                    " - missing default constructor.", e);
        }
    }

    @Override
    public R createObject() {
        try {
            return rClass.getConstructor().newInstance();
        } catch (Exception e) {
            throw new RuntimeException("Unable to create a new instance of " + rClass.getName() +
                    " - missing default constructor.", e);
        }
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
        R newInstance = createObject();
        newInstance.updateFromJSON(json);
        return newInstance;
    }

    @Override
    public boolean updateItem(R item, JSONObject data) throws JSONException {
        return item.updateFromJSON(data);
    }

    @Override
    public boolean updateItemDirect(R item, JSONObject data) throws JSONException {
        return item.updateFromJSON(data);
    }

    @SuppressWarnings("unchecked")
    @Override
    public SK getServerKeyFromJSON(JSONObject data) throws JSONException {
        try {
            return (SK) data.get("id").toString();
        } catch (ClassCastException e) {
            throw new JSONException("Unable to guess conversion from data to key. Please supply a ResourceFactory which overrides getServerKeyFromJSON");
        }
    }

    @Override
    public List<JSONObject> splitMultipleNetworkQuery(JSONObject data) throws JSONException {
        JSONArray array = data.getJSONArray("results");
        List<JSONObject> result = new ArrayList<>(array.length());
        for (int i = 0; i < array.length(); i++) {
            result.add(array.getJSONObject(i));
        }
        return result;
    }

    @Override
    public JSONObject turnItemIntoValidServerPayload(R item) throws JSONException {
        return item.toJSON();
    }
}
