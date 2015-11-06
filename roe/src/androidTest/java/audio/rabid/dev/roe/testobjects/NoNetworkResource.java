package audio.rabid.dev.roe.testobjects;

import com.j256.ormlite.field.DatabaseField;

import audio.rabid.dev.roe.models.JSONField;
import audio.rabid.dev.roe.models.Resource;
import audio.rabid.dev.roe.models.Source;

/**
 * Created by charles on 11/3/15.
 */
public class NoNetworkResource extends Resource<NoNetworkResource, Integer> {

    @DatabaseField(generatedId = true)
    private Integer id;

    @DatabaseField
    @JSONField
    public String myString;

    @Override
    public Integer getId() {
        return id;
    }

    @Override
    public boolean isNew() {
        return id == null;
    }

    public static final Source<NoNetworkResource, Integer> SOURCE = new Source.Builder<>(GenericDatabase.getInstance(), NoNetworkResource.class).build();

    @Override
    public Source<NoNetworkResource, Integer> getSource() {
        return SOURCE;
    }
}

