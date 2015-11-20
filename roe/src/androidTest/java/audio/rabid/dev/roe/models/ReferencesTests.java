package audio.rabid.dev.roe.models;

import android.test.AndroidTestCase;
import android.test.suitebuilder.annotation.Suppress;

import com.j256.ormlite.dao.Dao;

import java.sql.SQLException;

/**
 * Created by charles on 11/11/15.
 */
public class ReferencesTests extends AndroidTestCase {

    public void setUp() throws Exception {
        GenericDatabase.createInstance(getContext());
        GenericDatabase.getInstance().clearTables();
        GenericDatabase.getInstance().getDao(GenericResource.class).getObjectCache().clearAll();
        MockServer.getInstance().setNetworkEnabled(true);
        MockServer.getInstance().clearCounts();
        ((GenericResourceDao)GenericDatabase.getInstance().getDao(GenericResource.class)).clearFutures();
    }


    public void testChildOperationsNoAutoForeignCreate() throws Exception {

        GenericResource r = createNewResource();
        getDao().create(r);
        assertTrue(getDao().lastCreateFuture.get());
        assertEquals(1, MockServer.getInstance().createCount);
        assertNull(r.getChild());
        int id = getDao().extractId(r);
        assertEquals(1, getDao().countOf());
        assertEquals(0, getChildDao().countOf());

        GenericChild c = new GenericChild("child");
        getChildDao().create(c);
        assertTrue(getChildDao().lastCreateFuture.get());
        assertEquals(2, MockServer.getInstance().createCount);

        r.setChild(c);
        getDao().update(r);

        assertEquals(1, getDao().countOf());
        assertEquals(1, getChildDao().countOf());

        clearCache();

        r = getDao().queryForId(id);
        assertNotNull(r);
        assertNotNull(r.getChild());

        getChildDao().refresh(r.getChild());
        assertNotNull(r.getChild());
        assertEquals("child", r.getChild().getName());
    }

    @Suppress //only run this test with @DatabaseField(foreignAutoCreate = true) on GenericChild field of GenericResource TODO doesn't work yet
    public void testAutoCreate() throws Exception {

        GenericResource r = createNewResource();
        r.setChild(new GenericChild("childAuto"));
        getDao().create(r);
        assertTrue(getDao().lastCreateFuture.get());
        assertTrue(getChildDao().lastCreateFuture.get());
        assertEquals(2, MockServer.getInstance().createCount);

        assertEquals(1, getDao().countOf());
        assertEquals(1, getChildDao().countOf());
    }

    /*************************************************************************/

    public GenericResource createNewResource() throws Exception {
        GenericResource r = new GenericResource();
        r.setField1("meow");
        r.setField2(16);
//        getDao().create(r);
        return r;
    }

    public void clearCache() throws Exception {
        getDao().clearObjectCache();
        getChildDao().clearObjectCache();
        getDao().clearFutures();
        getChildDao().clearFutures();
        MockServer.getInstance().clearCounts();
    }

    public GenericResourceDao getDao() throws SQLException {
        return GenericDatabase.getInstance().getDao(GenericResource.class);
    }

    public GenericChildDao getChildDao() throws SQLException {
        return GenericDatabase.getInstance().getDao(GenericChild.class);
    }

    public void enableNetwork(){
        MockServer.getInstance().setNetworkEnabled(true);
    }

    public void disableNetwork(){
        MockServer.getInstance().setNetworkEnabled(false);
    }
}
