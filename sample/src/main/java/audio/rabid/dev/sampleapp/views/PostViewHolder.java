package audio.rabid.dev.sampleapp.views;

import android.app.Activity;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;
import android.widget.TextView;

import audio.rabid.dev.network_orm.views.ViewHolder;
import audio.rabid.dev.sampleapp.R;
import audio.rabid.dev.sampleapp.models.Post;
import butterknife.Bind;
import butterknife.ButterKnife;

/**
 * Created by charles on 10/25/15.
 */
public class PostViewHolder extends ViewHolder<Post> {

    @Bind(R.id.title)
    TextView title;
    @Nullable
    @Bind(R.id.body)
    TextView body;
    @Nullable
    @Bind(R.id.author)
    View author;
    @Nullable
    @Bind(R.id.createdAt)
    TextView createdAt;

    AuthorViewHolder authorViewHolder;

    public PostViewHolder(View v) {
        super(v);
    }

    public PostViewHolder(Activity a) {
        super(a);
    }

    @Override
    protected void draw(@NonNull Post item, View parent) {
        ButterKnife.bind(this, parent);

        title.setText(item.getTitle());
        if (body != null)
            body.setText(item.getBody());

        if (createdAt != null)
            createdAt.setText(item.getCreatedAt().toLocaleString());

        //pass child view through
        if (author != null) {
            authorViewHolder = new AuthorViewHolder(author);
            authorViewHolder.setItem(item.getAuthor());
        }
    }

    @Override
    protected void onNoItem(View parent) {
        title.setText("Post not found");
    }
}
