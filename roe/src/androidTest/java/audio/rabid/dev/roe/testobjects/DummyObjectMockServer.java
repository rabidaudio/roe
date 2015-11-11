package audio.rabid.dev.roe.testobjects;


import org.json.JSONException;
import org.json.JSONObject;

import java.net.HttpURLConnection;
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

    public int readCount = 0;

    public int deletedCount = 0;

    public int createCount = 0;

    public int updatedCount = 0;

    boolean networkAvailable = true;

    public void setNetworkAvailable(boolean available) {
        networkAvailable = available;
    }

    public void checkConnection() throws NetworkException {
        if(!networkAvailable){
            throw new NetworkException(new IllegalStateException("Network disabled"), "dummy", Method.GET, null);
        }
    }

    @Override
    public <T, ID> JSONObject getItem(Class<T> clazz, ID id) throws NetworkException {
        checkConnection();
        readCount++;
        try {
            return ((JSONObject) JSON.toJSON(new DummyObject("dummy" + String.valueOf(id), 0, null))).put("id", id);
        }catch (JSONException e){
            throw new RuntimeException(e);
        }
    }

    @Override
    public <T> List<JSONObject> getItems(Class<T> clazz, JSONObject search) throws NetworkException {
        checkConnection();
        List<JSONObject> items = new ArrayList<>();
        try {
            for (int i = 0; i < 10; i++) {
                readCount++;
                JSONObject data = ((JSONObject) JSON.toJSON(new DummyObject("dummy" + (currentPK++), 0, null))).put("id", currentPK);
                items.add(data);
            }
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
        return items;
    }

    @Override
    public <T> JSONObject createItem(Class<T> clazz, T item) throws NetworkException {
        checkConnection();
        createCount++;
        try {
            return ((JSONObject) JSON.toJSON(item)).put("id", currentPK++);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public <T> JSONObject updateItem(Class<T> clazz, T item) throws NetworkException {
        checkConnection();
        updatedCount++;
        return ((JSONObject) JSON.toJSON(item));
    }

    @Override
    public <T> JSONObject deleteItem(Class<T> clazz, T item) throws NetworkException {
        checkConnection();
        deletedCount++;
        return new JSONObject();
    }

    public boolean isErrorResponse(Response response) {
        return !networkAvailable;
    }
}
