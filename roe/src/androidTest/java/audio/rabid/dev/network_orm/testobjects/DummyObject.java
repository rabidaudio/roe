package audio.rabid.dev.network_orm.testobjects;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import audio.rabid.dev.network_orm.models.PermissionsManager;
import audio.rabid.dev.network_orm.models.JSONField;
import audio.rabid.dev.network_orm.models.Resource;
import audio.rabid.dev.network_orm.models.ResourceFactory;
import audio.rabid.dev.network_orm.models.SimplePermissionsManager;
import audio.rabid.dev.network_orm.models.Source;
import audio.rabid.dev.network_orm.models.cache.SparseArrayResourceCache;
import audio.rabid.dev.network_orm.models.rails.NetworkDateFormat;

/**
 * Created by charles on 10/30/15.
 */
@DatabaseTable(tableName = "dummies")
public class DummyObject extends Resource<DummyObject> {

    public DummyObject() {
    }

    public DummyObject(String name, int age, DummyChild child) {
        this.name = name;
        this.age = age;
        this.child = child;
    }

    @JSONField
    @DatabaseField
    public String name;

    @JSONField
    @DatabaseField
    public int age = 0;

    @JSONField
    @DatabaseField(foreign = true)
    public DummyChild child;

    @SuppressWarnings("unchecked")
    public static Source<DummyObject> SOURCE = new Source<>(
            DummyObjectMockServer.getInstance(),
            GenericDatabase.getDaoOrThrow(DummyObject.class),
            new SparseArrayResourceCache<DummyObject>(50),
            new DummyObjectResourceFactory(),
            SimplePermissionsManager.ALL,
            new NetworkDateFormat(),
            GenericDatabase.getInstance().getConnectionSource());

    @Override
    public Source<DummyObject> getSource() {
        return SOURCE;
    }

    private static class DummyObjectResourceFactory implements ResourceFactory<DummyObject> {

        @Override
        public DummyObject createFromJSON(JSONObject json) throws JSONException {
            DummyObject d = new DummyObject();
            d.updateFromJSON(json);
            return d;
        }

        @Override
        public boolean updateItem(DummyObject item, JSONObject data) throws JSONException {
            return item.updateFromJSON(data);
        }

        @Override
        public boolean updateItemDirect(DummyObject item, JSONObject data) throws JSONException {
            return item.updateFromJSON(data);
        }

        @Override
        public List<JSONObject> splitMultipleNetworkQuery(JSONObject data) throws JSONException {
            JSONArray a = data.getJSONArray("dummies");
            List<JSONObject> results = new ArrayList<>(a.length());
            for (int i = 0; i < a.length(); i++) {
                results.add(a.getJSONObject(i));
            }
            return results;
        }

        @Override
        public JSONObject turnItemIntoValidServerPayload(DummyObject item) throws JSONException {
            return item.toJSON();
        }
    }
}
