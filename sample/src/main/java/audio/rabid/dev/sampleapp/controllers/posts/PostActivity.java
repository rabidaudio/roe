package audio.rabid.dev.sampleapp.controllers.posts;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import audio.rabid.dev.roe.models.Source;
import audio.rabid.dev.sampleapp.R;
import audio.rabid.dev.sampleapp.controllers.author.AuthorActivity;
import audio.rabid.dev.sampleapp.models.Post;
import audio.rabid.dev.sampleapp.views.PostViewHolder;
import butterknife.Bind;
import butterknife.ButterKnife;

public class PostActivity extends AppCompatActivity {

    public static final String EXTRA_POST_ID = ".POST_ID";

    PostViewHolder postViewHolder;

    Post post;

    @Bind(R.id.author)
    View author;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post);
        ButterKnife.bind(this);

        postViewHolder = new PostViewHolder(this);

        int postID = getIntent().getIntExtra(EXTRA_POST_ID, -1);

        Post.Source.find(postID, new Source.OperationCallback<Post>() {
            @Override
            public void onResult(@Nullable Post result) {
                postViewHolder.setItem(result);
                post = result;
            }
        });

        author.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AuthorActivity.open(PostActivity.this, post.getAuthor().getId());
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(R.string.author_details).setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                AuthorActivity.open(PostActivity.this, post.getAuthor().getId());
                return true;
            }
        });
        menu.add(R.string.edit_post).setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                EditPostActivity.edit(PostActivity.this, post.getId());
                return true;
            }
        });
        return super.onCreateOptionsMenu(menu);
    }

    public static void open(Context context, @Nullable Integer postId) {
        Intent i = new Intent(context, PostActivity.class);
        i.putExtra(EXTRA_POST_ID, postId);
        context.startActivity(i);
    }
}
