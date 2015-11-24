package audio.rabid.dev.sampleapp.models;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.ObjectFactory;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Constructor;
import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

import audio.rabid.dev.roe.ThreadHandler;
import audio.rabid.dev.roe.models.NetworkSyncableDao;

/**
 * Created by charles on 10/29/15.
 */
public class PostDao extends NetworkSyncableDao<Post, Integer, Integer> {


    public PostDao(ConnectionSource connectionSource, Class<Post> dataClass) throws SQLException {
        super(connectionSource, dataClass);
        //should be faster than reflection at creating new instances
        setObjectFactory(new ObjectFactory<Post>() {
            @Override
            public Post createObject(Constructor<Post> construcor, Class<Post> dataClass) throws SQLException {
                return new Post();
            }
        });
    }


    public Future<List<Post>> getRecentByAuthor(final @NonNull Integer authorId, final long limit, @Nullable OperationCallback<List<Post>> callback) {
        return doAsync(new Callable<List<Post>>() {
            @Override
            public List<Post> call() throws Exception {
                return queryBuilder().orderBy("createdAt", false).limit(limit).where().eq("author_id", authorId).query();
            }
        }, callback);
    }

    /**
     * This callback will be called up to two times. Once after fetching local results and possibly
     * again after network results hit, if the results have changed. It will combine the two lists on
     * the second call.
     */
    public Future<List<Post>> allByAuthorOrAll(final @Nullable Integer authorId, @Nullable final OperationCallback<List<Post>> callback) {
        //first return any local items
        return doAsync(new Callable<List<Post>>() {
            @Override
            public List<Post> call() throws Exception {
                if (authorId == null) {
                    return queryForAll();
                } else {
                    return queryForEq("author_id", authorId);
                }
            }
        }, new OperationCallback<List<Post>>() {
            @Override
            public void onResult(final List<Post> result) {
                if(callback != null){
                    callback.onResult(result);
                }
                //now update from network
                doAsync(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            JSONObject search = null;
                            if (authorId != null) {
                                Author a = Author.AuthorDao.queryForId(authorId);
                                if (a != null) {
                                    search = new JSONObject().put("author_id", a.getServerId());
                                }
                            }
                            //callback should set observable on collection to see when new results are available
                            getFromNetwork("serverId", search, null);
                        }catch (SQLException | JSONException e){
                            throw new RuntimeException(e);
                        }
                    }
                });
            }
        });
    }

    @Override
    protected void onCreated(Post item) {
        try {
            //auto create/update Author relation
            Author actual = Author.AuthorDao.getByServerId("id", item.getAuthor().getServerId());
            if(actual != null){
                item.setAuthor(actual);
            }
        }catch (SQLException e){
            throw new RuntimeException(e);
        }
        super.onCreated(item);
    }

    @Override
    public void onSQLException(SQLException e){
        Log.e("OHSHI", "database exception", e);
    }

    @Override
    public void onJSONException(JSONException e){
        Log.e("OHSHI", "json exception", e);
    }
}
