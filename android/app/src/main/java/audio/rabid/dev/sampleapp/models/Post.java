package audio.rabid.dev.sampleapp.models;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

import org.json.JSONException;
import org.json.JSONObject;

import audio.rabid.dev.network_orm.AllowedOps;
import audio.rabid.dev.network_orm.Dao;
import audio.rabid.dev.network_orm.Resource;
import audio.rabid.dev.sampleapp.Database;

/**
 * Created by charles on 10/25/15.
 */
@DatabaseTable(tableName = "posts")
public class Post extends Resource<Post> {

    @SuppressWarnings("unchecked")
    public static Dao<Post> Dao = new Dao<>(Database.getDBDao(Post.class));

    @Override
    public Dao<Post> getDao() {
        return Dao;
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
    protected void updateFromJSON(JSONObject data) throws JSONException{
        super.updateFromJSON(data);
        title = data.getString("title");
        body = data.getString("body");
        likes = data.getInt("likes");
    }
}
