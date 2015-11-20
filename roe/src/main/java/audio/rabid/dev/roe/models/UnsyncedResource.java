package audio.rabid.dev.roe.models;

import android.support.annotation.Nullable;

import com.j256.ormlite.dao.BaseDaoImpl;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

import org.json.JSONException;
import org.json.JSONObject;

import java.sql.SQLException;
import java.util.List;

import audio.rabid.dev.roe.models.resource.Resource;

/**
 * Created by charles on 11/11/15.
 */
@DatabaseTable(tableName = "__unsynced_resources")
public class UnsyncedResource {

    @DatabaseField(generatedId = true)
    private int id;

    @DatabaseField
    protected String snapshot;

    @DatabaseField
    protected long timestamp = System.nanoTime();

    @DatabaseField(index = true)
    protected String className;

    @DatabaseField
    protected NetworkSyncableDao.Op operation; //CREATE, UPDATE, DELETE

    @DatabaseField(index = true)
    protected String itemId;

    protected UnsyncedResource() {

    }

    private UnsyncedResource(JSONObject snapshot, String className, String id, NetworkSyncableDao.Op operation) {
        this.snapshot = snapshot.toString();
        this.className = className;
        this.itemId = id;
        this.operation = operation;
    }

    public static <T extends Resource<LK, SK>, LK, SK> void create(Database database,
                                                                   NetworkSyncableDao<T, LK, SK> dao, T item,
                                                                   NetworkSyncableDao.Op operation) {
        if(operation == NetworkSyncableDao.Op.READ){
            throw new IllegalArgumentException("Don't cache reads!");
        }
        try {
            Dao<UnsyncedResource, Integer> myDao = database.getDao(UnsyncedResource.class);
            LK id = dao.extractId(item);
            if (item == null || id == null) {
                throw new IllegalArgumentException("Can't create unsynced for item that doesn't exist locally");
            }
            myDao.create(new UnsyncedResource(item.asJSON(), dao.getDataClass().getCanonicalName(), String.valueOf(id), operation));
        }catch (SQLException | JSONException e){
            throw new RuntimeException("Unable to save item for syncing later", e);
        }
    }

    public Class getItemClass(){
        try {
            return Class.forName(className);
        }catch (ClassNotFoundException e){
            return null;
        }
    }

    public NetworkSyncableDao.Op getOperation(){
        return operation;
    }

    public String getItemId(){
        return itemId;
    }
    public JSONObject getSnapshot(){
        try {
            return new JSONObject(snapshot);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("unchecked")
    public <T extends Resource> T getItem(Database database) throws SQLException {
        BaseDaoImpl<T, ?> iDao = (BaseDaoImpl<T,?>) database.getDao(getItemClass());
        //allows us to query for an id by string even if the id isn't a string
        List<T> results = iDao.queryForEq(iDao.getTableInfo().getIdField().getColumnName(), itemId);
        if(results.isEmpty()){
            return null;
        }else{
            return results.get(0);
        }
    }

    @SuppressWarnings("unchecked")
    protected <T extends Resource<LK, SK>, LK, SK> boolean attemptSync(Database database) throws SQLException {
        try {
            JSONObject result;
            Class<T> tClass = getItemClass();
            T itemSnapshot = tClass.newInstance();
            itemSnapshot.fromJSON(getSnapshot());
            Server server = database.getServer(tClass);
            switch (operation) {
                case CREATE:
                    result = server.createItem(tClass, itemSnapshot);
                    break;
                case UPDATE:
                    result = server.updateItem(tClass, itemSnapshot, itemId);
                    break;
                case DELETE:
                    server.deleteItem(tClass, itemSnapshot, itemId);
                    return true;
                default:
                    return true;
            }
            if (result != null) {
                T item = getItem(database);
                if (item != null) { //could have since been deleted locally
                    NetworkSyncableDao<T, LK, SK> itemDao = database.getDao(tClass);
                    item.fromJSON(result);
                    itemDao.getObservable(item).setChanged();
                    itemDao.notifyObservers(item, false);
                }
            }
            return true;
        }catch (IllegalAccessException | InstantiationException | JSONException e){
            throw new RuntimeException(e);
        } catch (Server.NetworkException e){
            return false; //still no network
        }
    }

    protected static synchronized boolean attemptAllPendingRequests(Database database, @Nullable SyncProgressListener taskForProgressUpdates) throws SQLException {
        Dao<UnsyncedResource, Integer> myDao = database.getDao(UnsyncedResource.class);
        if(myDao.countOf() == 0){
            return true; //nothing to do!
        }
        List<UnsyncedResource> unsyncedResources = myDao.queryBuilder().orderBy("timestamp", true).query();
        if(taskForProgressUpdates != null){
            taskForProgressUpdates.onProgress(0, unsyncedResources.size());
        }
        int position = 0;
        for(UnsyncedResource unsyncedResource : unsyncedResources){
            boolean success = unsyncedResource.attemptSync(database);
            if(success) {
                myDao.delete(unsyncedResource); // complete!
                if(taskForProgressUpdates != null){
                    taskForProgressUpdates.onProgress(++position, unsyncedResources.size());
                }
            }else{
                return false; //sync failed. Don't want to continue in case some calls depend on others
            }
        }
        return true; //all complete!
    }

    public interface SyncProgressListener {
        void onProgress(int complete, int total);
    }
}