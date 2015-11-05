package audio.rabid.dev.roe.testobjects;

import com.j256.ormlite.dao.Dao;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import audio.rabid.dev.roe.models.NetworkSource;
import audio.rabid.dev.roe.models.ResourceFactory;
import audio.rabid.dev.roe.models.SimplePermissionsManager;
import audio.rabid.dev.roe.models.cache.SparseArrayNetworkResourceCache;

/**
 * Created by charles on 11/5/15.
 */
public class DummyObjectSource extends NetworkSource<DummyObject> {

    private static DummyObjectSource instance;
    public static DummyObjectSource getInstance(){
        if(instance == null){
            try{
                instance = new DummyObjectSource();
            }catch (SQLException e){
                throw new RuntimeException(e);
            }
        }
        return instance;
    }

    private DummyObjectSource() throws SQLException {
        super(DummyObjectMockServer.getInstance(), (Dao<DummyObject, Integer>) GenericDatabase.getInstance().getDao(DummyObject.class), //TODO
                GenericDatabase.getInstance().getConnectionSource(), new DummyObjectResourceFactory(),
                new SparseArrayNetworkResourceCache<DummyObject>(50), new SimplePermissionsManager<DummyObject>().all(), null);
    }

    private boolean updateCompleted = false;

    @Override
    protected void onAfterUpdated(DummyObject updated){
        super.onAfterUpdated(updated);
        updateCompleted = true;
    }

    public boolean wasUpdateCompleted(){
        return updateCompleted;
    }

    public void clearUpdateCompleted(){
        updateCompleted = false;
    }

    private boolean createCompleted = false;

    @Override
    protected void onAfterCreated(DummyObject created){
        super.onAfterCreated(created);
        createCompleted = true;
    }

    public boolean wasCreateCompleted(){
        return createCompleted;
    }

    public void clearCreateCompleted(){
        createCompleted = false;
    }

    private boolean deleteCompleted = false;

    @Override
    protected void onAfterDeleted(DummyObject deleted){
        super.onAfterDeleted(deleted);
        deleteCompleted = true;
    }

    public boolean wasDeleteCompleted(){
        return deleteCompleted;
    }

    public void clearDeleteCompleted(){
        deleteCompleted = false;
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
