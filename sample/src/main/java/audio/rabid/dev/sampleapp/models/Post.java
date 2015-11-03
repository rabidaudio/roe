package audio.rabid.dev.sampleapp.models;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

import audio.rabid.dev.roe.models.JSONField;
import audio.rabid.dev.roe.models.Resource;
import audio.rabid.dev.roe.models.Source;

/**
 * Created by charles on 10/25/15.
 */
@DatabaseTable(tableName = "posts")
public class Post extends Resource<Post> {

    @SuppressWarnings("unchecked")
    public static final PostSource Source = new PostSource();

    @Override
    public Source<Post> getSource() {
        return Source;
    }

    @JSONField
    @DatabaseField
    private String title;

    @JSONField
    @DatabaseField
    private String body;

    @JSONField
    @DatabaseField
    private int likes;

    @JSONField
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

    public void unlike() {
        liked = false;
    }

    public Author getAuthor() {
        return author;
    }

    public void setAuthor(Author author) {
        this.author = author;
    }
}
