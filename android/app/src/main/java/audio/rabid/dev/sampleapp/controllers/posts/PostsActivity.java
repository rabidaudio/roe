package audio.rabid.dev.sampleapp.controllers.posts;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;

import java.util.List;

import audio.rabid.dev.network_orm.Source;
import audio.rabid.dev.sampleapp.R;
import audio.rabid.dev.sampleapp.controllers.author.AuthorActivity;
import audio.rabid.dev.sampleapp.controllers.author.AuthorsActivity;
import audio.rabid.dev.sampleapp.models.Author;
import audio.rabid.dev.sampleapp.models.Post;
import audio.rabid.dev.sampleapp.views.AuthorViewHolder;
import audio.rabid.dev.sampleapp.views.PostViewHolder;
import audio.rabid.dev.utils.EasyArrayAdapter;
import butterknife.Bind;
import butterknife.ButterKnife;

public class PostsActivity extends AppCompatActivity implements SwipeRefreshLayout.OnRefreshListener{

    public static final String EXTRA_AUTHOR_ID = "EXTRA_AUTHOR_ID";

    @Bind(R.id.posts) ListView posts;
    @Bind(R.id.refreshLayout) SwipeRefreshLayout refreshLayout;
    @Bind(R.id.author) View author;

    AuthorViewHolder authorViewHolder;

    int authorId = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_posts);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ButterKnife.bind(this);
        authorViewHolder = new AuthorViewHolder(this);

        authorId = getIntent().getIntExtra(EXTRA_AUTHOR_ID, -1);
        if(authorId == -1) {
            author.setVisibility(View.GONE);
        }else {
            Author.Source.getLocal(authorId, new Source.QueryCallback<Author>() {
                @Override
                public void onResult(Author result) {
                    authorViewHolder.setItem(result);
                }
            });
        }

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                EditPostActivity.newPost(PostsActivity.this, authorId);
            }
        });

        refreshLayout.setOnRefreshListener(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu){
        menu.add("Authors").setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                PostsActivity.this.startActivity(new Intent(PostsActivity.this, AuthorsActivity.class));
                return true;
            }
        });
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    protected void onResume(){
        super.onResume();
        updateList();
    }

    @Override
    public void onRefresh() {
        updateList();
    }

    void updateList(){
        refreshLayout.setRefreshing(true);
        final long start = System.nanoTime();

        Post.Source.allByAuthorOrAll(authorId, new Source.QueryCallback<List<Post>>() {
            @Override
            public void onResult(@org.jetbrains.annotations.Nullable List<Post> result) {
                Log.d("q", "query time ms: " + (System.nanoTime() - start) / 1000f / 1000f);
                refreshLayout.setRefreshing(false);
                posts.setAdapter(new PostAdapter(result));
            }
        });
    }

    public static void openForAuthor(Context context, int authorId){
        Intent i = new Intent(context, PostsActivity.class);
        i.putExtra(EXTRA_AUTHOR_ID, authorId);
        context.startActivity(i);
    }

    void open(Post post){
        PostActivity.open(this, post.getId());
    }

    void showMenu(final Post post){
        new AlertDialog.Builder(this)
                .setItems(new String[]{"Open", "Edit", "View Author", "Delete"},
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                switch (which) {
                                    case 0: //open
                                        PostActivity.open(PostsActivity.this, post.getId());
                                        break;
                                    case 1: //edit
                                        EditPostActivity.edit(PostsActivity.this, post.getId());
                                        break;
                                    case 2: //view author
                                        AuthorActivity.open(PostsActivity.this, post.getAuthor().getId());
                                        break;
                                    case 3: //delete
                                        new AlertDialog.Builder(PostsActivity.this)
                                                .setMessage("Are you sure you want to delete this post?")
                                                .setCancelable(true)
                                                .setNegativeButton("Cancel", null)
                                                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                                    @Override
                                                    public void onClick(DialogInterface dialog, int which) {
                                                        post.delete(new Source.QueryCallback<Post>() {
                                                            @Override
                                                            public void onResult(Post result) {
                                                                finish();
                                                            }
                                                        });
                                                    }
                                                })
                                                .create().show();
                                        break;
                                }
                            }
                        }
                ).create().show();
    }

    private class PostAdapter extends EasyArrayAdapter<Post, PostViewHolder> {

        public PostAdapter(@Nullable List<Post> list) {
            super(PostsActivity.this, R.layout.item_post, list);
        }

        @Override
        protected void onDrawView(final Post object, PostViewHolder viewHolder, View parent) {
            viewHolder.setItem(object);
            parent.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    open(object);
                }
            });
            parent.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    showMenu(object);
                    return false;
                }
            });
        }

        @Override
        protected PostViewHolder createViewHolder(View v) {
            return new PostViewHolder(v);
        }
    }
}
