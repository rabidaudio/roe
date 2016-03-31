package audio.rabid.dev.roe.views;

import android.app.Activity;
import android.support.annotation.NonNull;
import android.view.View;

/**
 * Created by  charles  on 3/30/16.
 */
public abstract class BasicViewHolder<T> {

    private T item;
    private View container;

    public BasicViewHolder(View v) {
        container = v;
    }

    public BasicViewHolder(Activity a) {
        container = a.getWindow().getDecorView();
    }

    public void setItem(T item) {
        this.item = item;
        if (item != null) {
            draw(item, container);
        } else {
            onNoItem(container);
        }
    }

    /**
     * Map your data to a view. This is called whenever data changes (including when it is deleted).
     *
     * @param item   the data
     * @param parent the view
     */
    protected abstract void draw(@NonNull T item, View parent);

    /**
     * Called when data is not available ({@link #setItem(T)} was called with null).
     *
     * @param parent the view
     */
    protected abstract void onNoItem(View parent);
}
