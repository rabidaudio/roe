package audio.rabid.dev.sampleapp.views.author;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import audio.rabid.dev.sampleapp.R;
import audio.rabid.dev.sampleapp.models.Author;
import audio.rabid.dev.network_orm.Dao;
import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class AuthorActivity extends AppCompatActivity {

    public static final String EXTRA_AUTHOR_ID = ".AUTHOR_ID";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_author);

        int authorId = getIntent().getIntExtra(EXTRA_AUTHOR_ID, -1);

        Author.Dao.findByLocalId(authorId, new Dao.SingleQueryCallback<Author>() {
            @Override
            public void onResult(@Nullable Author result) {
                new ViewHolder(result);
            }
        });
    }

    public static void open(Context context, int id){
        Intent i = new Intent(context, AuthorActivity.class);
        i.putExtra(AuthorActivity.EXTRA_AUTHOR_ID, id);
        context.startActivity(i);
    }

    private class ViewHolder {
        @Bind(R.id.avatar) ImageView avatar;
        @Bind(R.id.name) TextView name;
        @Bind(R.id.email) TextView email;

        private Author author;

        public ViewHolder(final Author author){
            ButterKnife.bind(this, AuthorActivity.this);

            this.author = author;

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

        @OnClick(R.id.email)
        private void sendEmail(){
            author.sendEmail(AuthorActivity.this);
        }

        @OnClick(R.id.edit_btn)
        private void edit(){
            EditAuthorActivity.edit(AuthorActivity.this, author.getId());
            finish();
        }

        @OnClick(R.id.delete_btn)
        private void delete(){
            new AlertDialog.Builder(AuthorActivity.this)
                    .setMessage("Are you sure you want to delete " + author.getName() + "?")
                    .setCancelable(true)
                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            author.delete(new Dao.SingleQueryCallback<Author>() {
                                @Override
                                public void onResult(@Nullable Author result) {
                                    finish(); //close page
                                }
                            });
                        }
                    }).create().show();
        }
    }
}
