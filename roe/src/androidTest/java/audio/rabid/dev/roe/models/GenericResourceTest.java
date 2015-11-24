package audio.rabid.dev.roe.models;

import android.os.Looper;
import android.test.AndroidTestCase;

import java.util.concurrent.TimeUnit;

import audio.rabid.dev.roe.testhelpers.ChangeDetectorObserver;
import audio.rabid.dev.roe.testhelpers.Synchronizer;

/**
 * Created by charles on 11/11/15.
 */
public class GenericResourceTest extends AndroidTestCase {

    public void setUp() throws Exception {
        GenericDatabase.createInstance(getContext());
        GenericDatabase.getInstance().clearTables();
        GenericDatabase.getInstance().getDao(GenericResource.class).getObjectCache().clearAll();
        MockServer.getInstance().setNetworkEnabled(true);
        MockServer.getInstance().clearCounts();
        ((GenericResourceDao)GenericDatabase.getInstance().getDao(GenericResource.class)).clearFutures();
        ((GenericChildDao)GenericDatabase.getInstance().getDao(GenericChild.class)).clearFutures();
    }

    public void testSingleCRUDOperations() throws Exception {

        GenericResourceDao dao = GenericDatabase.getInstance().getDao(GenericResource.class);

        GenericResource resource = new GenericResource();
        resource.setField1("hello");
        resource.setField2(15);

        assertTrue("initially should not have local id", dao.isNew(resource));
        assertFalse("initially should not have server id", resource.hasServerId());

        dao.create(resource);
        dao.lastCreateFuture.get(1, TimeUnit.SECONDS);
        assertNull("should not attempt a SHOW when cache added", dao.lastCheckUpdateFuture);
        assertEquals("should not attempt a SHOW when cache added", 0, MockServer.getInstance().showCount);

        assertEquals("should now be one item in database", 1, dao.countOf());
        assertFalse("should now have local id", dao.isNew(resource));

        assertTrue("should now have server id", resource.hasServerId());
        assertEquals("values should not have changed", "hello", resource.getField1());
        assertEquals("values should not have changed", 15, resource.getField2());
        assertEquals("server should have been hit", 1, MockServer.getInstance().createCount);

        int localId = dao.extractId(resource);
        int serverId = resource.getServerId();

        GenericResource readResult = dao.queryForSameId(resource);

        assertEquals("new query should be the same instance", resource, readResult);
        assertEquals("should still have item in database", 1, dao.countOf());
        assertFalse("should still have local id", dao.isNew(resource));
        assertTrue("should still have server id", resource.hasServerId());
        assertEquals("ids should not have changed", Integer.valueOf(localId), dao.extractId(resource));
        assertEquals("ids should not have changed", Integer.valueOf(serverId), resource.getServerId());
        assertEquals("values should not have changed", "hello", resource.getField1());
        assertEquals("values should not have changed", 15, resource.getField2());
        assertEquals("server should NOT have been hit", 0, MockServer.getInstance().showCount);

        resource.setField1("goodbye");
        resource.setField2(25);

        dao.createOrUpdate(resource);

        dao.lastUpdateFuture.get(1, TimeUnit.SECONDS); //block with timeout

        assertEquals("should still have item in database", 1, dao.countOf());
        assertFalse("should still have local id", dao.isNew(resource));
        assertTrue("should still have server id", resource.hasServerId());
        assertEquals("ids should not have changed", Integer.valueOf(localId), dao.extractId(resource));
        assertEquals("ids should not have changed", Integer.valueOf(serverId), resource.getServerId());
        assertEquals("values should have changed", "goodbye", resource.getField1());
        assertEquals("values should have changed", 25, resource.getField2());
        assertEquals("server should have been hit", 1, MockServer.getInstance().updateCount);

        readResult = dao.queryForSameId(resource);
        assertEquals("new query should be the same instance", resource, readResult);

        dao.delete(resource);

        assertEquals("should no longer be item in database", 0, dao.countOf());
        assertNull("query should return no results", dao.queryForId(dao.extractId(resource)));

        dao.lastDeleteFuture.get(1, TimeUnit.SECONDS);

        assertEquals("server should have been hit", 1, MockServer.getInstance().deleteCount);
    }

    public void testReadingExistingLocally() throws Exception {
        GenericResourceDao dao = GenericDatabase.getInstance().getDao(GenericResource.class);

        GenericResource r = new GenericResource();
        r.setField1("existing");
        dao.create(r);
        assertTrue(dao.lastCreateFuture.get(1, TimeUnit.SECONDS));
        assertNull("should not attempt a SHOW when cache added", dao.lastCheckUpdateFuture);
        assertEquals("should not attempt a SHOW when cache added", 0, MockServer.getInstance().showCount);

        int localId = dao.extractId(r);
        assertTrue("should have server id", r.hasServerId());

        dao.clearObjectCache();
        dao.clearFutures();
        MockServer.getInstance().clearCounts();

        assertEquals("should be one item saved in the database", 1, dao.countOf());
        assertEquals("cache should be empty", 0, dao.getObjectCache().sizeAll());

        GenericResource resource = dao.queryForId(localId);
        assertTrue("database resource should have serverId", resource.hasServerId());
        assertTrue(dao.lastCheckUpdateFuture.get(1, TimeUnit.SECONDS));

        assertNotNull("an item should have been returned", resource);
        assertEquals("should have same values", localId, (int) dao.extractId(resource));
        assertEquals("should have same values", "existing", resource.getField1());
        assertEquals("should be one item in cache", 1, dao.getObjectCache().sizeAll());
        assertEquals("server should have seen show", 1, MockServer.getInstance().showCount);

        GenericResource newRead = dao.queryForId(localId);
        assertEquals("should be the same resource", resource, newRead);
        assertEquals("server should NOT have seen show", 1, MockServer.getInstance().showCount);

        assertEquals("should still only be one item in database", 1, dao.countOf());
    }

    public void testObservablesWithNetwork() throws Exception {

        final GenericResourceDao dao = GenericDatabase.getInstance().getDao(GenericResource.class);

        final ChangeDetectorObserver<GenericResource> catcher1 = new ChangeDetectorObserver<>();
        final ChangeDetectorObserver<GenericResource> catcher2 = new ChangeDetectorObserver<>();

        final GenericResource r = new GenericResource();
        r.setField1("meow");
        r.setField2(99);

        dao.addObserver(catcher1, r);
        dao.addObserver(catcher2, r);

        dao.create(r); // causes 1 update to observers
        dao.lastCreateFuture.get(1, TimeUnit.SECONDS); //createNetwork did get called, which should update observers again
        assertNull(dao.lastCheckUpdateFuture); // checkUpdatesNetwork did not get called because it did not have an SK onCacheAdd
        assertEquals("should not attempt a SHOW when cache added", 0, MockServer.getInstance().showCount);

        new Synchronizer<Void>() {
            @Override
            public void run() {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        while(catcher1.peakChanges() < 2 && catcher2.peakChanges()<2){
                            /* block*/
                        }
                        setResult(null);
                    }
                }).start();
            }
        }.blockUntilFinished();

        assertEquals(2, catcher1.seenChanges());
        assertEquals(2, catcher2.seenChanges());
        assertFalse(catcher1.wasDeleted());
        assertFalse(catcher2.wasDeleted());
        assertEquals(Looper.getMainLooper().getThread(), catcher1.getCallingThread());

        final int id = dao.extractId(r);

        GenericResource r2 = new Synchronizer<GenericResource>() {
            @Override
            public void run() {
                dao.queryForIdAsync(id, new NetworkSyncableDao.OperationCallback<GenericResource>() {
                    @Override
                    public void onResult(GenericResource result) {
                        setResult(result);
                    }
                });
            }
        }.blockUntilFinished();

        r2.setField2(27);

        dao.update(r2); //calls observers once immediately, then launches updateNetwork
        dao.lastUpdateFuture.get(1, TimeUnit.SECONDS); //network is available, so this should update observers again

        new Synchronizer<Void>() {
            @Override
            public void run() {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        while(catcher1.peakChanges() < 2 && catcher2.peakChanges()<2){
                            /* block*/
                        }
                        setResult(null);
                    }
                }).start();
            }
        }.blockUntilFinished();

        assertEquals(2, catcher1.seenChanges());
        assertEquals(2, catcher2.seenChanges());
        assertFalse(catcher1.wasDeleted());
        assertFalse(catcher2.wasDeleted());
        assertEquals(Looper.getMainLooper().getThread(), catcher1.getCallingThread());

        dao.delete(r2); //observers will get notified when the item leaves the cache.
        dao.lastDeleteFuture.get(1, TimeUnit.SECONDS); //However, a successful net delete does not renotify

        new Synchronizer<Void>() {
            @Override
            public void run() {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        while(catcher1.peakChanges() < 1 && catcher2.peakChanges()<1){
                            /* block*/
                        }
                        setResult(null);
                    }
                }).start();
            }
        }.blockUntilFinished();

        assertEquals(1, catcher1.seenChanges());
        assertEquals(1, catcher2.seenChanges());
        assertTrue(catcher1.wasDeleted());
        assertTrue(catcher2.wasDeleted());
        assertEquals(Looper.getMainLooper().getThread(), catcher1.getCallingThread());
    }


    public void testObservablesWithoutNetwork() throws Exception {

        final GenericResourceDao dao = GenericDatabase.getInstance().getDao(GenericResource.class);

        MockServer.getInstance().setNetworkEnabled(false);

        final ChangeDetectorObserver<GenericResource> catcher1 = new ChangeDetectorObserver<>();
        final ChangeDetectorObserver<GenericResource> catcher2 = new ChangeDetectorObserver<>();

        final GenericResource r = new GenericResource();
        r.setField1("meow");
        r.setField2(99);

        dao.addObserver(catcher1, r);
        dao.addObserver(catcher2, r);

        dao.create(r); //notifies once
        dao.lastCreateFuture.get(1, TimeUnit.SECONDS);
        assertNull("should not attempt a SHOW when cache added", dao.lastCheckUpdateFuture);
        assertEquals("should not attempt a SHOW when cache added", 0, MockServer.getInstance().showCount);

        new Synchronizer<Void>() {
            @Override
            public void run() {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        while(catcher1.peakChanges() < 1 && catcher2.peakChanges()<1){
                            /* block*/
                        }
                        setResult(null);
                    }
                }).start();
            }
        }.blockUntilFinished();

        assertEquals(1, catcher1.seenChanges());
        assertEquals(1, catcher2.seenChanges());
        assertFalse(catcher1.wasDeleted());
        assertFalse(catcher2.wasDeleted());
        assertEquals(Looper.getMainLooper().getThread(), catcher1.getCallingThread());

        final int id = dao.extractId(r);
        GenericResource r2 = new Synchronizer<GenericResource>() {
            @Override
            public void run() {
                dao.queryForIdAsync(id, new NetworkSyncableDao.OperationCallback<GenericResource>() {
                    @Override
                    public void onResult(GenericResource result) {
                        setResult(result);
                    }
                });
            }
        }.blockUntilFinished();

        r2.setField2(27);

        dao.update(r2); //notifies once
        dao.lastUpdateFuture.get(1, TimeUnit.SECONDS);

        new Synchronizer<Void>() {
            @Override
            public void run() {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        while(catcher1.peakChanges() < 1 && catcher2.peakChanges()<1){
                            /* block*/
                        }
                        setResult(null);
                    }
                }).start();
            }
        }.blockUntilFinished();

        assertEquals(1, catcher1.seenChanges());
        assertEquals(1, catcher2.seenChanges());
        assertFalse(catcher1.wasDeleted());
        assertFalse(catcher2.wasDeleted());
        assertEquals(Looper.getMainLooper().getThread(), catcher1.getCallingThread());

        dao.delete(r2); //notifies once, regardless of network
        dao.lastDeleteFuture.get(1, TimeUnit.SECONDS);

        new Synchronizer<Void>() {
            @Override
            public void run() {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        while(catcher1.peakChanges() < 1 && catcher2.peakChanges()<1){
                            /* block*/
                        }
                        setResult(null);
                    }
                }).start();
            }
        }.blockUntilFinished();

        assertEquals(1, catcher1.seenChanges());
        assertEquals(1, catcher2.seenChanges());
        assertTrue(catcher1.wasDeleted());
        assertTrue(catcher2.wasDeleted());
        assertEquals(Looper.getMainLooper().getThread(), catcher1.getCallingThread());
    }


    public void testAsyncOperations() throws Exception {

        final GenericResourceDao dao = GenericDatabase.getInstance().getDao(GenericResource.class);

        final GenericResource r = new GenericResource();
        r.setField1("meow");
        r.setField2(99);

        GenericResource result = dao.saveAsync(r, null).get(1, TimeUnit.SECONDS);

        assertEquals(r, result);

        final int id = dao.extractId(r);
        result = dao.queryForIdAsync(id, null).get(1, TimeUnit.SECONDS);

        assertEquals(r, result);

        result = dao.saveAsync(r,null).get(1, TimeUnit.SECONDS);

        assertEquals(r, result);

        result = dao.deleteAsync(r,null).get(1, TimeUnit.SECONDS);

        assertEquals(r, result);
    }

    public void testSameLocalAndServerId() throws Exception {

        final GenericChildDao dao = GenericDatabase.getInstance().getDao(GenericChild.class);

        //with id
        GenericChild c = new GenericChild("cat");

        assertTrue(dao.isNew(c));
        assertTrue(c.hasServerId());

        dao.create(c);
        dao.lastCreateFuture.get(1, TimeUnit.SECONDS); //ensure create happens
        assertNull(dao.lastCheckUpdateFuture);

        //should not hit network
        GenericChild x = dao.getByServerIdAsync("cat", null).get();

        assertEquals(c, x);
    }
}
