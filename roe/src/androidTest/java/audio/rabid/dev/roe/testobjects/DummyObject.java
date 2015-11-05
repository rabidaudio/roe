package audio.rabid.dev.roe.testobjects;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import audio.rabid.dev.roe.models.JSONField;
import audio.rabid.dev.roe.models.NetworkResource;
import audio.rabid.dev.roe.models.NetworkSource;
import audio.rabid.dev.roe.models.Resource;
import audio.rabid.dev.roe.models.ResourceFactory;
import audio.rabid.dev.roe.models.SimplePermissionsManager;
import audio.rabid.dev.roe.models.Source;

/**
 * Created by charles on 10/30/15.
 */
@DatabaseTable(tableName = "dummies")
public class DummyObject extends NetworkResource<DummyObject> {

    public DummyObject() {
    }

    public DummyObject(String name, int age, DummyChild child) {
        this.name = name;
        this.age = age;
        this.child = child;
    }

    @JSONField
    @DatabaseField
    protected String name;

    @JSONField
    @DatabaseField
    protected int age = 0;

    @JSONField
    @DatabaseField(foreign = true)
    public DummyChild child;

    public String getName() {
        return name;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age){
        this.age = age;
    }

    public DummyChild getChild() {
        return child;
    }

    @Override
    public Source<DummyObject> getSource() {
        return DummyObjectSource.getInstance();
    }
}
