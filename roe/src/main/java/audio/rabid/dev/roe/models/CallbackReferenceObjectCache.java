package audio.rabid.dev.roe.models;

import com.j256.ormlite.dao.ReferenceObjectCache;

/**
 * Created by charles on 11/10/15.
 *
 * A simple weak reference object cache that we use to catch inserts and deletes.
 */
public class CallbackReferenceObjectCache extends ReferenceObjectCache {

    OnCacheAddListener listener;

    /**
     * @inheritDoc
     */
    public CallbackReferenceObjectCache(boolean useWeak) {
        super(useWeak);
    }

    public <T, ID> void setListener(OnCacheAddListener<T, ID> listener) {
        this.listener = listener;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T, ID> void put(Class<T> clazz, ID id, T data) {
        super.put(clazz, id, data);
        if (listener != null) {
            listener.onCacheItemAdded(clazz, id, data);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T, ID> T updateId(Class<T> tClass, ID oldId, ID newId) {
        T result = super.updateId(tClass, oldId, newId);
        if (listener != null) {
            listener.onCacheItemIdUpdated(tClass, newId, result);
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T, ID> void remove(Class<T> tClass, ID id) {
        T item = get(tClass, id);
        super.remove(tClass, id);
        if (listener != null) {
            listener.onCacheItemDeleted(tClass, id, item);
        }
    }

    public interface OnCacheAddListener<T, ID> {

        void onCacheItemAdded(Class<T> tClass, ID id, T data);

        void onCacheItemIdUpdated(Class<T> tClass, ID id, T data);

        void onCacheItemDeleted(Class<T> tClass, ID id, T data);
    }
}