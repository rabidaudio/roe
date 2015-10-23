package audio.rabid.dev.sampleapp.views.author;

import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

import audio.rabid.dev.sampleapp.R;
import audio.rabid.dev.sampleapp.models.Author;
import audio.rabid.dev.sampleapp.backend.Dao;
import audio.rabid.dev.utils.EasyArrayAdapter;
import butterknife.Bind;
import butterknife.ButterKnife;

/**
 * Created by charles on 10/23/15.
 */
public class AuthorAdapter extends EasyArrayAdapter<Author, AuthorAdapter.AuthorHolder> {

    private AuthorAdapterCallbacks callbacks;

    public AuthorAdapter(Context context, @Nullable List<Author> authors, AuthorAdapterCallbacks callbacks){
        super(context, R.layout.item_author, authors);
        this.callbacks = callbacks;
    }

    @Override
    protected void onDrawView(final Author author, final AuthorHolder viewHolder, View parent) {
        viewHolder.name.setText(author.getName());
        viewHolder.email.setText(author.getEmail());
        viewHolder.avatar.setImageResource(R.drawable.ic_keyboard_control);
        author.getAvatarBitmap(new Dao.SingleQueryCallback<Bitmap>() {
            @Override
            public void onResult(@Nullable Bitmap result) {
                viewHolder.avatar.setImageBitmap(result);
            }
        });

        parent.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AuthorActivity.open(getContext(), author.getId());
            }
        });

        parent.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                new AlertDialog.Builder(getContext())
                        .setItems(
                                new String[]{"Open", "Edit", "Delete"},
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        switch (which){
                                            case 0: //open
                                                AuthorActivity.open(getContext(), author.getId());
                                                break;
                                            case 1: //edit
                                                EditAuthorActivity.edit(getContext(), author.getId());
                                                break;
                                            case 2:
                                                author.delete(new Dao.SingleQueryCallback<Author>() {
                                                    @Override
                                                    public void onResult(@Nullable Author result) {
                                                        callbacks.onDelete(result);
                                                    }
                                                });
                                        }
                                    }
                                }
                        )
                        .create().show();
                return true;
            }
        });
    }

    public interface AuthorAdapterCallbacks {
        void onDelete(Author author);
    }

    @Override
    protected AuthorHolder createViewHolder(View v) {
        return new AuthorHolder(v);
    }

    public static class AuthorHolder {
        @Bind(R.id.avatar) ImageView avatar;
        @Bind(R.id.name) TextView name;
        @Bind(R.id.email) TextView email;
        public AuthorHolder(View v){
            ButterKnife.bind(this, v);
        }
    }
}
