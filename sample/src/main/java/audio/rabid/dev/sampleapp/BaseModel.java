package audio.rabid.dev.sampleapp;

import com.snappydb.DB;
import com.snappydb.SnappydbException;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import audio.rabid.dev.roe.models.Model;
import audio.rabid.dev.roe.models.Op;
import audio.rabid.dev.roe.models.json.JSONifyable;
import audio.rabid.dev.roe.models.network.Method;
import audio.rabid.dev.roe.models.network.NetworkOperation;
import audio.rabid.dev.roe.models.network.Response;

/**
 * Created by  charles  on 3/30/16.
 */
public abstract class BaseModel<T extends JSONifyable, ID> implements Model<T, ID>, NetworkOperation.RequestCallbacks {

    private DB database;

    private String path;

    private String rootURL = "https://localhost:3000";

    public BaseModel(DB database, String path){
        this.database = database;
        this.path = path;
    }

    public String getPath(Op op, ID id){
        switch (op){
            case INDEX:
            case CREATE:
                return path;
            case SHOW:
            case UPDATE:
            case DESTROY:
            default:
                return path+"/"+String.valueOf(id);
        }
    }

    public T getObject(Op op, Response response) throws Exception {
        T newInstance = newInstance();
        newInstance.fromJSON(response.getResponseBody().getJSONObject("data"));
        return newInstance;
    }

    public List<T> getObjects(Response response) throws Exception {
        JSONArray array = response.getResponseBody().getJSONArray("data");
        List<T> results = new ArrayList<>(array.length());
        for(int i=0; i<array.length(); i++){
            T newInstance = newInstance();
            newInstance.fromJSON(array.getJSONObject(i));
            results.add(newInstance);
        }
        return results;
    }

    @Override
    public void onBeforeConnection(HttpURLConnection connection, JSONObject payload) {
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setRequestProperty("Accept", "application/json");
    }

    @Override
    public List<T> query(Map<String, String> query) throws Exception {
        NetworkOperation op = new NetworkOperation(Method.GET, getPath(Op.INDEX, null), new JSONObject(query));
        Response response = op.request(rootURL, 1000, this);
        return getObjects(response);
    }

    @Override
    public T find(ID id) throws Exception {
        NetworkOperation op = new NetworkOperation(Method.GET, getPath(Op.SHOW, id), null);
        Response response = op.request(rootURL, 1000, this);
        return getObject(Op.SHOW, response);
    }

    @Override
    public T create(T object) throws Exception {
        NetworkOperation op = new NetworkOperation(Method.POST, getPath(Op.CREATE, null), object.asJSON());
        Response response = op.request(rootURL, 1000, this);
        return getObject(Op.CREATE, response);
    }

    @Override
    public T update(ID id, T object) throws Exception {
        NetworkOperation op = new NetworkOperation(Method.PUT, getPath(Op.UPDATE, id), object.asJSON());
        Response response = op.request(rootURL, 1000, this);
        return getObject(Op.UPDATE, response);
    }

    @Override
    public void delete(T object) throws Exception {
        NetworkOperation op = new NetworkOperation(Method.DELETE, getPath(Op.DESTROY, getID(object)), object.asJSON());
        op.request(rootURL, 1000, this);
    }

    @Override
    public T findInPersistentStorage(ID id) {
//        return database.getObject(String.valueOf(id), ); //TODO
        return null;
    }

    @Override
    public List<T> queryPersistentStorage(Map<String, String> query) {
        return new ArrayList<>(0); //TODO
    }

    @Override
    public void saveToPersistentStorage(ID id, T object) {
        try {
            database.put(String.valueOf(id), object);
        }catch (SnappydbException e){
            throw new RuntimeException(e);
        }
    }

    public abstract T newInstance();
}
