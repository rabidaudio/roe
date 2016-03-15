package audio.rabid.dev.roe.models;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;

import audio.rabid.dev.roe.models.json.JSONField;
import audio.rabid.dev.roe.models.resource.SameStringKeyedResource;

/**
 * Created by charles on 11/11/15.
 */
@DatabaseTable(daoClass = GenericChildDao.class)
public class GenericChild extends SameStringKeyedResource {

    @DatabaseField
    private Date timestamp = new Date();

    public GenericChild(){

    }

    public GenericChild(String name){
        id = name;
    }

    public String getName(){
        return id;
    }

    @Override
    public JSONObject asJSON() throws JSONException {
        return new JSONObject().put("id", getServerId())
                .put("timestamp", timestamp.getTime());
    }

    @Override
    public void fromJSON(JSONObject object) throws JSONException {
        if(object.has("id")) id = object.getString("id");
        if(object.has("timestamp")) timestamp = new Date(object.getLong("timestamp"));
    }
}
