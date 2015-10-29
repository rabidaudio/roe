package audio.rabid.dev.sampleapp.models;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

import org.jetbrains.annotations.Nullable;
import org.json.JSONException;
import org.json.JSONObject;

import audio.rabid.dev.network_orm.Resource;
import audio.rabid.dev.network_orm.Source;

/**
 * Created by charles on 10/25/15.
 */
@DatabaseTable(tableName = "posts")
public class Post extends Resource<Post> {

    @SuppressWarnings("unchecked")
    public static final PostSource Source = new PostSource();

    @Override
    public Source<Post> getSource(){
        return Source;
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
                .put("author_id", author.getId())
                .put("title", title)
                .put("body", body)
                .put("liked", liked);
    }

    @Override
    protected synchronized boolean updateFromJSON(JSONObject data) throws JSONException {
        boolean updated = super.updateFromJSON(data);
        String t = data.getString("title");
        String b = data.getString("body");
        int l = data.getInt("likes");

        if(title==null || !title.equals(t)){
            title = t;
            updated = true;
        }
        if(body==null || !body.equals(b)){
            body = b;
            updated = true;
        }
        if(l != likes){
            likes = l;
            updated = true;
        }
        //TODO this is hacky...
        if(!data.isNull("author")) {
            int authorServerId = data.getJSONObject("author").getInt("id");
            if (author == null || author.getServerId() != authorServerId) {
                author = Author.Source.getServerSync(authorServerId);
                updated = true;
            }
        }
        return updated;
    }
}
