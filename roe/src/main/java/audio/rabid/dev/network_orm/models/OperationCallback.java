package audio.rabid.dev.network_orm.models;

import android.support.annotation.Nullable;

/**
 * Created by charles on 11/1/15.
 */
public interface OperationCallback<Q> {
    void onResult(@Nullable Q result);
}