package audio.rabid.dev.network_orm.testobjects;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

import audio.rabid.dev.network_orm.models.Resource;
import audio.rabid.dev.network_orm.models.Source;

/**
 * Created by charles on 10/30/15.
 */
@DatabaseTable(tableName = "children")
public class DummyChild extends Resource<DummyChild> {

    @DatabaseField
    String name;


    @Override
    public Source<DummyChild> getSource() {
        return null;
    }
}
