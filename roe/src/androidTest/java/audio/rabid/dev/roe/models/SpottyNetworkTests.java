package audio.rabid.dev.roe.models;

import android.test.AndroidTestCase;

import com.j256.ormlite.dao.Dao;

import org.json.JSONObject;

import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Created by charles on 11/11/15.
 */
public class SpottyNetworkTests extends AndroidTestCase {

    public void setUp() throws Exception {
        GenericDatabase.createInstance(getContext());
        GenericDatabase.getInstance().clearTables();
        GenericDatabase.getInstance().getDao(GenericResource.class).getObjectCache().clearAll();
        MockServer.getInstance().setNetworkEnabled(true);
        MockServer.getInstance().clearCounts();
        getDao().clearFutures();
    }

    public void testUpdateWithNetLossAndRecover() throws Exception {

        enableNetwork();

        GenericResource r = createNewResource();
        //wait until network ops complete
        assertTrue(getDao().lastCreateFuture.get(1, TimeUnit.SECONDS));
        assertNull("should not attempt a SHOW when cache added", getDao().lastCheckUpdateFuture);
        assertEquals("should not attempt a SHOW when cache added", 0, MockServer.getInstance().showCount);

        assertEquals(1, MockServer.getInstance().createCount);

        disableNetwork();

        //update a resource that exists both locally and remotely
        r.setField1("new");
        getDao().update(r);
        JSONObject original = r.asJSON();
        assertFalse(getDao().lastUpdateFuture.get(1, TimeUnit.SECONDS));

        assertEquals("server should not have updated", 0, MockServer.getInstance().updateCount);
        assertEquals("should still be one item in database", 1, getDao().countOf());
        assertEquals("should be one unsynced item", 1, getUnsyncedDao().countOf());

        UnsyncedResource u = getUnsyncedDao().queryForAll().get(0);

        assertEquals(GenericResource.class, u.getItemClass());
        assertEquals(NetworkSyncableDao.Op.UPDATE, u.getOperation());
        assertEquals(getDao().extractId(r), Integer.valueOf(u.getItemId()));
        assertEquals(u.getSnapshot().toString(), r.asJSON().toString());

        //update again, still without network
        r.setField1("new2");
        getDao().update(r);
        assertFalse(getDao().lastUpdateFuture.get(1, TimeUnit.SECONDS));

        assertEquals("server should not have updated", 0, MockServer.getInstance().updateCount);
        assertEquals("should still be one item in database", 1, getDao().countOf());
        assertEquals("should be 2 unsynced items", 2, getUnsyncedDao().countOf());

        List<UnsyncedResource> uu = getUnsyncedDao().queryForAll();
        u = uu.get(0);
        UnsyncedResource u2 = uu.get(1);

        assertEquals(GenericResource.class, u2.getItemClass());
        assertEquals(NetworkSyncableDao.Op.UPDATE, u2.getOperation());
        assertEquals(getDao().extractId(r), Integer.valueOf(u2.getItemId()));
        assertEquals(u2.getSnapshot().toString(), r.asJSON().toString());

        assertFalse(r.asJSON().toString().equals(u.getSnapshot().toString()));

        //database queries should have the new values
        GenericResource q = getDao().queryForId(getDao().extractId(r));
        assertEquals("new2", q.getField1());

        enableNetwork();

        r.setField1("new3");
        getDao().update(r);
        assertTrue(getDao().lastUpdateFuture.get(1, TimeUnit.SECONDS));

        assertEquals("server should have updated", 3, MockServer.getInstance().updateCount);
        assertEquals("should still be one item in database", 1, getDao().countOf());
        assertEquals("unsynced items should have cleared", 0, getUnsyncedDao().countOf());
    }

    public void testCreateWithoutNet() throws Exception {

        disableNetwork();

        GenericResource g = createNewResource();
        assertFalse(getDao().lastCreateFuture.get(1, TimeUnit.SECONDS));
        assertNull("should not attempt a SHOW when cache added", getDao().lastCheckUpdateFuture);
        assertEquals("should not attempt a SHOW when cache added", 0, MockServer.getInstance().showCount);

        assertEquals("server should not have created", 0, MockServer.getInstance().createCount);
        assertEquals("should be one item in database", 1, getDao().countOf());
        assertEquals("should be one unsynced item", 1, getUnsyncedDao().countOf());

        UnsyncedResource u = getUnsyncedDao().queryForAll().get(0);
        assertEquals(GenericResource.class, u.getItemClass());
        assertEquals(NetworkSyncableDao.Op.CREATE, u.getOperation());
        assertEquals(getDao().extractId(g), Integer.valueOf(u.getItemId()));
        assertEquals(u.getSnapshot().toString(), g.asJSON().toString());

        JSONObject current = g.asJSON();

        //update still without network
        g.setField1("new");
        getDao().update(g);
        assertFalse(getDao().lastUpdateFuture.get(1, TimeUnit.SECONDS));

        assertEquals("server should not have updated", 0, MockServer.getInstance().updateCount);
        assertEquals("should still be one item in database", 1, getDao().countOf());
        assertEquals("should only be 2 unsynced items", 2, getUnsyncedDao().countOf());
        u = getUnsyncedDao().queryForAll().get(1);
        assertEquals(GenericResource.class, u.getItemClass());
        assertEquals(NetworkSyncableDao.Op.UPDATE, u.getOperation());
        assertEquals(getDao().extractId(g), Integer.valueOf(u.getItemId()));
        assertEquals(u.getSnapshot().toString(), g.asJSON().toString());

        assertFalse(current.toString().equals(u.getSnapshot().toString()));

        enableNetwork();

        g.setField1("new3");
        getDao().update(g);
        assertTrue(getDao().lastUpdateFuture.get(1, TimeUnit.SECONDS));

        assertEquals("server should have created", 1, MockServer.getInstance().createCount);
        assertEquals("server should have updated", 2, MockServer.getInstance().updateCount);
        assertEquals("should still be one item in database", 1, getDao().countOf());
        assertEquals("unsynced items should have cleared", 0, getUnsyncedDao().countOf());
    }

    public void testFullCRUDWithoutNetwork() throws Exception {

        disableNetwork();

        GenericResource r = createNewResource();
        assertFalse(getDao().lastCreateFuture.get(1, TimeUnit.SECONDS));
        assertNull("should not attempt a SHOW when cache added", getDao().lastCheckUpdateFuture);
        assertEquals("should not attempt a SHOW when cache added", 0, MockServer.getInstance().showCount);

        assertEquals(1, getUnsyncedDao().countOf()); //there should be one = create

        r.setField1("booga");
        getDao().update(r);
        assertFalse(getDao().lastUpdateFuture.get(1, TimeUnit.SECONDS)); //fails

        assertEquals(2, getUnsyncedDao().countOf());

        getDao().delete(r);
        assertFalse(getDao().lastDeleteFuture.get(1, TimeUnit.SECONDS)); //fails

        assertEquals(3, getUnsyncedDao().countOf());
    }

    public void testUpdateMiss() throws Exception {

        enableNetwork();

        GenericResource r = createNewResource();
        //wait until network ops complete
        assertTrue(getDao().lastCreateFuture.get(1, TimeUnit.SECONDS));
        assertNull("should not attempt a SHOW when cache added", getDao().lastCheckUpdateFuture);
        assertEquals("should not attempt a SHOW when cache added", 0, MockServer.getInstance().showCount);

        assertEquals(1, MockServer.getInstance().createCount);

        disableNetwork();

        //update a resource that exists both locally and remotely
        r.setField1("new");
        getDao().update(r);
        assertFalse(getDao().lastUpdateFuture.get(1, TimeUnit.SECONDS));

        assertEquals("server should not have updated", 0, MockServer.getInstance().updateCount);
        assertEquals("should still be one item in database", 1, getDao().countOf());
        assertEquals("should be one unsynced item", 1, getUnsyncedDao().countOf());

        UnsyncedResource u = getUnsyncedDao().queryForAll().get(0);

        assertEquals(GenericResource.class, u.getItemClass());
        assertEquals(NetworkSyncableDao.Op.UPDATE, u.getOperation());
        assertEquals(getDao().extractId(r), Integer.valueOf(u.getItemId()));
        assertEquals(u.getSnapshot().toString(), r.asJSON().toString());

        enableNetwork();

        getDao().delete(r);
        assertTrue(getDao().lastDeleteFuture.get(1, TimeUnit.SECONDS));

        assertEquals("server should have gotten delete", 1, MockServer.getInstance().deleteCount);
        assertEquals("server should have gotten update", 1, MockServer.getInstance().updateCount);

        assertEquals("unsynced items should be clear", 0, getUnsyncedDao().countOf());
    }

    public void testDeleteWithoutNet() throws Exception {

        enableNetwork();

        GenericResource r = createNewResource();
        assertTrue(getDao().lastCreateFuture.get(1, TimeUnit.SECONDS));
        assertNull("should not attempt a SHOW when cache added", getDao().lastCheckUpdateFuture);
        assertEquals("should not attempt a SHOW when cache added", 0, MockServer.getInstance().showCount);

        assertEquals(1, MockServer.getInstance().createCount);
        assertEquals(0, getUnsyncedDao().countOf());

        disableNetwork();

        getDao().delete(r);
        assertFalse(getDao().lastDeleteFuture.get(1, TimeUnit.SECONDS));
        assertEquals(0, getDao().countOf());

        assertEquals(1, getUnsyncedDao().countOf());
        UnsyncedResource u = getUnsyncedDao().queryForAll().get(0);
        assertEquals(GenericResource.class, u.getItemClass());
        assertEquals(NetworkSyncableDao.Op.DELETE, u.getOperation());
        assertEquals(getDao().extractId(r), Integer.valueOf(u.getItemId()));
        assertEquals(u.getSnapshot().toString(), r.asJSON().toString());

        enableNetwork();

        assertTrue(GenericDatabase.getInstance().blockingSyncWithServer());

        assertEquals(0, getUnsyncedDao().countOf());
        assertEquals(1, MockServer.getInstance().deleteCount);
    }

    /************************************************************************************************/

    public GenericResourceDao getDao() throws SQLException {
        return GenericDatabase.getInstance().getDao(GenericResource.class);
    }

    public Dao<UnsyncedResource, Integer> getUnsyncedDao() throws SQLException {
        return GenericDatabase.getInstance().getDao(UnsyncedResource.class);
    }

    public void enableNetwork(){
        MockServer.getInstance().setNetworkEnabled(true);
    }

    public void disableNetwork(){
        MockServer.getInstance().setNetworkEnabled(false);
    }

    public GenericResource createNewResource() throws Exception {
        GenericResource r = new GenericResource();
        r.setField1("meow");
        r.setField2(16);
        getDao().create(r);
        return r;
    }

    public String cancelId(NetworkSyncableDao.Op op, int id){
        return String.format("%s:%s:%s", op.toString(), GenericResource.class.getCanonicalName(), String.valueOf(id));
    }

    public Server.NetworkException createNE(Server.Method method) throws Exception {
        return  new Server.NetworkException(new Exception("meow"), "bark", method, new JSONObject().put("awooooga", "foobar"));
    }
}
