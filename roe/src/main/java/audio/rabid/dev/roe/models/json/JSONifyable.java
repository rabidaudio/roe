package audio.rabid.dev.roe.models.json;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by charles on 11/11/15.
 */
public interface JSONifyable {

    JSONObject asJSON() throws JSONException;

    void fromJSON(JSONObject object) throws JSONException;
}
