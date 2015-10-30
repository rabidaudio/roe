package audio.rabid.dev.network_orm;

import com.j256.ormlite.dao.Dao;

import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by charles on 10/29/15.
 */
public class RailsSource<T extends Resource> extends Source<T> {

    public RailsSource(@NotNull RailsServer server, @NotNull Dao<T, Integer> dao, @NotNull String endpoint,
                       @NotNull RailsResourceFactory<T> resourceFactory, @NotNull AllowedOps permissions) {

        super(server, dao, new SparseArrayResourceCache<T>(50), resourceFactory, permissions);
        server.addEndpoint(dao.getDataClass(), endpoint);
    }

    public abstract static class RailsResourceFactory<R extends Resource> implements ResourceFactory<R> {

        private String jsonSingleObjectKey;
        private String jsonArrayObjectKey;

        public RailsResourceFactory(String jsonSingleObjectKey, String jsonArrayObjectKey) {
            this.jsonSingleObjectKey = jsonSingleObjectKey;
            this.jsonArrayObjectKey = jsonArrayObjectKey;
        }

        @Override
        public boolean updateItem(R item, JSONObject data) throws JSONException {
            return item.updateFromJSON(data.getJSONObject(jsonSingleObjectKey));
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
    }
}
