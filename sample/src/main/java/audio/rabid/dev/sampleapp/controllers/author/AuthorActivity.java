package audio.rabid.dev.sampleapp.controllers.author;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ListView;

import org.jdeferred.DoneCallback;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import audio.rabid.dev.roe.views.EasyArrayAdapter;
import audio.rabid.dev.sampleapp.Database;
import audio.rabid.dev.sampleapp.R;
import audio.rabid.dev.sampleapp.controllers.posts.PostActivity;
import audio.rabid.dev.sampleapp.controllers.posts.PostsActivity;
import audio.rabid.dev.sampleapp.models.Author;
import audio.rabid.dev.sampleapp.models.Post;
import audio.rabid.dev.sampleapp.views.AuthorViewHolder;
import audio.rabid.dev.sampleapp.views.PostViewHolder;
import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class AuthorActivity extends AppCompatActivity {

    public static final String EXTRA_AUTHOR_ID = "AUTHOR_ID";

    @Bind(R.id.recentPosts)
    ListView recentPosts;

    private AuthorViewHolder view;

    private Author author;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_author);
        ButterKnife.bind(this);

        view = new AuthorViewHolder(this);

        final int authorId = getIntent().getIntExtra(EXTRA_AUTHOR_ID, -1);

        Database.getInstance().find(Author.class, authorId).then(new DoneCallback<Author>() {
            @Override
            public void onDone(Author result) {
                view.setItem(result);
                author = result;
            }
        });

        Map<String, String> query = new HashMap<>();
        query.put("author_id", String.valueOf(authorId));
        query.put("order_by", "created_at");
        query.put("per_page", "5");
        Database.getInstance().query(Post.class, query).then(new DoneCallback<List<Post>>() {
            @Override
            public void onDone(List<Post> result) {
                recentPosts.setAdapter(new RecentPostsAdapter(result));
            }
        });
    }

    public static void open(Context context, @Nullable Integer id) {
        Intent i = new Intent(context, AuthorActivity.class);
        i.putExtra(AuthorActivity.EXTRA_AUTHOR_ID, id);
        context.startActivity(i);
    }

    @OnClick(R.id.edit_btn)
    void edit() {
        if (author != null) {
            EditAuthorActivity.edit(this, author.getId());
        }
    }

    @OnClick(R.id.posts_btn)
    void showPosts() {
        PostsActivity.openForAuthor(this, author.getId());
    }

    protected class RecentPostsAdapter extends EasyArrayAdapter<Post, PostViewHolder> {
        public RecentPostsAdapter(List<Post> results) {
            super(AuthorActivity.this, R.layout.item_post_small, results);
        }

        @Override
        protected void onDrawView(final Post object, PostViewHolder viewHolder, View parent) {
            viewHolder.setItem(object);
            parent.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    PostActivity.open(AuthorActivity.this, object.getId());
                }
            });
        }

        @Override
        protected PostViewHolder createViewHolder(View v) {
            return new PostViewHolder(v);
        }
    }
}
