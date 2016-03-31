package audio.rabid.dev.sampleapp.controllers.posts;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.widget.EditText;

import org.jdeferred.DoneCallback;

import audio.rabid.dev.sampleapp.Database;
import audio.rabid.dev.sampleapp.R;
import audio.rabid.dev.sampleapp.models.Author;
import audio.rabid.dev.sampleapp.models.Post;
import audio.rabid.dev.sampleapp.views.AuthorViewHolder;
import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class EditPostActivity extends AppCompatActivity {

    public static final String EXTRA_POST_ID = "EXTRA_POST_ID";
    public static final String EXTRA_AUTHOR_ID = "EXTRA_AUTHOR_ID";

    @Bind(R.id.title)
    EditText title;
    @Bind(R.id.body)
    EditText body;

//    @Bind(R.id.searchAuthor) SearchView searchAuthor;

    AuthorViewHolder authorViewHolder;

    Post post;

    boolean isNew = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_post);
        ButterKnife.bind(this);
        authorViewHolder = new AuthorViewHolder(findViewById(R.id.author));

        final int postId = getIntent().getIntExtra(EXTRA_POST_ID, -1);
        final int authorId = getIntent().getIntExtra(EXTRA_AUTHOR_ID, -1);
        if (postId == -1) {
            isNew = true;
            post = new Post();
            Database.getInstance().show(Database.getInstance().getAuthorModel(), String.valueOf(authorId)).then(new DoneCallback<Author>() {
                @Override
                public void onDone(Author result) {
                    post.setAuthor(result);
                    authorViewHolder.setItem(result);
                }
            });
        } else {
            Database.getInstance().show(Database.getInstance().getPostModel(), String.valueOf(postId)).then(new DoneCallback<Post>() {
                @Override
                public void onDone(Post result) {
                    if (result == null) {
                        post = new Post();
                    } else {
                        post = result;

                        title.setText(post.getTitle());
                        body.setText(post.getBody());
                        authorViewHolder.setItem(post.getAuthor());
                    }
                }
            });
        }
    }

    @OnClick(R.id.submit)
    void submit() {
        String t = title.getText().toString();
        if (t.isEmpty()) {
            title.setError(getString(R.string.field_required));
            return;
        }
        String b = body.getText().toString();
        if (b.isEmpty()) {
            body.setError(getString(R.string.field_required));
            return;
        }

        post.setTitle(t);
        post.setBody(b);
        (isNew
                ? Database.getInstance().create(Database.getInstance().getPostModel(), post)
                : Database.getInstance().update(Database.getInstance().getPostModel(), String.valueOf(post.getId()), post)
        ).then(new DoneCallback<Post>() {
            @Override
            public void onDone(Post result) {
                setResult(RESULT_OK);
                finish();
            }
        });
    }

    public static void edit(Context context, @Nullable Integer postId) {
        Intent i = new Intent(context, EditPostActivity.class);
        i.putExtra(EXTRA_POST_ID, postId);
        context.startActivity(i);
    }

    public static void newPost(Context context, @Nullable Integer authorId) {
        Intent i = new Intent(context, EditPostActivity.class);
        i.putExtra(EXTRA_AUTHOR_ID, authorId);
        context.startActivity(i);
    }
}
