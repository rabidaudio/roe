package audio.rabid.dev.network_orm.models;

import android.os.Looper;
import android.support.annotation.Nullable;
import android.test.AndroidTestCase;
import android.test.suitebuilder.annotation.Suppress;
import android.util.Log;

import java.util.List;

import audio.rabid.dev.network_orm.ChangeDetectorObserver;
import audio.rabid.dev.network_orm.Synchronizer;
import audio.rabid.dev.network_orm.testobjects.DummyObject;
import audio.rabid.dev.network_orm.testobjects.DummyObjectMockServer;
import audio.rabid.dev.network_orm.testobjects.GenericDatabase;

/**
 * Created by charles on 10/30/15.
 */
public class GenericSourceTest extends AndroidTestCase {

    public void setUp(){
        GenericDatabase.getInstance(getContext()).clearDatabase(); //initialize database
        DummyObjectMockServer.getInstance().setNetworkAvailable(true);
    }

    /**
     * Test each CRUD operation on a single object while network connectivity exists
     */
    public void testBasicSingleOperationsWithNetwork() throws Exception {
        DummyObjectMockServer.getInstance().setNetworkAvailable(true);

        long start;

        final DummyObject d = new DummyObject("meow", 0, null);

        assertTrue("new object should not have a local id", d.getId() < 0);
        assertTrue("new object should not have a server id", d.getServerId() < 0);
        assertFalse("new object should not be synced", d.isSynced());

        //CREATE
        start = System.nanoTime();
        DummyObject result = (new Synchronizer<DummyObject>(){
            public void run(){
                d.save(new OperationCallback<DummyObject>() {
                    @Override
                    public void onResult(@Nullable DummyObject r) {
                        setResult(r);
                    }
                });
            }
        }).blockUntilFinished();
        Log.i("GenericSourceTest", "create took ms " + (System.nanoTime() - start) / 1000f / 1000f);

        assertNotNull("saved object should be returned in callback", result);
        assertTrue("saved object should have a local id", result.getId() > 0);
        assertTrue("saved object should have a server id", result.getServerId() > 0);
        assertTrue("saved object should be synced", result.isSynced());
        assertEquals("saved object should be the same instance", d, result);
        assertNotNull("item should be in database", DummyObject.SOURCE.getDao().queryForId(d.getId()));

        //READ
        start = System.nanoTime();
        result = new Synchronizer<DummyObject>() {
            @Override
            public void run() {
                DummyObject.SOURCE.getLocal(d.getId(), new OperationCallback<DummyObject>() {
                    @Override
                    public void onResult(@Nullable DummyObject result) {
                        setResult(result);
                    }
                });
            }
        }.blockUntilFinished();
        Log.i("GenericSourceTest", "read took ms " + (System.nanoTime() - start) / 1000f / 1000f);

        assertEquals("object by id should be the same instance", d, result);

        //UPDATE
        d.age = 10;
        start = System.nanoTime();
        result = new Synchronizer<DummyObject>() {
            @Override
            public void run() {
                d.save(new OperationCallback<DummyObject>() {
                    @Override
                    public void onResult(@Nullable DummyObject result) {
                        setResult(result);
                    }
                });
            }
        }.blockUntilFinished();
        Log.i("GenericSourceTest", "update took ms " + (System.nanoTime() - start) / 1000f / 1000f);

        assertNotNull("updated object should be returned in callback", result);
        assertEquals("updated object should have the same local id", d.getId(), result.getId());
        assertEquals("updated object should have the same server id", d.getServerId(), result.getServerId());
        assertEquals("updated object should have the new values", 10, result.age);
        assertEquals("updated object should be the same instance", d, result);

        //DELETE
        start = System.nanoTime();
        result = new Synchronizer<DummyObject>() {
            @Override
            public void run() {
                d.delete(new OperationCallback<DummyObject>() {
                    @Override
                    public void onResult(@Nullable DummyObject result) {
                        setResult(result);
                    }
                });
            }
        }.blockUntilFinished();
        Log.i("GenericSourceTest", "delete took ms " + (System.nanoTime() - start) / 1000f / 1000f);

        assertTrue("item should report it was deleted", result.wasDeleted());
        assertNull("item should no longer be in database", DummyObject.SOURCE.getDao().queryForId(d.getId()));
    }

    /**
     * Test CRUD operations on a single object without network, and then enabling network and syncing
     */
    public void testBasicSingleOperationsWithoutNetwork() throws Exception {
        DummyObjectMockServer.getInstance().setNetworkAvailable(false);

        final DummyObject d = new DummyObject("meow", 0, null);

        assertTrue("new object should not have a local id", d.getId() < 0);
        assertTrue("new object should not have a server id", d.getServerId() < 0);
        assertFalse("new object should not be synced", d.isSynced());

        //CREATE
        DummyObject result = (new Synchronizer<DummyObject>(){
            public void run(){
                d.save(new OperationCallback<DummyObject>() {
                    @Override
                    public void onResult(@Nullable DummyObject r) {
                        setResult(r);
                    }
                });
            }
        }).blockUntilFinished();

        assertNotNull("saved object should be returned in callback", result);
        assertTrue("saved object should have a local id", result.getId() > 0);
        assertFalse("saved object should NOT have a server id", result.getServerId() > 0);
        assertFalse("saved object should NOT be synced", result.isSynced());
        assertEquals("saved object should be the same instance", d, result);
        assertNotNull("item should be in database", DummyObject.SOURCE.getDao().queryForId(d.getId()));

        //READ
        result = new Synchronizer<DummyObject>() {
            @Override
            public void run() {
                DummyObject.SOURCE.getLocal(d.getId(), new OperationCallback<DummyObject>() {
                    @Override
                    public void onResult(@Nullable DummyObject result) {
                        setResult(result);
                    }
                });
            }
        }.blockUntilFinished();

        assertEquals("object by id should be the same instance", d, result);

        //UPDATE
        d.age = 10;

        result = new Synchronizer<DummyObject>() {
            @Override
            public void run() {
                d.save(new OperationCallback<DummyObject>() {
                    @Override
                    public void onResult(@Nullable DummyObject result) {
                        setResult(result);
                    }
                });
            }
        }.blockUntilFinished();

        assertNotNull("updated object should be returned in callback", result);
        assertFalse("saved object should NOT have a server id", result.getServerId() > 0);
        assertFalse("saved object should NOT be synced", result.isSynced());
        assertEquals("updated object should have the same local id", d.getId(), result.getId());
        assertEquals("updated object should have the new values", 10, result.age);
        assertEquals("updated object should be the same instance", d, result);

        //SYNC NO NET
        long prevUpdateTime = d.getUpdatedAt().getTime();
        List<DummyObject> synced = new Synchronizer<List<DummyObject>>() {
            @Override
            public void run() {
                DummyObject.SOURCE.sync(new OperationCallback<List<DummyObject>>() {
                    @Override
                    public void onResult(@Nullable List<DummyObject> result) {
                        setResult(result);
                    }
                });
            }
        }.blockUntilFinished();

        assertTrue("no items should have synced", synced.isEmpty());
        assertFalse("the item should not be marked as synced", d.isSynced());
        assertEquals("the new update time should be the same as the old one", prevUpdateTime, d.getUpdatedAt().getTime());

        //SYNC NET
        DummyObjectMockServer.getInstance().setNetworkAvailable(true); //enable network

        prevUpdateTime = d.getUpdatedAt().getTime();
        synced = new Synchronizer<List<DummyObject>>() {
            @Override
            public void run() {
                DummyObject.SOURCE.sync(new OperationCallback<List<DummyObject>>() {
                    @Override
                    public void onResult(@Nullable List<DummyObject> result) {
                        setResult(result);
                    }
                });
            }
        }.blockUntilFinished();

        assertEquals("one item should have synced", 1, synced.size());
        assertEquals("the synced item should be the same instance", d, synced.get(0));
        assertTrue("the item should be marked as synced", d.isSynced());
        assertTrue("the new update time should be larger than the old one", d.getUpdatedAt().getTime()>prevUpdateTime);
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

        d.addObserver(catcher1);
        d.addObserver(catcher2);

        //CREATE
        DummyObject result = (new Synchronizer<DummyObject>() {
            public void run() {
                d.save(new OperationCallback<DummyObject>() {
                    @Override
                    public void onResult(@Nullable DummyObject r) {
                        setResult(r);
                    }
                });
            }
        }).blockUntilFinished();

        assertNotNull(result);
        assertFalse("observer should not see a create", catcher1.sawChange());
        assertFalse("observer should not see a create", catcher2.sawChange());

        //UPDATE
        d.age = 10;
        result = new Synchronizer<DummyObject>() {
            @Override
            public void run() {
                d.save(new OperationCallback<DummyObject>() {
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
        assertEquals("observers should be called on main thread", Looper.getMainLooper().getThread(), catcher1.getCallingThread());

        //DELETE
        result = new Synchronizer<DummyObject>() {
            @Override
            public void run() {
                d.delete(new OperationCallback<DummyObject>() {
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
        assertEquals("observers should be called on main thread", Looper.getMainLooper().getThread(), catcher1.getCallingThread());
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
                    d.save(new OperationCallback<DummyObject>() {
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
                    DummyObject.SOURCE.createOrUpdateManyFromNetwork(null, new OperationCallback<List<DummyObject>>() {
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