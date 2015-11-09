package audio.rabid.dev.roe.models;

import android.support.annotation.Nullable;
import android.test.AndroidTestCase;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Date;

import audio.rabid.dev.roe.BackgroundThread;
import audio.rabid.dev.roe.Synchronizer;
import audio.rabid.dev.roe.testobjects.DummyObject;
import audio.rabid.dev.roe.testobjects.DummyObjectSource;
import audio.rabid.dev.roe.testobjects.GenericDatabase;

/**
 * Created by charles on 11/2/15.
 */
public class JSONTest extends AndroidTestCase {

    public void setUp(){
        GenericDatabase.getInstance(getContext()).clearDatabase();
    }

    public void testJSONToQueryString() throws Exception {

        String root = "http://example.com/api/";

        assertEquals(root, Server.buildQueryString(root, null).toExternalForm());

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

    public void testReflectiveConvertToJSON() throws Exception {

        final DummyObject o = new DummyObject("meow", 15, null);

        JSONObject json = DummyObjectSource.getInstance().toJSON(o);

        assertEquals("meow", json.getString("name"));
        assertEquals(15, json.getInt("age"));
        assertTrue(json.isNull("created_at"));
        assertTrue(json.isNull("updated_at"));
        assertFalse(json.has("id"));

        new Synchronizer<DummyObject>() {
            @Override
            public void run() {
                o.save(new Source.OperationCallback<DummyObject>() {
                    @Override
                    public void onResult(@Nullable DummyObject result) {
                        setResult(result);
                    }
                });
            }
        }.blockUntilFinished();
        new Synchronizer<Void>() {
            @Override
            public void run() {
                BackgroundThread.postBackground(new Runnable() {
                    @Override
                    public void run() {
                        while (!DummyObjectSource.getInstance().wasCreateCompleted()) { /*block*/ }
                        DummyObjectSource.getInstance().clearCreateCompleted();
                        setResult(null);
                    }
                });
            }
        }.blockUntilFinished();

        json = DummyObjectSource.getInstance().toJSON(o);

        assertEquals("meow", json.getString("name"));
        assertEquals(15, json.getInt("age"));
        assertEquals(o.getCreatedAt().getTime(), DummyObjectSource.getInstance().getDateFormat().parse(json.getString("created_at")).getTime());
        assertTrue(json.getInt("id") > 0);
    }

    public void testReflectivePopulateFromJSON() throws Exception {

        DummyObject o = new DummyObject();

        JSONObject data = new JSONObject()
                .put("id", 25)
                .put("name", "meow")
                .put("age", 15)
                .put("created_at", DummyObjectSource.getInstance().getDateFormat().format(new Date()))
                .put("updated_at", DummyObjectSource.getInstance().getDateFormat().format(new Date()));

        boolean changed = DummyObjectSource.getInstance().updateFromJSON(o, data);

        assertTrue(changed);
        assertEquals("meow", o.getName());
        assertEquals(15, o.getAge());
        assertEquals(Integer.valueOf(25), o.getServerId());
        assertNull(o.createdAt);
        assertNull(o.updatedAt);

        changed = DummyObjectSource.getInstance().updateFromJSON(o, data);
        assertFalse(changed);
    }
}
