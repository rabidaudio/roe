package audio.rabid.dev.sampleapp.models;

import org.json.JSONException;
import org.json.JSONObject;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import audio.rabid.dev.network_orm.AllowedOps;
import audio.rabid.dev.network_orm.ResourceCreator;
import audio.rabid.dev.network_orm.Source;
import audio.rabid.dev.sampleapp.Database;
import audio.rabid.dev.sampleapp.SampleAppServer;

/**
 * Created by charles on 10/29/15.
 */
public class PostSource extends Source<Post> {

    @SuppressWarnings("unchecked")
    public PostSource() {
        super(SampleAppServer.getInstance(), Database.getDaoOrThrow(Post.class),
                "posts", "post", "posts", new PostResourceCreator(), AllowedOps.ALL);
    }

    public void getRecentByAuthor(final int authorId, final long limit, QueryCallback<List<Post>> callback){
        (new Source.SourceAsyncTask<List<Post>>(callback){
            @Override
            protected List<Post> runInBackground() {
                try {
                    List<Post> results = getDao().queryBuilder().orderBy("createdAt", false)
                            .limit(limit).where().eq("author_id", authorId).query();
                    List<Post> returnResults = new ArrayList<>(results.size());
                    for (Post p : results) {
                        returnResults.add(cacheGetNetworkUpdateOnMiss(p));
                    }
                    return returnResults;
                }catch (SQLException e){
                    throw new RuntimeException(e);
                }
            }
        }).execute();
    }

    public void allByAuthorOrAll(final int authorId, QueryCallback<List<Post>> callback){
        if(authorId == -1) {
            remoteSearch(null, callback);
        }else {
            (new SourceAsyncTask<List<Post>>(callback) {
                @Override
                protected List<Post> runInBackground() {
                    try {
                        List<Post> results;
                        results = getDao().queryForEq("author_id", authorId);
                        List<Post> returnResults = new ArrayList<>(results.size());
                        for (Post p : results) {
                            returnResults.add(cacheGetNetworkUpdateOnMiss(p));
                        }
                        return returnResults;
                    } catch (SQLException e) {
                        throw new RuntimeException(e);
                    }
                }
            }).execute();
        }
    }

    public static class PostResourceCreator implements ResourceCreator<Post> {

        @Override
        public Post createFromJSON(JSONObject json) throws JSONException {
            Post p = new Post();
            p.updateFromJSON(json);
            return p;
        }
    }

}
