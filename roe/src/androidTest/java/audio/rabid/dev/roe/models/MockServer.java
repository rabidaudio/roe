package audio.rabid.dev.roe.models;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import audio.rabid.dev.roe.models.resource.Resource;

/**
 * Created by charles on 11/11/15.
 */
public class MockServer extends Server {

    private static MockServer instance;

    public static MockServer getInstance() {
        if (instance == null) {
            instance = new MockServer();
        }
        return instance;
    }

    public boolean networkEnabled;

    int serverPk = 10;

    int showCount = 0;
    int indexCount = 0;
    int createCount = 0;
    int updateCount = 0;
    int deleteCount = 0;

    Map<String, Integer> rawRequestCount = new HashMap<>();

    private MockServer() {
//        super("http://localhost");
    }

    public void setNetworkEnabled(boolean enabled) {
        this.networkEnabled = enabled;
    }

    public void clearCounts() {
        showCount = 0;
        indexCount = 0;
        createCount = 0;
        updateCount = 0;
        deleteCount = 0;
        rawRequestCount = new HashMap<>();
    }

    private void checkNetwork() throws NetworkException {
        if (!networkEnabled) {
            throw new NetworkException(null, "mock", Method.GET, null);
        }
    }

    @Override
    public <T extends Resource<?, SK>, SK> JSONObject getItem(Class<T> clazz, SK id) throws Server.NetworkException {
        checkNetwork();
        showCount++;
        if (clazz.equals(GenericResource.class)) {
            try {
                return new GenericResource().asJSON().put("id", id);
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }
        }else if(clazz.equals(GenericChild.class)){
            try {
                return new GenericChild().asJSON().put("id", id);
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }
        } else {
            return null;
        }
    }

    @NonNull
    @Override
    public <T extends Resource> List<JSONObject> getItems(Class<T> clazz, JSONObject search) throws Server.NetworkException {
        checkNetwork();
        indexCount++;
        if (clazz.equals(GenericResource.class)) {
            try {
                List<JSONObject> array = new ArrayList<>(10);
                for (int i = 0; i < 10; i++) {
                    array.add(new GenericResource().asJSON().put("id", ++serverPk));
                }
                return array;
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }
        }else if(clazz.equals(GenericChild.class)){
            try {
                List<JSONObject> array = new ArrayList<>(10);
                for (int i = 0; i < 10; i++) {
                    array.add(new GenericChild("child" + (++serverPk)).asJSON());
                }
                return array;
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }
        } else {
            return null;
        }
    }

    @Override
    public <T extends Resource> JSONObject createItem(Class<T> clazz, T item) throws Server.NetworkException {
        checkNetwork();
        createCount++;
        if (clazz.equals(GenericResource.class)) {
            try {
                return item.asJSON().put("id", ++serverPk);
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }
        }else if(clazz.equals(GenericChild.class)){
            try {
                return item.asJSON();
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }
        } else {
            return null;
        }
    }

    @Override
    public <T extends Resource<?, SK>, SK> JSONObject updateItem(Class<T> clazz, T item, String id) throws Server.NetworkException {
        checkNetwork();
        updateCount++;
        if (clazz.equals(GenericResource.class)) {
            try {
                return item.asJSON();
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }
        }else if(clazz.equals(GenericChild.class)){
            try {
                return item.asJSON();
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }
        } else {
            return null;
        }
    }

    @Override
    public <T extends Resource<?, SK>, SK> void deleteItem(Class<T> clazz, T item, String id) throws Server.NetworkException {
        checkNetwork();
        deleteCount++;
    }

//    @Override
//    public boolean isErrorResponse(Server.Response r) {
//        return !networkEnabled;
//    }

    @Override
    public Response request(String endpoint, Method method, @Nullable Map<String,String> query, @Nullable JSONObject payload) throws NetworkException {
        checkNetwork();
        String r = method.toString();
        Integer currentCount = rawRequestCount.get(r);
        if(currentCount == null){
            currentCount = 0;
        }
        rawRequestCount.put(r, currentCount + 1);
        return new Response(200, new JSONObject(), null);
    }
}
