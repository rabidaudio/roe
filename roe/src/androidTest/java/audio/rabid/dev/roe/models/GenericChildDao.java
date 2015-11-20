package audio.rabid.dev.roe.models;

import com.j256.ormlite.support.ConnectionSource;

import java.sql.SQLException;
import java.util.concurrent.Future;

/**
 * Created by charles on 11/11/15.
 */
public class GenericChildDao extends NetworkSyncableDao<GenericChild, String, String> {

    Future<Boolean> lastCheckUpdateFuture;
    Future<Boolean> lastCreateFuture;
    Future<Boolean> lastUpdateFuture;
    Future<Boolean> lastDeleteFuture;

    public GenericChildDao(ConnectionSource connectionSource, Class<GenericChild> dataClass) throws SQLException {
        super(connectionSource, dataClass);
    }

    public void clearFutures(){
        lastCheckUpdateFuture = lastCreateFuture = lastUpdateFuture = lastDeleteFuture = null;
    }

    @Override
    protected Future<Boolean> checkUpdatesNetwork(GenericChild item) {
        lastCheckUpdateFuture = super.checkUpdatesNetwork(item);
        return lastCheckUpdateFuture;
    }

    @Override
    protected Future<Boolean> createNetwork(GenericChild item) {
        lastCreateFuture = super.createNetwork(item);
        return lastCheckUpdateFuture;
    }

    @Override
    protected Future<Boolean> updateNetwork(GenericChild item) {
        lastUpdateFuture = super.updateNetwork(item);
        return lastCheckUpdateFuture;
    }

    @Override
    protected Future<Boolean> deleteNetwork(GenericChild item) {
        lastDeleteFuture = super.deleteNetwork(item);
        return lastDeleteFuture;
    }
}