package audio.rabid.dev.network_orm.models.rails;

import android.support.annotation.NonNull;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.support.ConnectionSource;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.List;

import audio.rabid.dev.network_orm.models.PermissionsManager;
import audio.rabid.dev.network_orm.models.Resource;
import audio.rabid.dev.network_orm.models.ResourceFactory;
import audio.rabid.dev.network_orm.models.Source;
import audio.rabid.dev.network_orm.models.cache.SparseArrayResourceCache;

/**
 * Created by charles on 10/29/15.
 */
public class RailsSource<T extends Resource> extends Source<T> {

    public RailsSource(@NonNull RailsServer server, @NonNull Dao<T, Integer> dao, @NonNull String endpoint,
                       @NonNull RailsResourceFactory<T> resourceFactory, @NonNull PermissionsManager<T> permissions, @NonNull ConnectionSource connectionSource) {

        super(server, dao, new SparseArrayResourceCache<T>(50), resourceFactory, permissions, new NetworkDateFormat(), connectionSource);
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
        public boolean updateItemDirect(R item, JSONObject data) throws JSONException {
            return item.updateFromJSON(data);
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

        @Override
        public JSONObject turnItemIntoValidServerPayload(R item) throws JSONException {
            return new JSONObject().put(jsonSingleObjectKey, item.toJSON());
        }
    }
}
