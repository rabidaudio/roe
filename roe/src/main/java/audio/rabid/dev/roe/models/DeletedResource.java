package audio.rabid.dev.roe.models;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

import java.sql.SQLException;
import java.util.List;

/**
 * Created by charles on 11/2/15.
 *
 * This is a table used by roe to keep track of items that were deleted locally but could not be
 * deleted on the remote server due to a network error.
 */
@DatabaseTable(tableName = "__deleted_resources")
public class DeletedResource {

    @DatabaseField(generatedId = true)
    private int id;

    @DatabaseField(canBeNull = false)
    private String serverId;

    @DatabaseField(canBeNull = false)
    private String className;


    protected DeletedResource(){

    }

    protected DeletedResource(NetworkResource deletedResource) {
        if(deletedResource.getServerId()==null){
            throw new IllegalArgumentException("Can't network delete a resource without a server id");
        }
        className = deletedResource.getClass().getCanonicalName();
        serverId = String.valueOf(deletedResource.getServerId());
    }

    protected static List<DeletedResource> getDeletedResources(Dao<DeletedResource, Integer> dao, Class clazz) throws SQLException {
        return dao.queryForEq("className", clazz.getCanonicalName());
    }

    protected boolean attemptDelete(Server server) {
        try {
            Class clazz = Class.forName(className);
            server.deleteItem(clazz, serverId);
            return true;
        } catch (ClassNotFoundException e) {
            //shouldn't happen since we converted it on create
            throw new RuntimeException(e);
        } catch (Server.NetworkException e) {
            return false;
        }
    }
}
