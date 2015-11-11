package audio.rabid.dev.roe.models;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.TableUtils;
import com.raizlabs.android.parser.ParserHolder;

import org.json.JSONObject;

import java.sql.SQLException;

/**
 * Created by charles on 11/10/15.
 */
public class NetworkRoeDao<T, ID> extends RoeDao<T, ID> {

    Server server;

    Dao<UnsyncedResource, Integer> unsyncedResourceDao;

    protected NetworkRoeDao(ConnectionSource connectionSource, Class<T> dataClass) throws SQLException {
        super(connectionSource, dataClass);
        TableUtils.createTableIfNotExists(connectionSource, UnsyncedResource.class);
    }

    public void initialize(Server server, RoeDatabase database) throws SQLException {
        this.server = server;
        unsyncedResourceDao = database.getDao(UnsyncedResource.class);
    }

    @Override
    public void onCacheItemAdded(Class<T> rClass, ID id, T item) {
        super.onCacheItemAdded(rClass, id, item);
        //at this point we know this item has been added from the local database to the cache, which
        // means a network update has not occurred yet, so do that

        if (server == null) {
            throw new RuntimeException("No JSON Serializer set. Make sure your OrmliteDatabaseHelper subclases RoeDatabase");
        }
        try {
            JSONObject data = server.getItem(rClass, id);
            synchronized (item) {
                ParserHolder.parse(item, data);
                getObservable(item).setChanged();
                refresh(item);
            }
        } catch (SQLException e) {
            onSQLException(e);
        } catch (Server.NetworkException e) {
            //well, the item is not necessarily up to date, but nothing we can do
        }
    }

    @Override
    protected void onCreated(T item) {
        try {
            String cancelId = generateCancelId(Op.CREATE, item);
            try {
                JSONObject data = server.createItem(getDataClass(), item);
                synchronized (item) {
                    ParserHolder.parse(item, data);
                    getObservable(item).setChanged();
                }
                UnsyncedResource.cancelPendingSyncs(unsyncedResourceDao, cancelId);
            } catch (Server.NetworkException e) {
                unsyncedResourceDao.create(new UnsyncedResource(e, cancelId));
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        super.onCreated(item);
    }

    @Override
    protected void onUpdated(T item) {
        try {
            String cancelId = generateCancelId(Op.UPDATE, item);
            try {
                JSONObject data = server.updateItem(getDataClass(), item);
                synchronized (item) {
                    ParserHolder.parse(item, data);
                    getObservable(item).setChanged();
                }
                UnsyncedResource.cancelPendingSyncs(unsyncedResourceDao, cancelId);
            } catch (Server.NetworkException e) {
                unsyncedResourceDao.create(new UnsyncedResource(e, cancelId));
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        super.onUpdated(item);
    }

    protected void onDeleted(T item) {
        super.onDeleted(item);
        try {
            String cancelId = generateCancelId(Op.DELETE, item);
            try {
                server.deleteItem(getDataClass(), item);
                UnsyncedResource.cancelPendingSyncs(unsyncedResourceDao, cancelId);
            } catch (Server.NetworkException e) {
                unsyncedResourceDao.create(new UnsyncedResource(e, cancelId));
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private String generateCancelId(Op operation, T item) throws SQLException {
        return String.format("%s:%s:%s", operation.toString(), getDataClass().getCanonicalName(), String.valueOf(extractId(item)));
    }

    public void onSQLException(SQLException e) {
        throw new RuntimeException(e);
    }
}