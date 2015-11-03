package audio.rabid.dev.roe.testobjects;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

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

    boolean networkAvailable = true;

    public void setNetworkAvailable(boolean available) {
        networkAvailable = available;
    }

    private void checkConnection() throws NetworkException {
        if (!networkAvailable) throw new NetworkException(new NoSuchMethodException());
    }

    @Override
    public Response getItem(Class<?> clazz, int serverId) throws NetworkException {
        checkConnection();
        try {
            JSONObject data = new DummyObject("dummy" + (currentPK++), 0, null).toJSON().put("id", currentPK);
            return new Response(200, data, null);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Response createItem(Class<?> clazz, JSONObject item) throws NetworkException {
        checkConnection();
        try {
            return new Response(200, item.put("id", currentPK++), null);
        }catch (JSONException e){
            throw new RuntimeException(e);
        }
    }

    @Override
    public Response getItems(Class<?> clazz, JSONObject search) throws NetworkException {
        checkConnection();
        JSONArray results = new JSONArray();
        try {
            for (int i = 0; i < Math.round(Math.random() * 10); i++) {
                JSONObject data = new DummyObject("dummy" + (currentPK++), 0, null).toJSON().put("id", currentPK);
                results.put(data);
            }
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
        try {
            return new Response(200, new JSONObject().put("dummies", results), null);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Response updateItem(Class<?> clazz, int serverId, JSONObject data) throws NetworkException {
        checkConnection();
        return new Response(200, data, null);
    }

    @Override
    public Response deleteItem(Class<?> clazz, int serverId) throws NetworkException {
        checkConnection();
        deletedCount++;
        return new Response(200, new JSONObject(), null);
    }

    @Override
    public boolean isErrorResponse(Response response) {
        return !networkAvailable;
    }
}
