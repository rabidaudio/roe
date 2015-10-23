package audio.rabid.dev.sampleapp.views.author;

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

import audio.rabid.dev.sampleapp.R;
import audio.rabid.dev.sampleapp.models.Author;
import audio.rabid.dev.network_orm.Dao;
import butterknife.Bind;
import butterknife.ButterKnife;

public class AuthorsActivity extends AppCompatActivity implements SwipeRefreshLayout.OnRefreshListener, AuthorAdapter.AuthorAdapterCallbacks {

    @Bind(R.id.authors) ListView authors;
    @Bind(R.id.refreshLayout) SwipeRefreshLayout refreshLayout;

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
        Author.Dao.all(new Dao.MultipleQueryCallback<Author>() {
            @Override
            public void onResult(@Nullable List<Author> results) {
                refreshLayout.setRefreshing(false);
                authors.setAdapter(new AuthorAdapter(AuthorsActivity.this, results, AuthorsActivity.this));
            }
        });
    }

    @Override
    public void onClick(Author author) {
        AuthorActivity.open(this, author.getId());
    }

    @Override
    public void onLongClick(final Author author) {
        new AlertDialog.Builder(this)
                .setItems(new String[]{"Open", "Edit", "Delete"},
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                switch (which){
                                    case 0: //open
                                        AuthorActivity.open(AuthorsActivity.this, author.getId());
                                        break;
                                    case 1: //edit
                                        EditAuthorActivity.edit(AuthorsActivity.this, author.getId());
                                        break;
                                    case 2: //delete
                                        confirmDelete(author);
                                        break;
                                }
                            }
                        }
                ).create().show();
    }

    private void confirmDelete(final Author author){
        new android.app.AlertDialog.Builder(this)
                .setMessage("Are you sure you want to delete "+author.getName()+"?")
                .setCancelable(true)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        author.delete(new Dao.SingleQueryCallback<Author>() {
                            @Override
                            public void onResult(@Nullable Author result) {
                                updateAuthors();
                            }
                        });
                    }
                }).create().show();
    }
}
