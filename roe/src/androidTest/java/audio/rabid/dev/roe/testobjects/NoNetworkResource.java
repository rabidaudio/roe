package audio.rabid.dev.roe.testobjects;

import com.j256.ormlite.field.DatabaseField;

import audio.rabid.dev.roe.models.JSONField;
import audio.rabid.dev.roe.models.Resource;
import audio.rabid.dev.roe.models.Source;

/**
 * Created by charles on 11/3/15.
 */
public class NoNetworkResource extends Resource<NoNetworkResource> {

    @DatabaseField
    @JSONField
    public String myString;

    public static final Source<NoNetworkResource> SOURCE = new Source.Builder<>(GenericDatabase.getInstance(), NoNetworkResource.class).build();

    @Override
    public Source<NoNetworkResource> getSource() {
        return SOURCE;
    }
}

