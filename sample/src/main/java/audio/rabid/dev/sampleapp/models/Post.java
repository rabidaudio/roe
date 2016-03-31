package audio.rabid.dev.sampleapp.models;

import java.util.Date;

import audio.rabid.dev.roe.models.json.JSONField;
import audio.rabid.dev.roe.models.json.SimpleJSONifyable;

/**
 * Created by charles on 10/25/15.
 */
public class Post extends SimpleJSONifyable {

    @JSONField
    protected int id;

    @JSONField
    private String title;

    @JSONField
    private String body;

    @JSONField
    private int likes;

    @JSONField
    private boolean liked;

    @JSONField
    private Date createdAt;

    @JSONField
    private Date updatedAt;

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

    public Date getCreatedAt() {
        return createdAt;
    }

    public Date getUpdatedAt() {
        return updatedAt;
    }

    public int getId() {
        return id;
    }
}
