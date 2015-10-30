package audio.rabid.dev.utils;

import android.content.Context;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import java.util.ArrayList;
import java.util.List;

/**
 * I got tired of building the same ArrayAdapter over and over.
 * <p/>
 * Subclass this class with two generics: {@link T} is the class of the "Model" object
 * and {@link V} is the class of the "View" object. Typically, you'll want to make a protected
 * ViewHolder subclass. If you do that, {@link #createViewHolder(View)} becomes as simple as
 * <p/>
 * <pre>
 *         @Override
 *         protected ViewHolder createViewHolder(View v) {
 *              return new ViewHolder(v);
 *         }
 * </pre>
 * <p/>
 * The other method you need is {@link #onDrawView(Object, Object)} which gives you the model and the
 * ViewHolder and allows you to map the data to the view.
 * <p/>
 * Everything else is handled for you (all the inflation bullshit, keeping track of the collection, etc.).
 *
 * @author Charles Julian Knight, <a href="mailto:charles@rabidaudio.com">charles@rabidaudio.com</a>
 * @see <a href="http://developer.android.com/training/improving-layouts/smooth-scrolling.html">ViewHolder Pattern</a>
 */
public abstract class EasyArrayAdapter<T, V> extends ArrayAdapter<T> {

    private Context context;
    private int layoutId;
    private List<T> list;

    /**
     * @param context  the parent context
     * @param layoutId the ID of the layout resource to use as the view for each item
     * @param list     the collection of backing objects
     */
    public EasyArrayAdapter(Context context, int layoutId, @Nullable List<T> list) {
        super(context, layoutId, (list == null ? new ArrayList<T>() : list));
        this.context = context;
        this.layoutId = layoutId;
        this.list = list;
    }

    protected abstract void onDrawView(T object, V viewHolder, View parent);

    protected abstract V createViewHolder(View v);

    public List<T> getCollection() {
        return list;
    }

    @Override
    @SuppressWarnings("unchecked")
    public View getView(int position, View view, ViewGroup viewGroup) {
        LayoutInflater inflater = LayoutInflater.from(context);

        View rowView;
        V viewHolder;
        if (view == null) {
            rowView = inflater.inflate(layoutId, viewGroup, false);
            viewHolder = createViewHolder(rowView);
            rowView.setTag(viewHolder);
        } else {
            rowView = view;
            viewHolder = (V) rowView.getTag();
            if (viewHolder == null) {
                //for some reason, no holder attached
                viewHolder = createViewHolder(rowView);
                rowView.setTag(viewHolder);
            }
        }
        onDrawView(list.get(position), viewHolder, rowView);
        return rowView;
    }
}