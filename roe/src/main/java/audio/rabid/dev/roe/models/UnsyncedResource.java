package audio.rabid.dev.roe.models;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

import org.json.JSONException;
import org.json.JSONObject;

import java.sql.SQLException;

/**
 * Created by charles on 11/6/15.
 */
@DatabaseTable(tableName = "__unsynced_resources")
public class UnsyncedResource {

    @DatabaseField(generatedId = true)
    private int id;

    @DatabaseField
    private Server.Method method;

    @DatabaseField
    private String endpoint;

    @DatabaseField
    private String payload;

    @DatabaseField(index = true)
    private String cancelId;

    protected UnsyncedResource() {

    }

    protected UnsyncedResource(Server.NetworkException e, String cancelId) {
        method = e.method;
        endpoint = e.endpoint;
        if (e.payload != null) {
            payload = e.payload.toString();
        }
        this.cancelId = cancelId;
    }

    protected boolean attemptSync(Server server) {
        try {
            server.request(endpoint, method, payload == null ? null : new JSONObject(payload));
            return true;
        } catch (JSONException e) {
            //this shouldn't happen, since it came from JSON
            throw new RuntimeException(e);
        } catch (Server.NetworkException e) {
            //oh, well, still no network
        }
        return false;
    }

    protected static void cancelPendingSyncs(Dao<UnsyncedResource, Integer> myDao, String cancelId) throws SQLException {
        myDao.delete(myDao.queryForEq("cancelId", cancelId));
    }
}
