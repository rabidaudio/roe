package audio.rabid.dev.roe.models;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

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
    private int serverId;

    @DatabaseField(canBeNull = false)
    private String className;


    protected DeletedResource(){

    }

    protected DeletedResource(Resource deletedResource) {
        if(deletedResource.getServerId()==null){
            throw new IllegalArgumentException("Can't network delete a resource without a server id");
        }
        className = deletedResource.getClass().getCanonicalName();
        serverId = deletedResource.getServerId();
    }

    protected Server.Response attemptDelete(Server server) throws Server.NetworkException{
        try {
            Class clazz = Class.forName(className);
            return server.deleteItem(clazz, serverId);
        }catch (ClassNotFoundException e){
            //shouldn't happen since we converted it on create
            return null;
        }
    }
}
