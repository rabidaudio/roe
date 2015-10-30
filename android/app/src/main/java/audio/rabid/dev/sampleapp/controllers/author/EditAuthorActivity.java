package audio.rabid.dev.sampleapp.controllers.author;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.widget.EditText;
import android.widget.TextView;

import java.net.URL;

import audio.rabid.dev.network_orm.Source;
import audio.rabid.dev.sampleapp.R;
import audio.rabid.dev.sampleapp.models.Author;
import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * We don't use a view holder pattern here because
 */
public class EditAuthorActivity extends AppCompatActivity {

    public static final String EXTRA_AUTHOR_ID = "EXTRA_AUTHOR_ID";

    @Bind(R.id.name)
    EditText name;
    @Bind(R.id.email)
    EditText email;
    @Bind(R.id.avatar)
    EditText avatar;

    @Bind(R.id.title)
    TextView title;

    private Author author;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_author);
        ButterKnife.bind(this);

        int authorId = getIntent().getIntExtra(EXTRA_AUTHOR_ID, -1);

        if (authorId == -1) {
            drawNewAuthor();
        } else {
            Author.Source.getLocal(authorId, new Source.QueryCallback<Author>() {
                @Override
                public void onResult(@org.jetbrains.annotations.Nullable Author result) {
                    if (result == null) {
                        drawNewAuthor();
                    } else {
                        author = result;
                        drawExisting();
                    }
                }
            });
        }
    }

    public static void edit(Context context, int authorId) {
        Intent i = new Intent(context, EditAuthorActivity.class);
        i.putExtra(EXTRA_AUTHOR_ID, authorId);
        context.startActivity(i);
    }

    public static void add(Context context) {
        Intent i = new Intent(context, EditAuthorActivity.class);
        context.startActivity(i);
    }

    private void drawNewAuthor() {
        author = new Author();
        title.setText("Add Author");
    }

    private void drawExisting() {
        title.setText("Edit Author " + author.getId());
        name.setText(author.getName());
        email.setText(author.getEmail());
        URL a = author.getAvatar();
        if (a != null) {
            avatar.setText(a.toExternalForm());
        }
    }

    @OnClick(R.id.submit)
    public void submit() {
        String n = name.getText().toString();
        if (n.isEmpty()) {
            name.setError("Field required");
            return;
        }
        String e = email.getText().toString();
        if (e.isEmpty()) {
            email.setError("Field required");
            return;
        }
        String a = avatar.getText().toString();

        synchronized (author.Lock) {
            author.setName(n);
            author.setEmail(e);
            author.setAvatar(a);
            author.save(new Source.QueryCallback<Author>() {
                @Override
                public void onResult(@Nullable Author result) {
                    setResult(Activity.RESULT_OK);
                    finish();
                }
            });
        }
    }
}
