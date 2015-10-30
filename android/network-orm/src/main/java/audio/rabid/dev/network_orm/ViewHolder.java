package audio.rabid.dev.network_orm;

import android.app.Activity;
import android.view.View;

/**
 * Created by charles on 10/25/15.
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

    protected abstract void draw(T item, View parent);

    protected abstract void onNoItem(View parent);

    @Override
    public void update(T observable, Object data) {
        item = observable;
        draw(item, container);
    }
}
