package audio.rabid.dev.roe.models;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

import org.json.JSONException;
import org.json.JSONObject;

import audio.rabid.dev.roe.models.resource.Resource;

/**
 * Created by charles on 11/11/15.
 */
@DatabaseTable(daoClass = GenericResourceDao.class)
public class GenericResource implements Resource<Integer, Integer> {

    @DatabaseField(generatedId = true)
    protected Integer localId;

    @DatabaseField(index = true)
    protected Integer serverId;

    @DatabaseField
    protected String field1;

    @DatabaseField
    protected int field2;

    @DatabaseField(foreign = true, foreignAutoRefresh = false, foreignAutoCreate = false)
    protected GenericChild child;


    @Override
    public Integer getServerId() {
        return serverId;
    }

    @Override
    public boolean hasServerId() {
        return serverId != null;
    }

    public String getField1() {
        return field1;
    }

    public void setField1(String field1) {
        this.field1 = field1;
    }

    public int getField2() {
        return field2;
    }

    public void setField2(int field2) {
        this.field2 = field2;
    }

    public GenericChild getChild(){
        return child;
    }

    public void setChild(GenericChild child) {
        this.child = child;
    }

    @Override
    public synchronized JSONObject asJSON() throws JSONException {
        return new JSONObject()
                .put("id", serverId)
                .put("field1", field1)
                .put("field2", field2);
    }

    @Override
    public synchronized void fromJSON(JSONObject object) throws JSONException {
        if (object.has("id")) serverId = object.getInt("id");
        if (object.has("field1")) field1 = object.getString("field1");
        if (object.has("field2")) field2 = object.getInt("field2");
    }
}
