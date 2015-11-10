package audio.rabid.dev.roe.testobjects;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

import audio.rabid.dev.roe.models.IntegerKeyedNetworkResource;
import audio.rabid.dev.roe.models.JSONField;
import audio.rabid.dev.roe.models.NetworkSource;
import audio.rabid.dev.roe.models.Source;
import audio.rabid.dev.roe.models.rails.RailsSource;

/**
 * Created by charles on 10/30/15.
 */
@DatabaseTable(tableName = "children")
public class DummyChild extends IntegerKeyedNetworkResource {

    @JSONField
    @DatabaseField
    public String name;

    @Override
    public Source<DummyChild, Integer> getSource() {
        return Source;
    }

    public static NetworkSource<DummyChild, Integer, Integer> Source = new NetworkSource<>(DummyObjectMockServer.getInstance(), GenericDatabase.getInstance(), DummyChild.class);
}
