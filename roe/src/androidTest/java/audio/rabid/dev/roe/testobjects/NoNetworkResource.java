package audio.rabid.dev.roe.testobjects;

import android.support.annotation.Nullable;

import com.j256.ormlite.field.DatabaseField;

import audio.rabid.dev.roe.models.JSONField;
import audio.rabid.dev.roe.models.Resource;
import audio.rabid.dev.roe.models.Source;

/**
 * Created by charles on 11/3/15.
 */
public class NoNetworkResource implements Resource<Integer> {

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
    public String localIdToString() {
        return String.valueOf(id);
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

    public void save(@Nullable Source.OperationCallback<NoNetworkResource> callback) {
        getSource().createOrUpdate(this, callback);
    }

    public void delete(@Nullable Source.OperationCallback<NoNetworkResource> callback) {
        getSource().delete(this, callback);
    }
}

