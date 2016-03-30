package audio.rabid.dev.roe.models.resource;

import android.support.annotation.Nullable;

import org.jdeferred.Promise;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;
import java.util.Map;

import audio.rabid.dev.roe.Manager;
import audio.rabid.dev.roe.Operation;
import audio.rabid.dev.roe.Route;

/**
 * Created by charles on 11/11/15.
 */
public interface Resource<T> {

    void setManager(Manager manager);

    Class<T> getModelClass();

    T newInstance();

    T fromJSON(JSONObject data) throws JSONException;

    JSONObject toJSON(T instance) throws JSONException;

    <ID> ID getID(T object) throws Exception;

    boolean allowsMethod(Operation operation);

    String getIdentificationAttribute();

    @Nullable Route getRouteForOperation(Operation operation);

    <ID> Promise<T, Throwable, Void> get(ID id);

    Promise<List<T>, Throwable, Void> getObjectsLocal(Map<String, Object> query);

    Promise<List<T>, Throwable, Void> getObjectsRemote(Map<String, Object> query);

    Promise<T, Throwable, Void> create(T object);

    Promise<T, Throwable, Void> update(T object);

    Promise<T, Throwable, Void> delete(T object);
}