package audio.rabid.dev.network_orm;

import android.util.Log;

import com.j256.ormlite.dao.Dao;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by charles on 10/29/15.
 */
public class RailsResourceSource<T extends Resource> extends Source<T> {

//    private String jsonSingleObjectKey;
//    private String jsonArrayObjectKey;
//    private String endpoint;

    public RailsResourceSource(@NotNull Server server, @NotNull Dao<T, Integer> dao, @NotNull String endpoint,
                  @NotNull String jsonSingleObjectKey, @NotNull String jsonArrayObjectKey,
                  @NotNull ResourceFactory<T> resourceFactory, @NotNull AllowedOps permissions){

        super(server, dao, new SparseArrayResourceCache<T>(50), new RailsResourceFactory<T>(), permissions);
//        this.jsonSingleObjectKey = jsonSingleObjectKey;
//        this.jsonArrayObjectKey = jsonArrayObjectKey;
//        this.endpoint = endpoint;
    }

    private static class RailsResourceFactory<R extends Resource> extends ResourceFactory<R> {

        @Override
        public R createFromJSON(JSONObject json) throws JSONException {
            return null;
        }

        @Override
        public boolean updateItem(R item, JSONObject data) throws JSONException {
            return false;
        }

        @Override
        public List<R> createManyFromJSON(JSONObject data) throws JSONException {
            return null;
        }
    }
}
