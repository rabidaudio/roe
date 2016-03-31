package audio.rabid.dev.roe;

import android.util.Pair;

/**
 * Created by  charles  on 3/30/16.
 */
public interface AModel<T> {

    T showNetwork(String id) throws Exception;

//    T showDatabase(String id) throws Exception;

    Pair<String, T> createNetwork(T object) throws Exception;

    void createDatabase(String key, T object) throws Exception;

    T updateNetwork(String id, T object) throws Exception;

    void updateDatabase(String id, T object) throws Exception;

    void deleteNetwork(String id) throws Exception;

    void deleteDatabase(String id) throws Exception;

}
