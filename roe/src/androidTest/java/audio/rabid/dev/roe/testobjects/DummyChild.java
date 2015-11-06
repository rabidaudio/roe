package audio.rabid.dev.roe.testobjects;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

import audio.rabid.dev.roe.models.IntegerKeyedNetworkResource;
import audio.rabid.dev.roe.models.JSONField;
import audio.rabid.dev.roe.models.Source;

/**
 * Created by charles on 10/30/15.
 */
@DatabaseTable(tableName = "children")
public class DummyChild extends IntegerKeyedNetworkResource<DummyChild> {

    @JSONField
    @DatabaseField
    String name;

    @Override
    public Source<DummyChild, Integer> getSource() {
        return null;
    }
}
