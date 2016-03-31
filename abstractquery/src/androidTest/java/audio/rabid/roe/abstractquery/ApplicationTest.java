package audio.rabid.roe.abstractquery;

import android.app.Application;
import android.os.Bundle;
import android.test.ApplicationTestCase;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * <a href="http://d.android.com/tools/testing/testing_android.html">Testing Fundamentals</a>
 */
public class ApplicationTest extends ApplicationTestCase<Application> {
    public ApplicationTest() {
        super(Application.class);
    }

    public void testEmpty() throws Exception {
        QueryBuilder q = new QueryBuilder();

        assertEquals("", q.buildHTTPQueryString());
        assertEquals("", q.buildSQLClause());
//        assertEquals(0, q.buildTree().size());

        q = new QueryBuilder().where(new WhereBuilder());

        assertEquals("", q.buildHTTPQueryString());
        assertEquals("", q.buildSQLClause());
//        assertEquals(0, q.buildTree().size());
    }

    public void testSimpleWhereString() throws Exception {
        QueryBuilder q = new QueryBuilder().where(new WhereBuilder().eq("column", "value"));

        assertEquals("?column=value", q.buildHTTPQueryString());
        assertEquals("WHERE `column` = 'value'", q.buildSQLClause());
//        Bundle tree = q.buildTree();
//        Bundle whereTree = tree.getBundle("where");
//
//        Bundle expectedWhereTree = new BundleBuilder()
//                .putString("column", "column")
//                .putString("relation", "eq")
//                .putString("value", "value")
//                .build();
//        assertEquals(expectedWhereTree, whereTree);
    }

    public void testSimpleWhereWithOrder() throws Exception {
        QueryBuilder q = new QueryBuilder().where(new WhereBuilder().contains("name", "john")).orderAsc("created_at");

        assertEquals("?name[contains]=john&order[created_at]=asc", q.buildHTTPQueryString());
        assertEquals("WHERE `name` LIKE '%john%' ORDER BY `created_at` ASC", q.buildSQLClause());
//        Bundle tree = q.buildTree();
//        Bundle whereTree = tree.getBundle("where");
//
//        Bundle expectedWhereTree = new BundleBuilder()
//                .putString("column", "name")
//                .putString("relation", "contains")
//                .putString("value", "john")
//                .build();
//        assertEquals(expectedWhereTree, whereTree);

    }

    public void testSimpleWhereWithLimit() throws Exception {
        QueryBuilder q = new QueryBuilder().where(new WhereBuilder().contains("name", "john")).limit(5);

        assertEquals("?name[contains]=john&limit=5", q.buildHTTPQueryString());
        assertEquals("WHERE `name` LIKE '%john%' LIMIT 5", q.buildSQLClause());
    }

    public void testSimpleWhereWithOrderAndLimit() throws Exception {
        QueryBuilder q = new QueryBuilder().where(new WhereBuilder().contains("name", "john")).orderAsc("created_at").limit(5);
        assertEquals("?name[contains]=john&order[created_at]=asc&limit=5", q.buildHTTPQueryString());
        assertEquals("WHERE `name` LIKE '%john%' ORDER BY `created_at` ASC LIMIT 5", q.buildSQLClause());
    }

    public void testWhereWithAnd() throws Exception {
        QueryBuilder q = new QueryBuilder()
                .where(new WhereBuilder().contains("name", "john")
                        .and(new WhereBuilder().contains("email", "example.com"))
                        .and(new WhereBuilder().lt("age", 30))
                        .and(new WhereBuilder().gte("age", 20)));
        assertEquals("?name[contains]=john&email[contains]=example.com&age<30&age>=20", q.buildHTTPQueryString());
        assertEquals("WHERE `name` LIKE '%john%' AND `email` LIKE '%example.com%' AND `age` < 30 AND `age` >= 20", q.buildSQLClause());
    }

    public void testConvertBundlesToJSONObjects() throws Exception {

        Bundle b = new Bundle();

        b.putInt("int", 27);
        b.putString("string", "string");
        b.putBoolean("bool", false);
        b.putDouble("double", 1234567.7654321);
        b.putFloat("float", (float) -99.9);
        b.putByte("byte", (byte) 0xEF);
        b.putChar("char", 'r');
        b.putLong("long", (long) 1000*60*60*24*365);

        Bundle child = new Bundle();
        child.putString("key", "val");
        child.putBoolean("child", true);

        b.putBundle("child", child);

        b.putStringArray("stringarray", new String[]{"one", "two"});
        ArrayList<String> stringArrayList = new ArrayList<>();
        stringArrayList.add("three");
        stringArrayList.add("four");
        b.putStringArrayList("stringarraylist", stringArrayList);

        b.putStringArray("emptyarray", new String[0]);

        b.putIntArray("intarray", new int[]{-1, 0, 1});
        b.putDoubleArray("doublearray", new double[]{-1.1, 0.0, 1.1});

        // 1D0C0DEBAD
        b.putByteArray("bytearray", new byte[]{ 0x1D, 0x0C, 0x0D, (byte)0xEB, (byte)0xAD });


        JSONObject o = Util.bundleToJSON(b);

        assertEquals(27, o.getInt("int"));
        assertEquals("string", o.getString("string"));
        assertEquals(false, o.getBoolean("bool"));
        assertEquals(1234567.7654321, o.getDouble("double"), 1e-7);
        assertEquals(-99.9, o.getDouble("float"), 1e-2);
        assertEquals((byte) 0xEF, o.getInt("byte"));
        assertEquals("r", o.getString("char"));
        assertEquals((long) 1000*60*60*24*365, o.getLong("long"));
        JSONObject oChild = o.getJSONObject("child");
        assertEquals("val", oChild.getString("key"));
        assertEquals(true, oChild.getBoolean("child"));

        JSONArray oStringArray = o.getJSONArray("stringarray");
        assertEquals(2, oStringArray.length());
        assertEquals("one", oStringArray.getString(0));
        assertEquals("two", oStringArray.getString(1));
        JSONArray oStringArrayList = o.getJSONArray("stringarraylist");
        assertEquals(2, oStringArrayList.length());
        assertEquals("three", oStringArrayList.getString(0));
        assertEquals("four", oStringArrayList.getString(1));

        assertEquals(0, o.getJSONArray("emptyarray").length());

        assertEquals(-1, o.getJSONArray("intarray").getInt(0));
        assertEquals(0, o.getJSONArray("intarray").getInt(1));
        assertEquals(1, o.getJSONArray("intarray").getInt(2));

        assertEquals(-1.1, o.getJSONArray("doublearray").getDouble(0));
        assertEquals(0.0, o.getJSONArray("doublearray").getDouble(1));
        assertEquals(1.1, o.getJSONArray("doublearray").getDouble(2));

        assertEquals(5, o.getJSONArray("bytearray").length());
        assertEquals(0x1D, o.getJSONArray("bytearray").getInt(0));
    }
}