package audio.rabid.dev.sampleapp.models;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

import org.json.JSONException;
import org.json.JSONObject;

import audio.rabid.dev.network_orm.AllowedOps;
import audio.rabid.dev.network_orm.Resource;
import audio.rabid.dev.network_orm.ResourceCreator;
import audio.rabid.dev.network_orm.Source;
import audio.rabid.dev.sampleapp.Database;
import audio.rabid.dev.sampleapp.SampleAppServer;

/**
 * Created by charles on 10/25/15.
 */
@DatabaseTable(tableName = "posts")
public class Post extends Resource<Post> {

    private static final String ENDPOINT = "posts";

    @SuppressWarnings("unchecked")
    public static final Source<Post> Source = new Source<>(SampleAppServer.getInstance(), Database.getDaoOrThrow(Post.class), ENDPOINT, new PostResourceCreator());

    @Override
    public Source<Post> getSource(){
        return Source;
    }

    @Override
    public AllowedOps getAllowedOps() {
        return AllowedOps.ALL;
    }

    @DatabaseField
    private String title;

    @DatabaseField
    private String body;

    @DatabaseField
    private int likes;

    @DatabaseField
    private boolean liked;

    @DatabaseField(foreign = true, foreignAutoRefresh = true)
    private Author author;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public int getLikes() {
        return likes;
    }

    public void like() {
        liked = true;
    }

    public void unlike(){
        liked = false;
    }

    public Author getAuthor() {
        return author;
    }

    public void setAuthor(Author author) {
        this.author = author;
    }

    @Override
    public JSONObject toJSON() throws JSONException {
        return super.toJSON()
                .put("title", title)
                .put("body", body)
                .put("liked", liked);
    }

    @Override
    protected boolean updateFromJSON(JSONObject data) throws JSONException {
        boolean updated = super.updateFromJSON(data);;
        String t = data.getString("title");
        String b = data.getString("body");
        int l = data.getInt("likes");
        if(!title.equals(t)){
            title = t;
            updated = true;
        }
        if(!body.equals(b)){
            body = b;
            updated = true;
        }
        if(l != likes){
            likes = l;
            updated = true;
        }
        return updated;
    }

    public static class PostResourceCreator extends ResourceCreator<Post>{

        @Override
        public Post createFromJSON(JSONObject json) throws JSONException {
            Post p = new Post();
            p.updateFromJSON(json);
            return p;
        }

        @Override
        public String jsonArrayContainerKey() {
            return "posts";
        }
    }
}
