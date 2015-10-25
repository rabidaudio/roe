package audio.rabid.dev.sampleapp.views;

import android.app.Activity;
import android.view.View;
import android.widget.TextView;

import audio.rabid.dev.network_orm.ViewHolder;
import audio.rabid.dev.sampleapp.R;
import audio.rabid.dev.sampleapp.models.Post;
import butterknife.Bind;
import butterknife.ButterKnife;

/**
 * Created by charles on 10/25/15.
 */
public class PostViewHolder extends ViewHolder<Post> {

    @Bind(R.id.title) TextView title;
    @Bind(R.id.body) TextView body;
    @Bind(R.id.author) View author;

    AuthorViewHolder authorViewHolder;

    public PostViewHolder(View v) {
        super(v);
    }

    public PostViewHolder(Activity a) {
        super(a);
    }

    @Override
    protected void draw(Post item, View parent) {
        ButterKnife.bind(this, parent);

        title.setText(item.getTitle());
        body.setText(item.getBody());

        //pass child view through
        authorViewHolder = new AuthorViewHolder(author);
        authorViewHolder.setItem(item.getAuthor());
    }

    @Override
    protected void onNoItem(View parent) {
        body.setText("Post not found");
    }
}
