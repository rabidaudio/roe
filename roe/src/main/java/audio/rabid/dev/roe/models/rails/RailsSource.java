package audio.rabid.dev.roe.models.rails;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import audio.rabid.dev.roe.models.NetworkResource;
import audio.rabid.dev.roe.models.NetworkSource;
import audio.rabid.dev.roe.models.PermissionsManager;
import audio.rabid.dev.roe.models.RoeDatabase;
import audio.rabid.dev.roe.models.SimplePermissionsManager;
import audio.rabid.dev.roe.models.cache.WeakMapNetworkResourceCache;

/**
 * Created by charles on 10/29/15.
 */
public class RailsSource<T extends NetworkResource<LK, Integer>, LK> extends NetworkSource<T, LK, Integer> {

    public RailsSource(@NonNull RailsServer server,
                       @NonNull RoeDatabase database,
                       @NonNull Class<T> rClass,
                          @Nullable String endpoint,
                          @Nullable PermissionsManager<T> permissions) {


        super(server, database, rClass,
                new WeakMapNetworkResourceCache<T, LK, Integer>(50),
                permissions == null ? new SimplePermissionsManager<T>().all() : permissions,
                new NetworkDateFormat());

        if (endpoint != null) {
            server.addEndpoint(rClass, endpoint);
        }
    }

    public RailsSource(@NonNull RailsServer server, @NonNull RoeDatabase database, @NonNull Class<T> rClass) {
        this(server, database, rClass, null, null);
    }
}
