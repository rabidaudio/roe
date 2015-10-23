package audio.rabid.dev.sampleapp.controllers.author;

import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import java.util.List;

import audio.rabid.dev.network_orm.Dao;
import audio.rabid.dev.sampleapp.R;
import audio.rabid.dev.sampleapp.models.Author;
import audio.rabid.dev.utils.EasyArrayAdapter;
import butterknife.Bind;
import butterknife.ButterKnife;

public class AuthorsActivity extends AppCompatActivity implements SwipeRefreshLayout.OnRefreshListener {

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
                authors.setAdapter(new AuthorAdapter(AuthorsActivity.this, results));
            }
        });
    }

    public void open(Author author) {
        AuthorActivity.open(this, author.getId());
    }

    public void openMenu(final Author author) {
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

    private class AuthorAdapter extends EasyArrayAdapter<Author, AuthorAdapter.AuthorHolder> {

        public AuthorAdapter(Context context, @Nullable List<Author> authors){
            super(context, R.layout.item_author, authors);
        }

        @Override
        protected void onDrawView(final Author author, final AuthorHolder viewHolder, View parent) {
            viewHolder.name.setText(author.getName());
            viewHolder.email.setText(author.getEmail());
            viewHolder.avatar.setImageResource(R.drawable.ic_keyboard_control);
            author.getAvatarBitmap(new Dao.SingleQueryCallback<Bitmap>() {
                @Override
                public void onResult(@Nullable Bitmap result) {
                    viewHolder.avatar.setImageBitmap(result);
                }
            });
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
        protected AuthorHolder createViewHolder(View v) {
            return new AuthorHolder(v);
        }

        protected class AuthorHolder {
            @Bind(R.id.avatar) ImageView avatar;
            @Bind(R.id.name) TextView name;
            @Bind(R.id.email) TextView email;
            public AuthorHolder(View v){
                ButterKnife.bind(this, v);
            }
        }
    }
}
