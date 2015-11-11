package audio.rabid.dev.roe.testobjects;

import com.alibaba.fastjson.annotation.JSONField;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

import audio.rabid.dev.roe.models.NetworkRoeDao;

/**
 * Created by charles on 10/30/15.
 */
@DatabaseTable(tableName = "children", daoClass = NetworkRoeDao.class)
public class DummyChild {

    @JSONField
    @DatabaseField(id = true, canBeNull = false)
    public String name;
}
