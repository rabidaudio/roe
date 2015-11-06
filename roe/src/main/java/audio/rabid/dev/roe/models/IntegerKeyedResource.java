package audio.rabid.dev.roe.models;

import com.j256.ormlite.field.DatabaseField;

/**
 * Created by charles on 11/5/15.
 */
public abstract class IntegerKeyedResource<R extends IntegerKeyedResource<R>> extends Resource<R, Integer> {

    @DatabaseField(generatedId = true)
    @JSONField(export = false, accept = false)
    protected Integer id;

    @Override
    public Integer getId() {
        return id;
    }

    @Override
    public boolean isNew() {
        return id == null;
    }
}
