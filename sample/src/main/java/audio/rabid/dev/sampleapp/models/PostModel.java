package audio.rabid.dev.sampleapp.models;

import com.snappydb.DB;

import org.json.JSONException;
import org.json.JSONObject;

import audio.rabid.dev.sampleapp.BaseModel;

/**
 * Created by charles on 10/29/15.
 */
public class PostModel extends BaseModel<Post, Integer> {

    public PostModel(DB database) {
        super(database, "/posts");
    }

    @Override
    public Post newInstance() {
        return new Post();
    }

    @Override
    public Integer getID(Post object) {
        return object.getId();
    }
}
