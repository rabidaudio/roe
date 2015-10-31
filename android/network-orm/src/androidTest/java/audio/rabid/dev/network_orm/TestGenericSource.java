package audio.rabid.dev.network_orm;

import android.test.AndroidTestCase;

import org.jetbrains.annotations.Nullable;

import audio.rabid.dev.network_orm.testobjects.DummyObject;
import audio.rabid.dev.network_orm.testobjects.DummyObjectMockServer;
import audio.rabid.dev.network_orm.testobjects.GenericDatabase;

/**
 * Created by charles on 10/30/15.
 */
public class TestGenericSource extends AndroidTestCase {

    public void setUp(){
        GenericDatabase.getInstance(getContext()).clearDatabase(); //initialize database
    }

    public void testBasicSingleOperationsWithNetwork() throws Exception {
        DummyObjectMockServer.getInstance().setNetworkAvailable(true);

        final DummyObject d = new DummyObject("meow", 0, null);

        assertTrue("new object should not have a local id", d.getId() < 0);
        assertTrue("new object should not have a server id", d.getServerId() < 0);
        assertFalse("new object should not be synced", d.isSynced());

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

        assertNotNull("saved object should be returned in callback", result);
        assertTrue("saved object should have a local id", result.getId() > 0);
        assertTrue("saved object should have a server id", result.getServerId() > 0);
        assertTrue("saved object should be synced", result.isSynced());
        assertEquals("saved object should be the same instance", d, result);
        assertNotNull("item should be in database", DummyObject.SOURCE.getDao().queryForId(d.getId()));

        //READ
        result = new Synchronizer<DummyObject>() {
            @Override
            public void run() {
                DummyObject.SOURCE.getLocal(d.getId(), new Source.OperationCallback<DummyObject>() {
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
                d.save(new Source.OperationCallback<DummyObject>() {
                    @Override
                    public void onResult(@Nullable DummyObject result) {
                        setResult(result);
                    }
                });
            }
        }.blockUntilFinished();

        assertNotNull("updated object should be returned in callback", result);
        assertEquals("updated object should have the same local id", d.getId(), result.getId());
        assertEquals("updated object should have the same server id", d.getServerId(), result.getServerId());
        assertEquals("updated object should have the new values", 10, result.age);
        assertEquals("updated object should be the same instance", d, result);

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

        assertTrue("observers should see the item is deleted", result.wasDeleted());
        assertNull("item should no longer be in database", DummyObject.SOURCE.getDao().queryForId(d.getId()));
    }


}
