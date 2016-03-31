package audio.rabid.dev.roe.models.cache;

import android.support.annotation.Nullable;

/**
 * Created by charles on 10/29/15.
 * <p/>
 * An object cache for keeping track of Resources (either by localId or serverId). You should make
 * your cache thread-safe by making all your methods synchronized for atomicity.
 */
public interface ResourceCache {

    <T> T put(String key, T object);

    <T> T delete(String key, T object);

    boolean has(String key);

    void clear();

    int size();

    <T> T get(String id);
}