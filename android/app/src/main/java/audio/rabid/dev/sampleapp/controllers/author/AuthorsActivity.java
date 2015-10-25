package audio.rabid.dev.sampleapp.controllers.author;

import android.app.SearchManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import java.sql.SQLException;
import java.util.List;

import audio.rabid.dev.network_orm.Dao;
import audio.rabid.dev.network_orm.TypedObserver;
import audio.rabid.dev.sampleapp.R;
import audio.rabid.dev.sampleapp.models.Author;
import audio.rabid.dev.sampleapp.views.AuthorViewHolder;
import audio.rabid.dev.utils.EasyArrayAdapter;
import butterknife.Bind;
import butterknife.ButterKnife;

public class AuthorsActivity extends AppCompatActivity implements SwipeRefreshLayout.OnRefreshListener {

    @Bind(R.id.authors) ListView authors;
    @Bind(R.id.refreshLayout) SwipeRefreshLayout refreshLayout;

    String query = null;

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

        Intent intent = getIntent();
        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            query = intent.getStringExtra(SearchManager.QUERY);
        }
    }

    @Override
    protected void onResume(){
        super.onResume();
        updateAuthors();
    }

    @Override
    public void onRefresh() {
        updateAuthors();
    }

    private void updateAuthors(){
        refreshLayout.setRefreshing(true);
        Author.Dao.customMultipleQuery(new Dao.CustomMultipleQuery<Author>() {
            @Override
            public List<Author> executeQuery(Dao<Author> dao) {
                if(query==null){
                    return dao.queryForAll();
                }else{
                    String search = "%"+query+"%";
                    try {
                        return dao.queryBuilder().where().like("name", search).or().like("email", search).query();
                    }catch (SQLException e){
                        throw new RuntimeException(e);
                    }
                }
            }

            @Override
            public void onResult(List<Author> results) {
                refreshLayout.setRefreshing(false);
                authors.setAdapter(new AuthorAdapter(AuthorsActivity.this, results));
            }
        });
    }

    public void open(Author author) {
        AuthorActivity.open(this, author.getId());
    }

    public void openMenu(final Author author) {
        new AlertDialog.Builder(this)
                .setItems(new String[]{"Open", "Edit", "Email"},
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

        public AuthorAdapter(Context context, @Nullable List<Author> authors){
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
