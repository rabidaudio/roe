package audio.rabid.dev.roe.testobjects;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import audio.rabid.dev.roe.models.Server;

/**
 * Created by charles on 10/30/15.
 */
public class DummyObjectMockServer extends Server {

    private static DummyObjectMockServer instance = new DummyObjectMockServer();

    public static DummyObjectMockServer getInstance() {
        return instance;
    }

    private DummyObjectMockServer() {
        super(null);
    }

    private int currentPK = 10;

    public int deletedCount = 0;

    public int createCount = 0;

    public int updatedCount = 0;

    boolean networkAvailable = true;

    public void setNetworkAvailable(boolean available) {
        networkAvailable = available;
    }

    private void checkConnection() throws NetworkException {
        if (!networkAvailable) try {
            throw new NetworkException(new Response(404, new JSONObject().put("error", "Network Disabled"), null));
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public synchronized JSONObject getItem(Class<?> clazz, String serverId) throws NetworkException {
        checkConnection();
        try {
            return DummyObjectSource.getInstance().toJSON(new DummyObject("dummy" + serverId, 0, null)).put("id", Integer.parseInt(serverId));
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public synchronized JSONObject createItem(Class<?> clazz, JSONObject item) throws NetworkException {
        checkConnection();
        createCount++;
        try {
            return item.put("id", currentPK++);
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public synchronized List<JSONObject> getItems(Class<?> clazz, JSONObject search) throws NetworkException {
        checkConnection();
        List<JSONObject> items = new ArrayList<>();
        try {
            for (int i = 0; i < Math.round(Math.random() * 10); i++) {
                JSONObject data = DummyObjectSource.getInstance().toJSON(new DummyObject("dummy" + (currentPK++), 0, null)).put("id", currentPK);
                items.add(data);
            }
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
        return items;
    }

    @Override
    public synchronized JSONObject updateItem(Class<?> clazz, String serverId, JSONObject data) throws NetworkException {
        checkConnection();
        updatedCount++;
        return data;
    }

    @Override
    public synchronized JSONObject deleteItem(Class<?> clazz, String serverId) throws NetworkException {
        checkConnection();
        deletedCount++;
        return new JSONObject();
    }

    public boolean isErrorResponse(Response response) {
        return !networkAvailable;
    }
}
