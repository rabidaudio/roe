package audio.rabid.dev.roe.models.rails;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.j256.ormlite.android.apptools.OrmLiteSqliteOpenHelper;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.support.ConnectionSource;

import java.sql.SQLException;

import audio.rabid.dev.roe.models.NetworkResource;
import audio.rabid.dev.roe.models.NetworkSource;
import audio.rabid.dev.roe.models.Op;
import audio.rabid.dev.roe.models.PermissionsManager;
import audio.rabid.dev.roe.models.SimplePermissionsManager;
import audio.rabid.dev.roe.models.cache.MapNetworkResourceCache;

/**
 * Created by charles on 10/29/15.
 */
public class RailsSource<T extends NetworkResource<LK, Integer>, LK> extends NetworkSource<T, LK, Integer> {

    protected RailsSource(@NonNull RailsServer server,
                          @NonNull Dao<T, LK> dao,
                          @Nullable String endpoint,
                          @Nullable PermissionsManager<T> permissions) {


        super(server, dao,
                new MapNetworkResourceCache<T, LK, Integer>(50),
                permissions == null ? new SimplePermissionsManager<T>().all() : permissions,
                new NetworkDateFormat());

        if (endpoint != null) {
            server.addEndpoint(dao.getDataClass(), endpoint);
        }
    }

    public static class Builder<T extends NetworkResource<LK, Integer>, LK> {

        RailsServer server;
        Dao<T, LK> dao;

        String endpoint;
        PermissionsManager<T> permissionsManager = new SimplePermissionsManager<T>().all();

        public Builder() {

        }

        public Builder(RailsServer server, OrmLiteSqliteOpenHelper database, Class<T> tClass) {
            setServer(server);
            setDatabase(database, tClass);
        }

        public Builder(RailsServer server, Dao<T, LK> dao) {
            setServer(server);
            setDao(dao);
        }

        public Builder<T, LK> setServer(@NonNull RailsServer server) {
            this.server = server;
            return this;
        }

        public Builder<T, LK> setServerURL(String rootURL) {
            this.server = new RailsServer(rootURL);
            return this;
        }

        public Builder<T, LK> setDatabase(@NonNull OrmLiteSqliteOpenHelper database, Class<T> tClass) {
            try {
                this.dao = database.getDao(tClass);
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
            return this;
        }

        public Builder<T, LK> setDao(@NonNull Dao<T, LK> dao) {
            this.dao = dao;
            return this;
        }

        public Builder<T, LK> setEndpoint(String endpoint) {
            this.endpoint = endpoint;
            return this;
        }

        public Builder<T, LK> setPermissionsManager(PermissionsManager<T> permissionsManager) {
            this.permissionsManager = permissionsManager;
            return this;
        }

        public Builder<T, LK> setPermissions(Op... allowedOps) {
            this.permissionsManager = new SimplePermissionsManager<>(allowedOps);
            return this;
        }

        public RailsSource<T, LK> build() {
            if (server == null)
                throw new IllegalArgumentException("Must supply a Server instance");
            if (dao == null)
                throw new IllegalArgumentException("Must supply either a Dao or a Database instance");

            return new RailsSource<>(server, dao, endpoint, permissionsManager);
        }
    }
}
