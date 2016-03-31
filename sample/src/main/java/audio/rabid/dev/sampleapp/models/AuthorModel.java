package audio.rabid.dev.sampleapp.models;

import com.snappydb.DB;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.HttpURLConnection;

import audio.rabid.dev.sampleapp.BaseModel;

/**
 * Created by  charles  on 3/30/16.
 */
public class AuthorModel extends BaseModel<Author, Integer> {

    public AuthorModel(DB database) {
        super(database, "/authors");
    }

    @Override
    public Author newInstance() {
        return new Author();
    }

    @Override
    public Integer getID(Author object) {
        return object.getId();
    }
}
