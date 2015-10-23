package audio.rabid.dev.sampleapp.views.author;

import android.content.Context;
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

public class AuthorActivity extends AppCompatActivity {

    public static final String EXTRA_AUTHOR_ID = ".AUTHOR_ID";

    @Bind(R.id.avatar) ImageView avatar;
    @Bind(R.id.name) TextView name;
    @Bind(R.id.email) TextView email;

    @Bind(R.id.edit_btn) Button edit;
    @Bind(R.id.delete_btn) Button delete;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_author);
        ButterKnife.bind(this);

        int authorId = getIntent().getIntExtra(EXTRA_AUTHOR_ID, -1);

        Author.Dao.findByLocalId(authorId, new Dao.SingleQueryCallback<Author>() {
            @Override
            public void onResult(@Nullable Author result) {
                updateView(result);
            }
        });
    }

    private void updateView(final Author author){
        name.setText(author.getName());
        email.setText(author.getEmail());
        if(author.getEmail()!=null) {
            email.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    sendEmail(author);
                }
            });
        }
        avatar.setImageResource(R.drawable.ic_keyboard_control);
        author.getAvatarBitmap(new Dao.SingleQueryCallback<Bitmap>() {
            @Override
            public void onResult(@Nullable Bitmap result) {
                avatar.setImageBitmap(result);
            }
        });
        edit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                EditAuthorActivity.edit(AuthorActivity.this, author.getId());
                finish();
            }
        });
        delete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                author.delete(new Dao.SingleQueryCallback<Author>() {
                    @Override
                    public void onResult(@Nullable Author result) {
                        finish(); //close page
                    }
                });
            }
        });
    }

    private void sendEmail(Author author){
        Intent i = new Intent(Intent.ACTION_SENDTO, Uri.fromParts("mailto", author.getEmail(), null));
        i.putExtra(Intent.EXTRA_EMAIL, new String[]{author.getEmail()});
        startActivity(Intent.createChooser(i, "Contact the author"));
    }

    public static void open(Context context, int id){
        Intent i = new Intent(context, AuthorActivity.class);
        i.putExtra(AuthorActivity.EXTRA_AUTHOR_ID, id);
        context.startActivity(i);
    }
}
