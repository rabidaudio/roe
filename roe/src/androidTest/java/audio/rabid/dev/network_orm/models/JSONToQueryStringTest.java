package audio.rabid.dev.network_orm.models;

import android.net.Uri;
import android.test.AndroidTestCase;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Created by charles on 11/2/15.
 */
public class JSONToQueryStringTest extends AndroidTestCase {

    public void testJSONToQueryString() throws Exception {

        String root = "http://example.com/api/";

        assertEquals(root, Server.buildQueryString(root, null).toExternalForm());

//        assertEquals(root, Server.buildQueryString(root, new JSONObject("")).toExternalForm());

        JSONObject simpleSingleKey = new JSONObject().put("q", "my search");
        assertEquals(root+"?q=my%20search", Server.buildQueryString(root, simpleSingleKey).toExternalForm());

        JSONObject simpleMultipleKey = new JSONObject().put("q", 1).put("user_id", 13);
        assertEquals(root+"?q=1&user_id=13", Server.buildQueryString(root, simpleMultipleKey).toExternalForm());

        JSONObject simpleArray = new JSONObject().put("q", new JSONArray("[1,2,3]"));
        assertEquals(root+"?q%5B%5D=1&q%5B%5D=2&q%5B%5D=3", Server.buildQueryString(root, simpleArray).toExternalForm());

        JSONObject arrayAndKey = new JSONObject().put("q", new JSONArray("[1,2,3]")).put("user_id", 13);
        assertEquals(root+"?q%5B%5D=1&q%5B%5D=2&q%5B%5D=3&user_id=13", Server.buildQueryString(root, arrayAndKey).toExternalForm());

        JSONObject nestedArray = new JSONObject().put("q", new JSONArray("[1, [2,3]]"));
        assertEquals(root+"?q%5B%5D=1&q%5B%5D%5B%5D=2&q%5B%5D%5B%5D=3", Server.buildQueryString(root, nestedArray).toExternalForm());

        JSONObject object = new JSONObject().put("q", new JSONObject().put("a", "b").put("c", "d"));
        assertEquals(root+"?q%5Ba%5D=b&q%5Bc%5D=d", Server.buildQueryString(root, object).toExternalForm());

        JSONObject hardMode = new JSONObject()
                .put("q", new JSONObject()
                        .put("a","b")
                        .put("c", new JSONObject().put("cat", "meow").put("dog", "bark"))
                        .put("d", new JSONArray("[1,2]")))
                .put("key", JSONObject.NULL);
        assertEquals(root+"?q%5Ba%5D=b&q%5Bc%5D%5Bcat%5D=meow&q%5Bc%5D%5Bdog%5D=bark&q%5Bd%5D%5B%5D=1&q%5Bd%5D%5B%5D=2&key=null",
                Server.buildQueryString(root, hardMode).toExternalForm());
    }
}
