package audio.rabid.dev.roe.models;

import android.support.annotation.Nullable;
import android.test.AndroidTestCase;

import org.json.JSONObject;

import audio.rabid.dev.roe.Synchronizer;
import audio.rabid.dev.roe.testobjects.DummyChild;
import audio.rabid.dev.roe.testobjects.DummyObject;
import audio.rabid.dev.roe.testobjects.DummyObjectMockServer;
import audio.rabid.dev.roe.testobjects.DummyObjectSource;
import audio.rabid.dev.roe.testobjects.GenericDatabase;

/**
 * Created by charles on 11/10/15.
 */
public class ForeignFieldTest extends AndroidTestCase {

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

    public void testAddExistingChildWithoutNet() throws Exception {

        DummyObjectMockServer.getInstance().setNetworkAvailable(false);

        final DummyChild c = new DummyChild();
        c.name = "child";

        new Synchronizer<DummyChild>() {
            /**
             * Starts executing the active part of the class' code. This method is
             * called when a thread is started that has been created with a class which
             * implements {@code Runnable}.
             */
            @Override
            public void run() {
                DummyChild.Source.create(c, new Source.OperationCallback<DummyChild>() {
                    @Override
                    public void onResult(@Nullable DummyChild result) {
                        setResult(result);
                    }
                });
            }
        }.blockUntilFinished();

        assertFalse(c.isNew());
        assertNotNull(c.getId());

        final DummyObject o = new DummyObject("parent", 1, c);

        new Synchronizer<DummyObject>(){
            @Override
            public void run(){
                o.save(new Source.OperationCallback<DummyObject>() {
                    @Override
                    public void onResult(@Nullable DummyObject result) {
                        setResult(result);
                    }
                });
            }
        }.blockUntilFinished();

        assertFalse(o.isNew());
        assertNotNull(o.getId());

        assertEquals(c, o.getChild());

        assertEquals(1, DummyObjectSource.getInstance().getDao().queryForEq("child_id", c.getId()).size());
    }

    public void testAddNewChildWithoutNet() throws Exception {

        DummyObjectMockServer.getInstance().setNetworkAvailable(false);

        final DummyChild c = new DummyChild();
        c.name = "child";

        assertTrue(c.isNew());
        assertNull(c.getId());

        final DummyObject o = new DummyObject("parent", 1, c);

        new Synchronizer<DummyObject>(){
            @Override
            public void run(){
                o.save(new Source.OperationCallback<DummyObject>() {
                    @Override
                    public void onResult(@Nullable DummyObject result) {
                        setResult(result);
                    }
                });
            }
        }.blockUntilFinished();

        assertFalse(o.isNew());
        assertNotNull(o.getId());

        assertEquals(c, o.getChild());

        //saving parent should also save child
        assertFalse(c.isNew());
        assertNotNull(c.getId());

        assertEquals(1, DummyObjectSource.getInstance().getDao().queryForEq("child_id", c.getId()).size());
    }
}
