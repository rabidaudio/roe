package audio.rabid.dev.roe.testobjects;

import com.alibaba.fastjson.annotation.JSONField;
import com.j256.ormlite.field.DatabaseField;
import com.raizlabs.android.parser.core.Parseable;

/**
 * Created by charles on 11/3/15.
 */
@Parseable
public class NoNetworkResource {

    @JSONField
    @DatabaseField(generatedId = true)
    private Integer id;

    @DatabaseField
    @JSONField
    public String myString;

    public Integer getId() {
        return id;
    }

    public boolean isNew() {
        return id == null;
    }
}

