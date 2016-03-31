package audio.rabid.dev.roe.models;

import android.util.Pair;

import java.util.List;
import java.util.Map;

/**
 * Created by  charles  on 3/30/16.
 */
public interface Model<T, ID> {

    List<T> query(Map<String, String> query) throws Exception;

    T find(ID id) throws Exception;

    T create(T object) throws Exception;

    T update(ID id, T object) throws Exception;;

    void delete(T object) throws Exception;

    T findInPersistentStorage(ID id);

    List<T> queryPersistentStorage(Map<String, String> query);

    void saveToPersistentStorage(ID id, T object);

    ID getID(T object);
}
