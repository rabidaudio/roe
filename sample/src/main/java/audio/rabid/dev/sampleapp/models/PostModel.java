package audio.rabid.dev.sampleapp.models;

import com.snappydb.DB;

import org.json.JSONException;
import org.json.JSONObject;

import audio.rabid.dev.sampleapp.BaseModel;

/**
 * Created by charles on 10/29/15.
 */
public class PostModel extends BaseModel<Post> {

    public PostModel(DB database) {
        super(database, "/posts/:key");
    }

    @Override
    public Post fromJSON(JSONObject object) throws JSONException {
        Post p = new Post();
        p.fromJSON(object);
        return p;
    }

    @Override
    public JSONObject toJSON(Post object) throws JSONException {
        return object.asJSON();
    }

    @Override
    public String getKey(Post object) {
        return String.valueOf(object.id);
    }
}
