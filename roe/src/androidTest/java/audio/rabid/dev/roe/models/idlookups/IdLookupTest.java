package audio.rabid.dev.roe.models.idlookups;

import android.test.AndroidTestCase;

import java.util.List;

import audio.rabid.dev.roe.models.NetworkSyncableDao;

/**
 * Created by charles on 11/13/15.
 */
public class IdLookupTest extends AndroidTestCase {

    public void setUp() throws Exception {
        IdDb.createInstance(getContext());
        IdDb.getInstance().clearTables();
    }

    public void testLookups() throws Exception {

        NetworkSyncableDao<StringIdResource, String, String> sDao = IdDb.getInstance().getDao(StringIdResource.class);
        NetworkSyncableDao<IntegerIdResource, Integer, String> i1Dao = IdDb.getInstance().getDao(IntegerIdResource.class);
        NetworkSyncableDao<IntIdResource, Integer, String> i2Dao = IdDb.getInstance().getDao(IntIdResource.class);
        NetworkSyncableDao<DoubleIdResource, Double, String> bDao = IdDb.getInstance().getDao(DoubleIdResource.class);

        StringIdResource s = new StringIdResource();
        s.setId("foobar");
        sDao.create(s);

        List<StringIdResource> r1 = sDao.queryForEq(sDao.getTableInfo().getIdField().getColumnName(), String.valueOf("foobar"));
        assertEquals(1, r1.size());
        assertEquals(s, r1.get(0));

        IntegerIdResource i = new IntegerIdResource();
        i1Dao.create(i);

        Integer i1id = i1Dao.extractId(i);

        List<IntegerIdResource> r2 = i1Dao.queryForEq(i1Dao.getTableInfo().getIdField().getColumnName(), String.valueOf(i1id));
        assertEquals(1, r2.size());
        assertEquals(i, r2.get(0));

        IntIdResource i2 = new IntIdResource();
        i2Dao.create(i2);

        int i2id = i2Dao.extractId(i2);

        List<IntIdResource> r3 = i2Dao.queryForEq(i2Dao.getTableInfo().getIdField().getColumnName(), String.valueOf(i2id));
        assertEquals(1, r3.size());
        assertEquals(i2, r3.get(0));

        DoubleIdResource b = new DoubleIdResource();
        b.setId(Math.PI);
        bDao.create(b);

        List<DoubleIdResource> r4 = bDao.queryForEq(bDao.getTableInfo().getIdField().getColumnName(), String.valueOf(Math.PI));
        assertEquals(1, r4.size());
        assertEquals(b, r4.get(0));
    }
}
