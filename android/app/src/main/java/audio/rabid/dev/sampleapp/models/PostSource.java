package audio.rabid.dev.sampleapp.models;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.table.ObjectFactory;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Constructor;
import java.sql.SQLException;
import java.util.List;

import audio.rabid.dev.network_orm.AllowedOps;
import audio.rabid.dev.network_orm.RailsSource;
import audio.rabid.dev.sampleapp.Database;
import audio.rabid.dev.sampleapp.SampleAppServer;

/**
 * Created by charles on 10/29/15.
 */
public class PostSource extends RailsSource<Post> {

    @SuppressWarnings("unchecked")
    public PostSource() {
        super(SampleAppServer.getInstance(), Database.getDaoOrThrow(Post.class), "posts", new PostResourceFactory(), AllowedOps.ALL);

        //should be faster than reflection at creating new instances
        getDao().setObjectFactory(new ObjectFactory<Post>() {
            @Override
            public Post createObject(Constructor<Post> construcor, Class<Post> dataClass) throws SQLException {
                return new Post();
            }
        });
    }

    public void getRecentByAuthor(final int authorId, final long limit, QueryCallback<List<Post>> callback) {
        doMultipleLocalQuery(callback, new MultipleLocalQuery<Post>() {
            @Override
            public List<Post> query(Dao<Post, Integer> dao) throws SQLException {
                return dao.queryBuilder().orderBy("createdAt", false).limit(limit).where().eq("author_id", authorId).query();
            }
        });
    }

    public void allByAuthorOrAll(final int authorId, QueryCallback<List<Post>> callback) {
        //first download any new items
        try {
            JSONObject query = authorId < 0 ? null : new JSONObject().put("author_id", authorId);
            createOrUpdateManyFromNetwork(query, null);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }

        //then get all local items, which should be fast for the ones returned by the last call since they are already cached
        doMultipleLocalQuery(callback, new MultipleLocalQuery<Post>() {
            @Override
            public List<Post> query(Dao<Post, Integer> dao) throws SQLException {
                if (authorId < 0) {
                    return dao.queryForAll();
                } else {
                    return getDao().queryForEq("author_id", authorId);
                }
            }
        });
    }

    public static class PostResourceFactory extends RailsResourceFactory<Post> {

        public PostResourceFactory() {
            super("post", "posts");
        }

        @Override
        public Post createFromJSON(JSONObject json) throws JSONException {
            Post p = new Post();
            p.updateFromJSON(json);
            return p;
        }
    }

}
