package audio.rabid.dev.sampleapp.views;

import android.app.Activity;
import android.graphics.Bitmap;
import android.support.annotation.Nullable;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import audio.rabid.dev.network_orm.Dao;
import audio.rabid.dev.network_orm.ViewHolder;
import audio.rabid.dev.sampleapp.R;
import audio.rabid.dev.sampleapp.models.Author;
import butterknife.Bind;
import butterknife.ButterKnife;

/**
 * Created by charles on 10/25/15.
 */
public class AuthorViewHolder extends ViewHolder<Author> {
    @Bind(R.id.avatar) ImageView avatar;
    @Bind(R.id.name) TextView name;
    @Bind(R.id.email) TextView email;

    public AuthorViewHolder(View v) {
        super(v);
    }

    public AuthorViewHolder(Activity a) {
        super(a);
    }

    @Override
    protected void draw(Author author, View parent) {
        ButterKnife.bind(this, parent);
        name.setText(author.getName());
        email.setText(author.getEmail());

        avatar.setImageResource(R.drawable.ic_keyboard_control);
        author.getAvatarBitmap(new Dao.SingleQueryCallback<Bitmap>() {
            @Override
            public void onResult(@Nullable Bitmap result) {
                avatar.setImageBitmap(result);
            }
        });
    }

    @Override
    protected void onNoItem(View parent) {
        ButterKnife.bind(this, parent);
        avatar.setImageResource(R.drawable.ic_not_interested);
        name.setText("Author not found");
        email.setText(null);
    }
}
