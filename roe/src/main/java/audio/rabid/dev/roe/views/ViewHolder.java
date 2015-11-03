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
 * parent {@link View}. Then call {@link #setItem(TypedObservable)} when your data becomes available.
 * This will set itself as an observable, so when your model changes, your draw method will be called.
 */
public abstract class ViewHolder<T extends TypedObservable> implements TypedObserver<T> {

    private T item;
    private View container;

    public ViewHolder(View v) {
        container = v;
    }

    public ViewHolder(Activity a) {
        container = a.getWindow().getDecorView();
    }

    @SuppressWarnings("unchecked")
    public void setItem(T item) {
        this.item = item;
        if (item != null) {
            item.addObserver(this);
            draw(item, container);
        } else {
            onNoItem(container);
        }
    }

    @Override
    public void update(T observable, Object data) {
        item = observable;
        draw(item, container);
    }

    /**
     * Map your data to a view. This is called whenever data changes (including when it is deleted).
     *
     * @param item   the data
     * @param parent the view
     */
    protected abstract void draw(@NonNull T item, View parent);

    /**
     * Called when data is not available ({@link #setItem(TypedObservable)} was called with null).
     *
     * @param parent the view
     */
    protected abstract void onNoItem(View parent);
}
