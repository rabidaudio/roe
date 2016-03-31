package audio.rabid.dev.roe.views;

import android.app.Activity;
import android.support.annotation.NonNull;
import android.view.View;

import audio.rabid.dev.roe.models.TypedObservable;
import audio.rabid.dev.roe.models.TypedObserver;

/**
 * Created by charles on 10/25/15.
 *
 * A cleaner way to manage views by mapping data to {@link View}s. Create a ViewHolder, passing in the
 * parent {@link View}. Then call {@link #setItem(T)} when your data becomes available.
 * This will set itself as an observable, so when your model changes, your draw method will be called.
 */
public abstract class ViewHolder<T extends TypedObservable<T>> extends BasicViewHolder<T> implements TypedObserver<T> {

    public ViewHolder(View v) {
        super(v);
    }

    public ViewHolder(Activity a) {
        super(a);
    }

    @Override
    public void setItem(T item) {
        if (item != null) {
            item.addObserver(this);
        }
        super.setItem(item);
    }
}
