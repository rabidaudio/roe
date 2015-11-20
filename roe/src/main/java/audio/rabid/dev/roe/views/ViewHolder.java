package audio.rabid.dev.roe.views;

import android.app.Activity;
import android.support.annotation.NonNull;
import android.view.View;

import audio.rabid.dev.roe.models.NetworkSyncableDao;
import audio.rabid.dev.roe.models.resource.Resource;
import audio.rabid.dev.roe.models.TypedObserver;

/**
 * Created by charles on 10/25/15.
 *
 * A cleaner way to manage views by mapping data to {@link View}s. Create a ViewHolder, passing in the
 * parent {@link View}. Then call {@link #setItem(T)} when your data becomes available.
 * This will set itself as an observable, so when your model changes, your draw method will be called.
 */
public abstract class ViewHolder<T extends Resource<?,?>> implements TypedObserver<T> {

    private T item;
    private View container;

    private NetworkSyncableDao<T,?,?> resourceDao;

    public ViewHolder(View v, NetworkSyncableDao<T,?,?> resourceDao) {
        container = v;
        this.resourceDao = resourceDao;
    }

    public ViewHolder(Activity a, NetworkSyncableDao<T,?,?> resourceDao) {
        container = a.getWindow().getDecorView();
        this.resourceDao = resourceDao;
    }

    @SuppressWarnings("unchecked")
    public void setItem(T item) {
        this.item = item;
        if (item != null) {
            resourceDao.addObserver(this, item);
            draw(item, container);
        } else {
            onNoItem(container);
        }
    }

    @Override
    public void update(T observable, boolean deleted) {
        if (deleted) {
            onNoItem(container);
        } else {
            item = observable;
            draw(item, container);
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