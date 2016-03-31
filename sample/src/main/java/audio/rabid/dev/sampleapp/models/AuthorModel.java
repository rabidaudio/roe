package audio.rabid.dev.sampleapp.models;

import com.snappydb.DB;

import org.json.JSONException;
import org.json.JSONObject;

import audio.rabid.dev.sampleapp.BaseModel;

/**
 * Created by  charles  on 3/30/16.
 */
public class AuthorModel extends BaseModel<Author> {

    public AuthorModel(DB database) {
        super(database, "/authors/:key");
    }

    @Override
    public Author fromJSON(JSONObject object) throws JSONException {
        Author a = new Author();
        a.fromJSON(object);
        return a;
    }

    @Override
    public JSONObject toJSON(Author object) throws JSONException {
        return object.asJSON();
    }

    @Override
    public String getKey(Author object) {
        return String.valueOf(object.id);
    }
}
