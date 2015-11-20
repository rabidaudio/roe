package audio.rabid.dev.roe.models.idlookups;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

import org.json.JSONException;
import org.json.JSONObject;

import audio.rabid.dev.roe.models.NetworkSyncableDao;
import audio.rabid.dev.roe.models.resource.Resource;

/**
 * Created by charles on 11/13/15.
 */
@DatabaseTable(daoClass = NetworkSyncableDao.class)
public class StringIdResource implements Resource<String, String> {

    @DatabaseField(id = true)
    private String localid;

    @DatabaseField(index = true)
    private String serverid;

    public void setId(String id){
        localid = id;
    }

    @Override
    public String getServerId() {
        return serverid;
    }

    @Override
    public boolean hasServerId() {
        return serverid != null;
    }

    @Override
    public JSONObject asJSON() throws JSONException {
        return null;
    }

    @Override
    public void fromJSON(JSONObject object) throws JSONException {

    }
}
