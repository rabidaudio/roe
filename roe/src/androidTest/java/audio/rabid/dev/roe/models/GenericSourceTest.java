package audio.rabid.dev.roe.models;

import android.os.Looper;
import android.support.annotation.Nullable;
import android.test.AndroidTestCase;
import android.test.suitebuilder.annotation.Suppress;
import android.util.Log;

import com.j256.ormlite.dao.Dao;

import java.util.List;

import audio.rabid.dev.roe.BackgroundThread;
import audio.rabid.dev.roe.ChangeDetectorObserver;
import audio.rabid.dev.roe.Synchronizer;
import audio.rabid.dev.roe.models.cache.WeakMapNetworkResourceCache;
import audio.rabid.dev.roe.testobjects.DummyObject;
import audio.rabid.dev.roe.testobjects.DummyObjectMockServer;
import audio.rabid.dev.roe.testobjects.DummyObjectSource;
import audio.rabid.dev.roe.testobjects.GenericDatabase;
import audio.rabid.dev.roe.testobjects.NoNetworkResource;

/**
 * Created by charles on 10/30/15.
 */
public class GenericSourceTest extends AndroidTestCase {

    public void setUp(){
        GenericDatabase.getInstance(getContext()); //initialize database
        DummyObjectSource.getInstance();
        GenericDatabase.getInstance().clearDatabase();
        DummyObjectMockServer.getInstance().setNetworkAvailable(true);
        DummyObjectSource.getInstance().clearCreateCompleted();
        DummyObjectSource.getInstance().clearUpdateCompleted();
        DummyObjectSource.getInstance().clearDeleteCompleted();
        DummyObjectSource.getInstance().clearCache();
    }

    /**
     * Test each CRUD operation on a single object while network connectivity exists
     */
    public void testBasicSingleOperationsWithNetwork() throws Exception {
        DummyObjectMockServer.getInstance().setNetworkAvailable(true);

        final DummyObject d = new DummyObject("meow", 0, null);

        assertNull("new object should not have a local id", d.getId());
        assertNull("new object should not have a server id", d.getServerId());
        assertFalse("new object should not be synced", d.hasServerId());
        assertTrue("new object should be new", d.isNew());

        //CREATE
        final int serverCreateCount = DummyObjectMockServer.getInstance().createCount;
        final DummyObject saveResult = (new Synchronizer<DummyObject>(){
            public void run(){
                d.save(new Source.OperationCallback<DummyObject>() {
                    @Override
                    public void onResult(final @Nullable DummyObject r) {
                        setResult(r);
                    }
                });
            }
        }).blockUntilFinished();

        assertNotNull("saved object should be returned in callback", saveResult);
        assertEquals("saved object should be the same instance", d, saveResult);
        assertNotNull("saved object should have a local id", saveResult.getId());
        assertNotNull("item should be in database", DummyObjectSource.getInstance().getDao().queryForId(d.getId()));

        new Synchronizer<Void>() {
            @Override
            public void run() {
                BackgroundThread.postBackground(new Runnable() {
                    @Override
                    public void run() {
                        while (!DummyObjectSource.getInstance().wasCreateCompleted()) { /*block*/ }
                        DummyObjectSource.getInstance().clearCreateCompleted();
                        setResult(null);
                    }
                });
            }
        }.blockUntilFinished();
        assertEquals("server should have seen create", serverCreateCount + 1, DummyObjectMockServer.getInstance().createCount);
        assertTrue("saved object should be synced", saveResult.hasServerId());
        assertNotNull("saved object should have a server id", saveResult.getServerId());

        //READ
        DummyObject readResult = new Synchronizer<DummyObject>() {
            @Override
            public void run() {
                DummyObjectSource.getInstance().find(d.getId(), new Source.OperationCallback<DummyObject>() {
                    @Override
                    public void onResult(@Nullable DummyObject result) {
                        setResult(result);
                    }
                });
            }
        }.blockUntilFinished();

        assertEquals("object by id should be the same instance", d, readResult);

        //UPDATE
        d.setAge(10);
        final int serverUpdatedCount = DummyObjectMockServer.getInstance().updatedCount;
        final DummyObject updateResult = new Synchronizer<DummyObject>() {
            @Override
            public void run() {
                d.save(new Source.OperationCallback<DummyObject>() {
                    @Override
                    public void onResult(@Nullable DummyObject result) {
                        setResult(result);
                    }
                });
            }
        }.blockUntilFinished();

        assertNotNull("updated object should be returned in callback", updateResult);
        assertEquals("updated object should be the same instance", d, updateResult);
        assertEquals("updated object should have the same local id", d.getId(), updateResult.getId());
        assertEquals("updated object should have the same server id", d.getServerId(), updateResult.getServerId());
        assertEquals("updated object should have the new values", 10, updateResult.getAge());

        new Synchronizer<Void>() {
            @Override
            public void run() {
                BackgroundThread.postBackground(new Runnable() {
                    @Override
                    public void run() {
                        while (!DummyObjectSource.getInstance().wasUpdateCompleted()) { /*block*/ }
                        DummyObjectSource.getInstance().clearUpdateCompleted();
                        setResult(null);
                    }
                });
            }
        }.blockUntilFinished();
        assertEquals("server should have seen update", serverUpdatedCount+1, DummyObjectMockServer.getInstance().updatedCount);

        //DELETE
        final int serverDeletedCount = DummyObjectMockServer.getInstance().deletedCount;
        DummyObject deleteResult = new Synchronizer<DummyObject>() {
            @Override
            public void run() {
                d.delete(new Source.OperationCallback<DummyObject>() {
                    @Override
                    public void onResult(@Nullable DummyObject result) {
                        setResult(result);
                    }
                });
            }
        }.blockUntilFinished();

        assertEquals("deleted item should be the same item", d, deleteResult);
        assertNull("item should no longer be in database", DummyObjectSource.getInstance().getDao().queryForId(d.getId()));

        new Synchronizer<Void>() {
            @Override
            public void run() {
                BackgroundThread.postBackground(new Runnable() {
                    @Override
                    public void run() {
                        while (!DummyObjectSource.getInstance().wasDeleteCompleted()){ /*block*/ }
                        DummyObjectSource.getInstance().clearDeleteCompleted();
                        setResult(null);
                    }
                });
            }
        }.blockUntilFinished();

        assertEquals("server should have seen delete", serverDeletedCount + 1, DummyObjectMockServer.getInstance().deletedCount);
    }

    /**
     * Test CRUD operations on a single object without network, and then enabling network and syncing
     */
    public void testBasicSingleOperationsWithoutNetwork() throws Exception {
        DummyObjectMockServer.getInstance().setNetworkAvailable(false);

        final DummyObject d = new DummyObject("meow", 0, null);

        assertNull("new object should not have a local id", d.getId());
        assertNull("new object should not have a server id", d.getServerId());
        assertFalse("new object should not be synced", d.hasServerId());
        assertTrue("new object should be new", d.isNew());

        WeakMapNetworkResourceCache cache = (WeakMapNetworkResourceCache)DummyObjectSource.getInstance().getResourceCache();

        Dao<UnsyncedResource, Integer> unsyncedResourceDao = GenericDatabase.getInstance().getDao(UnsyncedResource.class);
        Dao<DeletedResource, Integer> deletedResourceDao = GenericDatabase.getInstance().getDao(DeletedResource.class);

        assertEquals("object cache should be empty", 0, cache.size());
        assertEquals("unsynced resources should be empty", 0, unsyncedResourceDao.countOf());
        assertEquals("deleted resources should be empty", 0, deletedResourceDao.countOf());

        //CREATE
        DummyObject result = (new Synchronizer<DummyObject>(){
            public void run(){
                d.save(new Source.OperationCallback<DummyObject>() {
                    @Override
                    public void onResult(@Nullable DummyObject r) {
                        setResult(r);
                    }
                });
            }
        }).blockUntilFinished();

        new Synchronizer<Void>() {
            @Override
            public void run() {
                BackgroundThread.postBackground(new Runnable() {
                    @Override
                    public void run() {
                        while (!DummyObjectSource.getInstance().wasCreateCompleted()) { /*block*/ }
                        DummyObjectSource.getInstance().clearCreateCompleted();
                        setResult(null);
                    }
                });
            }
        }.blockUntilFinished();

        assertEquals("object cache should be 1", 1, cache.size());

        assertNotNull("saved object should be returned in callback", result);
        assertNotNull("saved object should have a local id", result.getId());
        assertNull("saved object should NOT have a server id", result.getServerId());
        assertFalse("saved object should NOT be synced", result.hasServerId());
        assertEquals("saved object should be the same instance", d, result);
        assertNotNull("item should be in database", DummyObjectSource.getInstance().getDao().queryForId(d.getId()));
        assertEquals("item should be in unsynced table", 1, unsyncedResourceDao.countOf());
        UnsyncedResource unsyncedResource = unsyncedResourceDao.queryForAll().get(0);
        assertEquals("the unsynced resource should have the same id", String.valueOf(d.getId()), unsyncedResource.getLocalId());
        assertEquals("the usynced class name should be correct", "audio.rabid.dev.roe.testobjects.DummyObject", unsyncedResource.getClassName());
        assertTrue("the unsynced resource should be marked that a create is required", unsyncedResource.needsCreate());

        //READ
        result = new Synchronizer<DummyObject>() {
            @Override
            public void run() {
                DummyObjectSource.getInstance().find(d.getId(), new Source.OperationCallback<DummyObject>() {
                    @Override
                    public void onResult(@Nullable DummyObject result) {
                        setResult(result);
                    }
                });
            }
        }.blockUntilFinished();

        assertEquals("object by id should be the same instance", d, result);
        assertEquals("object cache should be 1", 1, cache.size());

        //UPDATE
        d.setAge(10);

        result = new Synchronizer<DummyObject>() {
            @Override
            public void run() {
                d.save(new Source.OperationCallback<DummyObject>() {
                    @Override
                    public void onResult(@Nullable DummyObject result) {
                        setResult(result);
                    }
                });
            }
        }.blockUntilFinished();

        new Synchronizer<Void>() {
            @Override
            public void run() {
                BackgroundThread.postBackground(new Runnable() {
                    @Override
                    public void run() {
                        while (!DummyObjectSource.getInstance().wasUpdateCompleted()) { /*block*/ }
                        DummyObjectSource.getInstance().wasUpdateCompleted();
                        setResult(null);
                    }
                });
            }
        }.blockUntilFinished();

        assertNotNull("updated object should be returned in callback", result);
        assertNull("saved object should NOT have a server id", result.getServerId());
        assertFalse("saved object should NOT be synced", result.hasServerId());
        assertEquals("updated object should have the same local id", d.getId(), result.getId());
        assertEquals("updated object should have the new values", 10, result.getAge());
        assertEquals("updated object should be the same instance", d, result);
        assertEquals("object cache should be 1", 1, cache.size());
        assertEquals("item should be in unsynced table", 1, unsyncedResourceDao.countOf());
        unsyncedResource = unsyncedResourceDao.queryForAll().get(0);
        assertEquals("the unsynced resource should have the same id", d.localIdToString(), unsyncedResource.getLocalId());
        assertEquals("the usynced class name should be correct", "audio.rabid.dev.roe.testobjects.DummyObject", unsyncedResource.getClassName());
        assertTrue("the unsynced resource should STILL be marked that a create is required", unsyncedResource.needsCreate());

        //SYNC NO NET
        long prevUpdateTime = d.getUpdatedAt().getTime();
        List<DummyObject> synced = new Synchronizer<List<DummyObject>>() {
            @Override
            public void run() {
                DummyObjectSource.getInstance().sync(new Source.OperationCallback<List<DummyObject>>() {
                    @Override
                    public void onResult(@Nullable List<DummyObject> result) {
                        setResult(result);
                    }
                });
            }
        }.blockUntilFinished();

        assertEquals("synced list should be empty", 0, synced.size());
        assertFalse("the item should not be marked as synced", d.hasServerId());
        assertEquals("the new update time should be the same as the old one", prevUpdateTime, d.getUpdatedAt().getTime());
        assertEquals("object cache should be 1", 1, cache.size());
        assertEquals("item should still be in unsynced table", 1, unsyncedResourceDao.countOf());

        //SYNC NET
        DummyObjectMockServer.getInstance().setNetworkAvailable(true); //enable network

        prevUpdateTime = d.getUpdatedAt().getTime();
        synced = new Synchronizer<List<DummyObject>>() {
            @Override
            public void run() {
                DummyObjectSource.getInstance().sync(new Source.OperationCallback<List<DummyObject>>() {
                    @Override
                    public void onResult(@Nullable List<DummyObject> result) {
                        setResult(result);
                    }
                });
            }
        }.blockUntilFinished();

        assertTrue("the item should be marked as synced", d.hasServerId());
        assertNotNull("the item should have a server id", d.getServerId());
        assertEquals("synced list should have one item", 1, synced.size());
        assertEquals("synced list object should be the same", d, synced.get(0));
        assertEquals("object cache should be 1", 1, cache.size());
        assertEquals("there should be no more items in the unsynced table", 0, unsyncedResourceDao.queryForAll().size());
        assertTrue("the new update time should be larger than the old one", d.getUpdatedAt().getTime() > prevUpdateTime);
    }

    /**
     * Test that observables
     * @throws Exception
     */
    public void testObservablesOnBasicSingleOperationsWithNetwork() throws Exception {
        DummyObjectMockServer.getInstance().setNetworkAvailable(true);

        final DummyObject d = new DummyObject("meow", 0, null);

        ChangeDetectorObserver<DummyObject> catcher1 = new ChangeDetectorObserver<>();
        ChangeDetectorObserver<DummyObject> catcher2 = new ChangeDetectorObserver<>();

        DummyObjectSource.getInstance().addObserver(d, catcher1);
        DummyObjectSource.getInstance().addObserver(d, catcher2);

        //CREATE
        DummyObject result = (new Synchronizer<DummyObject>() {
            public void run() {
                d.save(new Source.OperationCallback<DummyObject>() {
                    @Override
                    public void onResult(@Nullable DummyObject r) {
                        setResult(r);
                    }
                });
            }
        }).blockUntilFinished();

        assertNotNull(result);
        assertTrue("observer should see a create", catcher1.sawChange());
        assertTrue("observer should see a create", catcher2.sawChange());
        assertFalse("item should not be reported deleted", catcher1.wasDeleted());
        assertFalse("item should not be reported deleted", catcher2.wasDeleted());
        assertEquals("observers should be called on main thread", Looper.getMainLooper().getThread(), catcher1.getCallingThread());

        //UPDATE
        d.setAge(10);
        result = new Synchronizer<DummyObject>() {
            @Override
            public void run() {
                d.save(new Source.OperationCallback<DummyObject>() {
                    @Override
                    public void onResult(@Nullable DummyObject result) {
                        setResult(result);
                    }
                });
            }
        }.blockUntilFinished();

        assertNotNull(result);
        assertTrue("observer should see update", catcher1.sawChange());
        assertTrue("observer should see update", catcher2.sawChange());
        assertFalse("item should not be reported deleted", catcher1.wasDeleted());
        assertFalse("item should not be reported deleted", catcher2.wasDeleted());
        assertEquals("observers should be called on main thread", Looper.getMainLooper().getThread(), catcher1.getCallingThread());

        //DELETE
        result = new Synchronizer<DummyObject>() {
            @Override
            public void run() {
                d.delete(new Source.OperationCallback<DummyObject>() {
                    @Override
                    public void onResult(@Nullable DummyObject result) {
                        setResult(result);
                    }
                });
            }
        }.blockUntilFinished();

        assertNotNull(result);
        assertTrue("observer should see delete", catcher1.sawChange());
        assertTrue("observer should see delete", catcher2.sawChange());
        assertTrue("item should be reported deleted", catcher1.wasDeleted());
        assertTrue("item should be reported deleted", catcher2.wasDeleted());
        assertEquals("observers should be called on main thread", Looper.getMainLooper().getThread(), catcher1.getCallingThread());
    }

    public void testNetworkDeleteWithoutNetwork() throws Exception {

        DummyObjectMockServer.getInstance().setNetworkAvailable(true);

        //create a new, valid test object
        final DummyObject object = new DummyObject("meow", 50, null);

        new Synchronizer<DummyObject>() {
            @Override
            public void run() {
                object.save(new Source.OperationCallback<DummyObject>() {
                    @Override
                    public void onResult(@Nullable DummyObject result) {
                        setResult(result);
                    }
                });
            }
        }.blockUntilFinished();
        new Synchronizer<Void>() {
            @Override
            public void run() {
                BackgroundThread.postBackground(new Runnable() {
                    @Override
                    public void run() {
                        while (!DummyObjectSource.getInstance().wasCreateCompleted()) { /*block*/ }
                        DummyObjectSource.getInstance().clearCreateCompleted();
                        setResult(null);
                    }
                });
            }
        }.blockUntilFinished();
        assertTrue(object.hasServerId());

        //turn off network
        DummyObjectMockServer.getInstance().setNetworkAvailable(false);

        //delete object
        DummyObject deleted = new Synchronizer<DummyObject>() {
            @Override
            public void run() {
                DummyObjectSource.getInstance().delete(object, new Source.OperationCallback<DummyObject>() {
                    @Override
                    public void onResult(@Nullable DummyObject result) {
                        setResult(result);
                    }
                });
            }
        }.blockUntilFinished();

        assertEquals("the object should have been returned by the callback", object, deleted);
        assertNull("the dummy object was actually deleted locally", DummyObjectSource.getInstance().getDao().queryForId(object.getId()));
        Dao<DeletedResource, Integer> deletedResourceDao = GenericDatabase.getInstance().getDao(DeletedResource.class);

        new Synchronizer<Void>() {
            @Override
            public void run() {
                BackgroundThread.postBackground(new Runnable() {
                    @Override
                    public void run() {
                        while (!DummyObjectSource.getInstance().wasDeleteCompleted()){ /*block*/ }
                        DummyObjectSource.getInstance().clearDeleteCompleted();
                        setResult(null);
                    }
                });
            }
        }.blockUntilFinished();

        assertEquals("a DeletedResource row was created", 1, deletedResourceDao.countOf());
        assertEquals("an UnsyncedResource row was not created", 0, GenericDatabase.getInstance().getDao(UnsyncedResource.class).countOf());

        //attempt sync, still without network
        int initialServerDeletedCount = DummyObjectMockServer.getInstance().deletedCount;
        List<DummyObject> unsynced = new Synchronizer<List<DummyObject>>() {
            @Override
            public void run() {
                DummyObjectSource.getInstance().sync(new Source.OperationCallback<List<DummyObject>>() {
                    @Override
                    public void onResult(@Nullable List<DummyObject> result) {
                        setResult(result);
                    }
                });
            }
        }.blockUntilFinished();

        assertEquals("there should be no items returned by unsynced", 0, unsynced.size());
        assertEquals("no items should have been deleted on the server", initialServerDeletedCount, DummyObjectMockServer.getInstance().deletedCount);
        assertEquals("there should still be one DeletedResource after a failed sync", 1, deletedResourceDao.countOf());

        DummyObjectMockServer.getInstance().setNetworkAvailable(true);

        //attempt sync with network
        unsynced = new Synchronizer<List<DummyObject>>() {
            @Override
            public void run() {
                DummyObjectSource.getInstance().sync(new Source.OperationCallback<List<DummyObject>>() {
                    @Override
                    public void onResult(@Nullable List<DummyObject> result) {
                        setResult(result);
                    }
                });
            }
        }.blockUntilFinished();

        assertEquals("there should be no items returned by unsynced (they've been deleted already, so why would we return something?)", 0, unsynced.size());
        assertEquals("there should no longer be a DeletedResource", 0, deletedResourceDao.countOf());
        assertEquals("1 item should have been deleted on the server", initialServerDeletedCount + 1, DummyObjectMockServer.getInstance().deletedCount);
    }


    public void testSourceWithoutServer() throws Exception {

        final NoNetworkResource resource = new NoNetworkResource();
        resource.myString = "y'all";

        new Synchronizer<NoNetworkResource>() {
            @Override
            public void run() {
                resource.save(new Source.OperationCallback<NoNetworkResource>() {
                    @Override
                    public void onResult(@Nullable NoNetworkResource result) {
                        setResult(result);
                    }
                });
            }
        }.blockUntilFinished();

        assertEquals("resource should have proper data", "y'all", resource.myString);
        assertNotNull("resource should have local id", resource.getId());
        assertEquals("resource should have been created locally", 1, GenericDatabase.getInstance().getDao(NoNetworkResource.class).countOf());

        NoNetworkResource r = new Synchronizer<NoNetworkResource>() {
            @Override
            public void run() {
                NoNetworkResource.SOURCE.find(resource.getId(), new Source.OperationCallback<NoNetworkResource>() {
                    @Override
                    public void onResult(@Nullable NoNetworkResource result) {
                        setResult(result);
                    }
                });
            }
        }.blockUntilFinished();

        assertEquals("same resource should be returned by callback", resource, r);

        resource.myString = "lla'y";

        r = new Synchronizer<NoNetworkResource>() {
            @Override
            public void run() {
                resource.save(new Source.OperationCallback<NoNetworkResource>() {
                    @Override
                    public void onResult(@Nullable NoNetworkResource result) {
                        setResult(result);
                    }
                });
            }
        }.blockUntilFinished();

        assertEquals("same resource should be returned by callback", resource, r);
        assertEquals("data should be updated", "lla'y", r.myString);
        assertNotNull("resource should still have a local id", r.getId());

        new Synchronizer<NoNetworkResource>() {
            @Override
            public void run() {
                resource.delete(new Source.OperationCallback<NoNetworkResource>() {
                    @Override
                    public void onResult(@Nullable NoNetworkResource result) {
                        setResult(result);
                    }
                });
            }
        }.blockUntilFinished();

        assertEquals("deleting locally should not have created a deleted resoure row", 0, GenericDatabase.getInstance().getDao(DeletedResource.class).countOf());
    }

    public void testFetchingSingleFromNetwork() throws Exception {
        assertEquals("There should be no items at start", 0, GenericDatabase.getInstance().getDao(DummyObject.class).countOf());

        int readCount = DummyObjectMockServer.getInstance().readCount;
        DummyObject o = new Synchronizer<DummyObject>() {
            /**
             * Starts executing the active part of the class' code. This method is
             * called when a thread is started that has been created with a class which
             * implements {@code Runnable}.
             */
            @Override
            public void run() {
                DummyObjectSource.getInstance().findByServerId(5, new Source.OperationCallback<DummyObject>() {
                    @Override
                    public void onResult(@Nullable DummyObject result) {
                        setResult(result);
                    }
                });
            }
        }.blockUntilFinished();

        assertEquals("server should be hit", readCount+1, DummyObjectMockServer.getInstance().readCount);
        assertNotNull("an object should be returned", o);
        assertTrue("object should have server presence", o.hasServerId());
        assertEquals("object should have the right server id", Integer.valueOf(5), o.getServerId());
        assertNotNull("object should have local id", o.getId());
        assertFalse("object should not be new", o.isNew());
        assertEquals("object should have proper values", "dummy5", o.getName());
        assertEquals("database object should exist", 1, GenericDatabase.getInstance().getDao(DummyObject.class).countOf());
        assertEquals("object should be cached", 1, ((WeakMapNetworkResourceCache)DummyObjectSource.getInstance().getResourceCache()).size());
    }

    public void testFetchingManyFromNetwork() throws Exception {
        assertEquals("There should be no items at start", 0, GenericDatabase.getInstance().getDao(DummyObject.class).countOf());

        int readCount = DummyObjectMockServer.getInstance().readCount;
        List<DummyObject> objects = new Synchronizer<List<DummyObject>>() {
            /**
             * Starts executing the active part of the class' code. This method is
             * called when a thread is started that has been created with a class which
             * implements {@code Runnable}.
             */
            @Override
            public void run() {
                DummyObjectSource.getInstance().getManyFromNetwork(null, new Source.OperationCallback<List<DummyObject>>() {
                    @Override
                    public void onResult(@Nullable List<DummyObject> result) {
                        setResult(result);
                    }
                });
            }
        }.blockUntilFinished();

        assertFalse("some items should be returned", objects.isEmpty());
        int count = objects.size();
        assertEquals("server should be hit", readCount + count, DummyObjectMockServer.getInstance().readCount);
        for(DummyObject o : objects) {
            assertTrue("object should have server presence", o.hasServerId());
            assertNotNull("object should have a server id", o.getServerId());
            assertNotNull("object should have local id", o.getId());
            assertFalse("object should not be new", o.isNew());
            assertTrue("object should have proper values", o.getName().matches("dummy[0-9]+"));
        }
        assertEquals("database objects should exist", count, GenericDatabase.getInstance().getDao(DummyObject.class).countOf());
        assertEquals("objects should be cached", count, ((WeakMapNetworkResourceCache)DummyObjectSource.getInstance().getResourceCache()).size());
    }


    /**
     * This is a slow running test of performance. It is disabled
     */
    @Suppress
    public void testBackgroundOperationSpeed() throws Exception {
        int testSize = 1000;
        DummyObject[] dummyObjects = new DummyObject[testSize];
        for (int i = 0; i < testSize; i++) {
            dummyObjects[i] = new DummyObject("test", i, null);
        }

        long start = System.nanoTime();
        for (int i = 0; i < testSize; i++) {
            final DummyObject d = dummyObjects[i];
            new Synchronizer<DummyObject>() {
                @Override
                public void run() {
                    d.save(new Source.OperationCallback<DummyObject>() {
                        @Override
                        public void onResult(@Nullable DummyObject result) {
                            setResult(result);
                        }
                    });
                }
            }.blockUntilFinished();
        }
        Log.w("OpSpeed", "Create total time was: " + ((System.nanoTime() - start) / 1000f / 1000f) + " ms");

        start = System.nanoTime();
        for (int i = 0; i < testSize; i++) {
            new Synchronizer<List<DummyObject>>() {
                @Override
                public void run() {
                    DummyObjectSource.getInstance().getManyFromNetwork(null, new Source.OperationCallback<List<DummyObject>>() {
                        @Override
                        public void onResult(@Nullable List<DummyObject> result) {
                            setResult(result);
                        }
                    });
                }
            }.blockUntilFinished();
        }
        Log.w("OpSpeed", "Remote fetch total time was: " + ((System.nanoTime() - start) / 1000f / 1000f) + " ms");
    }
}
