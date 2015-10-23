package audio.rabid.dev.sampleapp.backend;

import com.j256.ormlite.dao.RuntimeExceptionDao;

import java.util.List;

/**
 * Created by charles on 10/23/15.
 */
public class Dao<T> extends RuntimeExceptionDao<T, Integer> {

    public Dao(com.j256.ormlite.dao.Dao<T, Integer> dao) {
        super(dao);
    }

    public T findLocal(int id){
        return queryForId(id);
    }

    public T findRemote(int serverId){
        try {
            return queryForEq("serverId", serverId).get(0);
        }catch (IndexOutOfBoundsException e){
            return null;
        }
    }

    public List<T> all(){
        return queryForAll();
    }
}
