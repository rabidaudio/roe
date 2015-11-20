package audio.rabid.dev.roe.models;

import com.j256.ormlite.support.ConnectionSource;

import java.sql.SQLException;
import java.util.concurrent.Future;

/**
 * Created by charles on 11/11/15.
 */
public class GenericResourceDao extends NetworkSyncableDao<GenericResource, Integer, Integer> {

    Future<Boolean> lastCheckUpdateFuture;
    Future<Boolean> lastCreateFuture;
    Future<Boolean> lastUpdateFuture;
    Future<Boolean> lastDeleteFuture;

    public void clearFutures(){
        lastCheckUpdateFuture = lastCreateFuture = lastUpdateFuture = lastDeleteFuture = null;
    }

    public GenericResourceDao(ConnectionSource connectionSource, Class<GenericResource> dataClass) throws SQLException {
        super(connectionSource, dataClass);
    }

    @Override
    protected Future<Boolean> checkUpdatesNetwork(GenericResource item) {
        lastCheckUpdateFuture = super.checkUpdatesNetwork(item);
        return lastCheckUpdateFuture;
    }

    @Override
    protected Future<Boolean> createNetwork(GenericResource item) {
        lastCreateFuture = super.createNetwork(item);
        return lastCheckUpdateFuture;
    }

    @Override
    protected Future<Boolean> updateNetwork(GenericResource item) {
        lastUpdateFuture = super.updateNetwork(item);
        return lastCheckUpdateFuture;
    }

    @Override
    protected Future<Boolean> deleteNetwork(GenericResource item) {
        lastDeleteFuture = super.deleteNetwork(item);
        return lastDeleteFuture;
    }
}
