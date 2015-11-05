package audio.rabid.dev.sampleapp.models;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.table.ObjectFactory;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Constructor;
import java.sql.SQLException;
import java.util.List;

import audio.rabid.dev.roe.models.SimplePermissionsManager;
import audio.rabid.dev.roe.models.rails.RailsResourceFactory;
import audio.rabid.dev.roe.models.rails.RailsSource;
import audio.rabid.dev.sampleapp.Database;
import audio.rabid.dev.sampleapp.SampleAppServer;

/**
 * Created by charles on 10/29/15.
 */
public class PostSource extends RailsSource<Post> {

    @SuppressWarnings("unchecked")
    public PostSource() {
        super(SampleAppServer.getInstance(),
                Database.getDaoOrThrow(Post.class),
                Database.getInstance().getConnectionSource(), "posts",
                new RailsResourceFactory<>(Post.class, "post", "posts"),
                new SimplePermissionsManager<Post>().all());

        //should be faster than reflection at creating new instances
        getDao().setObjectFactory(new ObjectFactory<Post>() {
            @Override
            public Post createObject(Constructor<Post> construcor, Class<Post> dataClass) throws SQLException {
                return new Post();
            }
        });
    }

    public void getRecentByAuthor(final @NonNull Integer authorId, final long limit, OperationCallback<List<Post>> callback) {
        executeLocalQuery(new LocalQuery<Post>() {
            @Override
            public List<Post> executeQuery(Dao<Post, Integer> dao) throws SQLException {
                return dao.queryBuilder().orderBy("createdAt", false).limit(limit).where().eq("author_id", authorId).query();
            }
        }, callback);
    }


    /**
     * This callback will be called up to two times. Once after fetching local results and possibly
     * again after network results hit, if the results have changed. It will combine the two lists on
     * the second call.
     */
    public void allByAuthorOrAll(final @Nullable Integer authorId, final OperationCallback<List<Post>> callback) {
        //first return any local items
        executeLocalQuery(new LocalQuery<Post>() {
            @Override
            public List<Post> executeQuery(Dao<Post, Integer> dao) throws SQLException {
                if (authorId == null) {
                    return dao.queryForAll();
                } else {
                    return getDao().queryForEq("author_id", authorId);
                }
            }
        }, new OperationCallback<List<Post>>() {
            @Override
            public void onResult(@Nullable List<Post> result) {
                final List<Post> localResults = result;
                callback.onResult(result);
                //then update with latest server data
                if(authorId==null){
                    getManyFromNetwork(null, new OperationCallback<List<Post>>() {
                        @Override
                        public void onResult(@Nullable List<Post> result) {
                            if(localResults==null){
                                if(result != null && !result.isEmpty()){
                                    callback.onResult(result);
                                }
                            }else if(result != null) {
                                for(Post r : result){
                                    if(!localResults.contains(r)){
                                        localResults.add(r);
                                    }
                                }
                                callback.onResult(localResults);
                            }
                        }
                    });
                }else{
                    Author.Source.find(authorId, new OperationCallback<Author>() {
                        @Override
                        public void onResult(@Nullable Author result) {
                            if(result!=null) {
                                try{
                                    getManyFromNetwork(new JSONObject().put("author_id", result.getServerId()), new OperationCallback<List<Post>>() {
                                        @Override
                                        public void onResult(@Nullable List<Post> result) {
                                            if (localResults == null) {
                                                if (result != null && !result.isEmpty()) {
                                                    callback.onResult(result);
                                                }
                                            } else if (result != null) {
                                                for (Post r : result) {
                                                    if (!localResults.contains(r)) {
                                                        localResults.add(r);
                                                    }
                                                }
                                                callback.onResult(localResults);
                                            }
                                        }
                                    });
                                }catch (JSONException e){
                                    throw new RuntimeException(e);
                                }
                            }
                        }
                    });
                }
            }
        });
    }
}
