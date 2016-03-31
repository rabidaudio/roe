package audio.rabid.dev.sampleapp.views;

import android.app.Activity;
import android.graphics.Bitmap;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import audio.rabid.dev.roe.views.BasicViewHolder;
import audio.rabid.dev.sampleapp.R;
import audio.rabid.dev.sampleapp.models.Author;
import butterknife.Bind;
import butterknife.ButterKnife;

/**
 * Created by charles on 10/25/15.
 */
public class AuthorViewHolder extends BasicViewHolder<Author> {

    @Bind(R.id.avatar)
    ImageView avatar;
    @Bind(R.id.name)
    TextView name;
    @Bind(R.id.email)
    TextView email;

    public AuthorViewHolder(View v) {
        super(v);
    }

    public AuthorViewHolder(Activity a) {
        super(a);
    }

    @Override
    protected void draw(@NonNull Author author, View parent) {
        ButterKnife.bind(this, parent);
        name.setText(author.getName());
        email.setText(author.getEmail());

        avatar.setImageResource(R.drawable.ic_keyboard_control);
        author.getAvatarBitmap(new Author.BitmapCallback() {
            @Override
            public void onBitmapReady(@Nullable Bitmap bitmap) {
                avatar.setImageBitmap(bitmap);
            }
        });
    }

    @Override
    protected void onNoItem(View parent) {
        ButterKnife.bind(this, parent);
        avatar.setImageResource(R.drawable.ic_not_interested);
        name.setText(R.string.author_not_found);
        email.setText(null);
    }
}
