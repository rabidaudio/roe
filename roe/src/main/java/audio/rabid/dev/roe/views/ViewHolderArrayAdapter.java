package audio.rabid.dev.roe.views;

import android.content.Context;
import android.support.annotation.Nullable;
import android.view.View;

import java.util.List;

import audio.rabid.dev.roe.models.TypedObservable;

/**
 * Created by charles on 10/25/15.
 */
public abstract class ViewHolderArrayAdapter<T extends TypedObservable, H extends ViewHolder<T>> extends EasyArrayAdapter<T, H> {

    public ViewHolderArrayAdapter(Context context, int layoutId, @Nullable List<T> list) {
        super(context, layoutId, list);
    }

    @Override
    protected void onDrawView(T object, H viewHolder, View parent) {
        viewHolder.setItem(object);
    }
}
