package audio.rabid.dev.roe.models.json;

import android.test.AndroidTestCase;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * Created by charles on 11/11/15.
 *
 * On a Galaxy SII running Lollipop, serializing the test object takes less than 10ms and deserializing
 * takes less than 20. Not sure how this compares to GSON, but I bet it is faster.
 */
public class ReflectiveJSONSerializationTest extends AndroidTestCase {

    public void testJSONSerialization() throws Exception {

        SimpleObject s = new SimpleObject();

        long start = System.nanoTime();
        JSONObject o = s.asJSON();
        long end = System.nanoTime();
        Log.d("test", "serialized in " + (end - start) / 1000d / 1000d + "ms.");

        JSONObject object = new JSONObject(o.toString()); //convert to string and back

        assertEquals("meow", object.getString("aString"));
        assertEquals(15, object.getInt("anInt"));
        assertEquals(Math.PI, object.getDouble("aDouble"));

        assertFalse(object.has("noSerialize"));
        assertEquals("wow", object.getString("noDeserialize"));

        assertFalse(object.has("hidden"));

        JSONArray array1 = object.getJSONArray("intArray");
        assertEquals(3, array1.length());
        assertEquals(1, array1.get(0));
        JSONArray array2 = object.getJSONArray("stringArray");
        assertEquals(2, array2.length());
        assertEquals("two", array2.getString(1));

        assertTrue(object.has("nullString"));
        assertTrue(object.isNull("nullString"));

        assertEquals("Wed Nov 11 19:57:56 EST 2015", object.getString("myDate"));

        assertEquals("2015-11-11T19:57:56.000", object.getString("mySpecialDate"));

        JSONArray array = object.getJSONArray("customSerializer");
        assertEquals(7, array.length());
        assertEquals("a", array.getString(0));
        assertEquals("g", array.getString(6));

        assertTrue(object.has("child"));
        JSONObject child = object.getJSONObject("child");
        assertEquals("meow", child.getString("meow"));
        assertEquals(3, child.getJSONArray("floats").length());
        assertEquals(.99, child.getJSONArray("floats").getDouble(2));

        assertEquals(3.141, object.getDouble("f"));

        assertEquals(1, object.getJSONArray("childList").length());
        assertEquals(2, object.getJSONArray("childArray").length());

        assertEquals(13, object.getInt("shorty"));
        assertEquals(127, object.getInt("bite"));

        assertEquals(2, object.getJSONArray("listyList").length());
        assertEquals(2, object.getJSONArray("listyList").getJSONArray(0).length());
        assertEquals(2, object.getJSONArray("listyList").getJSONArray(1).length());
    }

    public void testJSONDeserialization() throws Exception {

        SimpleObject s = new SimpleObject();

        ArrayList<String> strings = new ArrayList<>();
        strings.add("nine");
        strings.add("ten");

        JSONObject child = new JSONObject().put("meow", "bark").put("floats", new JSONArray().put(3.33));
        JSONObject o = new JSONObject()
                .put("aString", "bark")
                .put("anInt", 7)
                .put("aDouble", 9001.1234)
                .put("noSerialize", "xxx")
                .put("noDeserialize", "yyy")
                .put("different", "riotgrrrl")
                .put("intArray", new JSONArray().put(4).put(5).put(6))
                .put("stringArray", new JSONArray(strings))
                .put("hidden", "sniper")
                .put("nullString", "not null")
                .put("anotherString", JSONObject.NULL)
                .put("myDate", new Date(1448888888000l))
                .put("mySpecialDate", "2015-11-11T11:11:11.000")
                .put("customSerializer", new JSONArray(Arrays.asList(new String[]{"1", "2", "3", "4", "5", "6", "7"})))
                .put("child", child)
                .put("f", .1234f)
                .put("childList", new JSONArray().put(child))
                .put("childArray", new JSONArray().put(child).put(child))
                .put("shorty", 11)
                .put("bite", 9)
                .put("listyList", new JSONArray()
                        .put(new JSONArray().put("one"))
                        .put(new JSONArray().put("two")));

        JSONObject object = new JSONObject(o.toString()); //convert to string and back

        long start = System.nanoTime();
        s.fromJSON(object);
        long end = System.nanoTime();
        Log.d("test", "deserialized in " + (end - start) / 1000d / 1000d + "ms.");

        assertEquals(s.aString, "bark");
        assertEquals(s.anInt, 7);
        assertEquals(s.aDouble, 9001.1234);
        assertEquals(s.noSerialize, "xxx");
        assertFalse(s.noDeserialize.equals("yyy"));
        assertEquals(s.differentKey, "riotgrrrl");
        assertEquals(3, s.intArray.length);
        assertEquals(4, (int) s.intArray[0]);
        assertEquals(2, s.stringArray.size());
        assertEquals("ten", s.stringArray.get(1));
        assertFalse(s.hidden.equals("sniper"));
        assertNotNull(s.nullString);
        assertNull(s.anotherString);
        assertEquals(1448888888000l, s.myDate.getTime());
        assertEquals(1447258271000l, s.mySpecialDate.getTime());

        assertEquals("1234567", s.customSerializer);

        assertNotNull(s.child);
        assertEquals("bark", s.child.meow);
        assertEquals(1, s.child.floats.length);
        assertEquals(3.33f, s.child.floats[0]);

        assertEquals(.1234f, s.f);

        assertEquals(1, s.childList.size());
        assertEquals(3.33f, s.childList.get(0).floats[0]);

        assertEquals(2, s.childArray.length);
        assertEquals(3.33f, s.childArray[1].floats[0]);

        assertEquals(11, s.shorty);
        assertEquals(9, s.bite);

        assertEquals(2, s.listyList.length);
        assertEquals(1, s.listyList[0].length);
        assertEquals("one", s.listyList[0][0]);
        assertEquals("two", s.listyList[1][0]);
    }


    public static class SimpleObject extends SimpleJSONifyable {

        @JSONField
        private String aString = "meow";

        @JSONField
        protected int anInt = 15;

        @JSONField
        public double aDouble = Math.PI;

        @JSONField(serialize = false)
        private String noSerialize = "sneaky";

        @JSONField(deserialize = false)
        private String noDeserialize = "wow";

        @JSONField(key = "different")
        private String differentKey = "rebel";

        @JSONField
        private Integer[] intArray = new Integer[]{1, 2, 3};

        @JSONField
        private List<String> stringArray = new ArrayList<>();

        private String hidden = "hiding";

        @JSONField
        private String nullString = null;

        @JSONField
        private String anotherString = null;

        @JSONField
        private Date myDate = new Date(1447289876000l);

        @JSONField(dateFormat = "yyyy-MM-dd'T'HH:mm:ss.SSS")
        private Date mySpecialDate = new Date(1447289876000l);

        @JSONField(serializer = CustomSerializer.class)
        String customSerializer = "abcdefg";

        @JSONField
        ChildClass child = new ChildClass();

        @JSONField
        float f = 3.141f;

        @JSONField(childType = ChildClass.class)
        List<ChildClass> childList = new ArrayList<>();

        @JSONField
        ChildClass[] childArray = new ChildClass[]{new ChildClass(), new ChildClass()};

        @JSONField
        short shorty = 13;

        @JSONField
        byte bite = 127;

        @JSONField
        String[][] listyList = new String[][]{ new String[]{"1", "2"}, new String[]{"3", "4"} };

        public SimpleObject(){
            stringArray.add("one");
            stringArray.add("two");
            childList.add(new ChildClass());
        }
    }

    public static class ChildClass extends SimpleJSONifyable {

        @JSONField
        String meow = "meow";

        @JSONField
        Float[] floats = new Float[]{1f,2.5f,.99f};
    }

    public static class CustomSerializer implements Serializer<String> {

        @Override
        public Object serialize(String item) throws JSONException {
            JSONArray array = new JSONArray();
            for(String s : item.split("")) if(!s.isEmpty()) array.put(s);
            return array;
        }

        @Override
        public String deserialize(Object object) throws JSONException {
            JSONArray array = (JSONArray) object;
            StringBuilder builder = new StringBuilder();
            for(int i=0; i<array.length(); i++){
                builder.append(array.get(i));
            }
            return builder.toString();
        }
    }
}
