package audio.rabid.dev.sampleapp.backend;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

/**
 * Created by charles on 10/23/15.
 */
@DatabaseTable(tableName = "authors")
public class Author {

    public static Dao<Author> Dao = Database.getInstance().getRuntimeExceptionDao(Author.class);

    @DatabaseField(id=true)
    private int id;

    @DatabaseField(unique = true, index = true)
    private int serverId;

    @DatabaseField
    private String name;

    @DatabaseField
    private String email;

    @DatabaseField
    private String avatar;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getAvatar() {
        return avatar;
    }

    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }

    public void save(){
        Dao.createOrUpdate(this);
    }

    public void delete(){
        Dao.delete(this);
    }

    public JSONObject toJSON() throws JSONException{
        return new JSONObject()
                .put("id", id)
                .put("serverId", serverId)
                .put("name", name)
                .put("email", email)
                .put("avatar", avatar);
    }
}
