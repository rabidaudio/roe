package audio.rabid.dev.roe.models;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by charles on 11/6/15.
 */
@DatabaseTable(tableName = "__unsynced_resources")
public class UnsyncedResource {

    @DatabaseField(generatedId = true)
    private int id;

    @DatabaseField(canBeNull = false)
    private String localId;

    @DatabaseField(canBeNull = false)
    private String className;

    @DatabaseField
    private boolean needsCreated = false;

    protected UnsyncedResource() {

    }

    private UnsyncedResource(NetworkResource uncreatedResource, String localId, boolean needsCreated) {
        if (uncreatedResource.getId() == null) {
            throw new IllegalArgumentException("Can't network create a resource that hasn't been saved locally");
        }
        className = uncreatedResource.getClass().getCanonicalName();
        this.localId = localId;
        this.needsCreated = needsCreated;
    }

    public String getLocalId() {
        return localId;
    }

    public boolean needsCreate() {
        return needsCreated;
    }

    protected String getClassName() {
        return className;
    }

    protected static List<UnsyncedResource> getUnsynced(Class uClass, Dao<UnsyncedResource, ?> dao) throws SQLException {
        return dao.queryForEq("className", uClass.getCanonicalName());
    }

    protected static void createIfNeeded(Dao<UnsyncedResource, ?> dao, NetworkResource resource, String localId, boolean needsCreated) throws SQLException {
        Map<String, Object> query = new HashMap<>(2);
        query.put("className", resource.getClass().getCanonicalName());
        query.put("localId", localId);
        List<UnsyncedResource> results = dao.queryForFieldValuesArgs(query);
        if (results.isEmpty()) {
            //need to add one
            dao.create(new UnsyncedResource(resource, localId, needsCreated));
        } else {
            UnsyncedResource r = results.get(0);
            r.needsCreated = r.needsCreated || needsCreated;
            dao.update(r);
        }
    }
}
