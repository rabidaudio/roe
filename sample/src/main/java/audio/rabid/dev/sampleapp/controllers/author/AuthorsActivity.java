package audio.rabid.dev.sampleapp.controllers.author;

import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.ListView;

import java.util.List;

import audio.rabid.dev.roe.views.EasyArrayAdapter;
import audio.rabid.dev.sampleapp.R;
import audio.rabid.dev.sampleapp.controllers.posts.PostsActivity;
import audio.rabid.dev.sampleapp.models.Author;
import audio.rabid.dev.sampleapp.views.AuthorViewHolder;
import butterknife.Bind;
import butterknife.ButterKnife;

public class AuthorsActivity extends AppCompatActivity implements SwipeRefreshLayout.OnRefreshListener {

    @Bind(R.id.authors)
    ListView authors;
    @Bind(R.id.refreshLayout)
    SwipeRefreshLayout refreshLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_authors);
        ButterKnife.bind(this);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                EditAuthorActivity.add(AuthorsActivity.this);
            }
        });
        refreshLayout.setOnRefreshListener(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateAuthors();
    }

    @Override
    public void onRefresh() {
        updateAuthors();
    }

    private void updateAuthors() {
        refreshLayout.setRefreshing(true);
        final long start = System.nanoTime();

        //TODO
//        Author.Source.getAllLocal(new Source.OperationCallback<List<Author>>() {
//            @Override
//            public void onResult(@Nullable List<Author> results) {
//                Log.d("q", "query time ms: " + (System.nanoTime() - start) / 1000f / 1000f);
//                refreshLayout.setRefreshing(false);
//                authors.setAdapter(new AuthorAdapter(AuthorsActivity.this, results));
//            }
//        });
    }

    public void open(Author author) {
        PostsActivity.openForAuthor(this, author.getId());
    }

    public void openMenu(final Author author) {
        new AlertDialog.Builder(this)
                .setItems(new String[]{getString(R.string.open), getString(R.string.edit), getString(R.string.email)},
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                switch (which) {
                                    case 0: //open
                                        AuthorActivity.open(AuthorsActivity.this, author.getId());
                                        break;
                                    case 1: //edit
                                        EditAuthorActivity.edit(AuthorsActivity.this, author.getId());
                                        break;
                                    case 2:
                                        author.sendEmail(AuthorsActivity.this);
                                        break;
                                }
                            }
                        }
                ).create().show();
    }

    protected class AuthorAdapter extends EasyArrayAdapter<Author, AuthorViewHolder> {

        public AuthorAdapter(Context context, @Nullable List<Author> authors) {
            super(context, R.layout.item_author, authors);
        }

        @Override
        protected void onDrawView(final Author author, AuthorViewHolder viewHolder, final View parent) {

            viewHolder.setItem(author);

            parent.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    open(author);
                }
            });
            parent.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    openMenu(author);
                    return true;
                }
            });
        }

        @Override
        protected AuthorViewHolder createViewHolder(View v) {
            return new AuthorViewHolder(v);
        }
    }
}
