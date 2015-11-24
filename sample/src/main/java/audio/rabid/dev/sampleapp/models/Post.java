package audio.rabid.dev.sampleapp.models;

import android.support.annotation.Nullable;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

import java.util.Date;
import java.util.concurrent.Future;

import audio.rabid.dev.roe.models.NetworkSyncableDao;
import audio.rabid.dev.roe.models.json.JSONField;
import audio.rabid.dev.roe.models.rails.RailsServer;
import audio.rabid.dev.roe.models.rails.RailsServerResource;
import audio.rabid.dev.roe.models.resource.IntegerKeyedResource;
import audio.rabid.dev.sampleapp.Database;

/**
 * Created by charles on 10/25/15.
 */
@RailsServerResource(endpoint = "/posts", includes = {"author"})
@DatabaseTable(tableName = "posts", daoClass = PostDao.class)
public class Post extends IntegerKeyedResource {

    public static PostDao PostDao = Database.getInstance().getDao(Post.class);

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

    @JSONField
    @DatabaseField(foreign = true, foreignAutoRefresh = true)
    private Author author;

    @JSONField(serializer = RailsServer.RailsDateSerializer.class)
    @DatabaseField
    private Date createdAt = new Date();

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

    public Date getCreatedAt(){
        return createdAt;
    }

    public Future<Post> save(@Nullable NetworkSyncableDao.OperationCallback<Post> callback){
        return PostDao.saveAsync(this, callback);
    }

    public Future<Post> delete(@Nullable NetworkSyncableDao.OperationCallback<Post> callback){
        return PostDao.deleteAsync(this, callback);
    }
}
