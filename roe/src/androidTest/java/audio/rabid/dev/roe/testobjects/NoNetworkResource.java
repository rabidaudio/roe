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
    public boolean isNew() {
        return id == null;
    }

    @Override
    public Source<NoNetworkResource, Integer> getSource() {
        return MySource;
    }

    public void save(@Nullable Source.OperationCallback<NoNetworkResource> callback) {
        getSource().createOrUpdate(this, callback);
    }

    public void delete(@Nullable Source.OperationCallback<NoNetworkResource> callback) {
        getSource().delete(this, callback);
    }

    public static Source<NoNetworkResource, Integer> MySource = new Source<>(GenericDatabase.getInstance(), NoNetworkResource.class);
}

