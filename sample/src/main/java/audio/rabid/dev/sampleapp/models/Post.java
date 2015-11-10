package audio.rabid.dev.sampleapp.models;

import android.support.annotation.Nullable;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

import audio.rabid.dev.roe.models.IntegerKeyedNetworkResource;
import audio.rabid.dev.roe.models.JSONField;
import audio.rabid.dev.roe.models.Source;

/**
 * Created by charles on 10/25/15.
 */
@DatabaseTable(tableName = "posts")
public class Post extends IntegerKeyedNetworkResource {

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

    public static PostSource Source = new PostSource();

    @Override
    public Source<Post, Integer> getSource() {
        return Source;
    }

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

    public void save(@Nullable Source.OperationCallback<Post> callback){
        getSource().createOrUpdate(this, callback);
    }

    public void delete(@Nullable Source.OperationCallback<Post> callback){
        getSource().delete(this, callback);
    }
}
