package audio.rabid.dev.sampleapp.controllers.author;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.TextView;

import java.net.URL;

import audio.rabid.dev.network_orm.Dao;
import audio.rabid.dev.sampleapp.R;
import audio.rabid.dev.sampleapp.models.Author;
import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class EditAuthorActivity extends AppCompatActivity {

    public static final String EXTRA_AUTHOR_ID = "EXTRA_AUTHOR_ID";

    private ViewHolder viewHolder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_author);
        ButterKnife.bind(this);

        int authorId = getIntent().getIntExtra(EXTRA_AUTHOR_ID, -1);

        if(authorId == -1){
            new ViewHolder(new Author(), true);
        }else{
            Author.Dao.findByLocalId(authorId, new Dao.SingleQueryCallback<Author>() {
                @Override
                public void onResult(@Nullable Author result) {
                    if(result==null){
                        viewHolder = new ViewHolder(new Author(), true);
                    }else {
                        viewHolder = new ViewHolder(result, false);
                    }
                }
            });
        }
    }

    @Override
    public boolean onCreateOptionsMenu (Menu menu){
        super.onCreateOptionsMenu(menu);
        menu.add("Delete").setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                if(viewHolder == null || viewHolder.isNew){
                    finish();
                    return false;
                }else{
                    viewHolder.delete();
                    return true;
                }
            }
        });
        return true;
    }

    public static void edit(Context context, int authorId){
        Intent i  = new Intent(context, EditAuthorActivity.class);
        i.putExtra(EXTRA_AUTHOR_ID, authorId);
        context.startActivity(i);
    }

    public static void add(Context context){
        Intent i  = new Intent(context, EditAuthorActivity.class);
        context.startActivity(i);
    }

    protected class ViewHolder {
        @Bind(R.id.name) EditText name;
        @Bind(R.id.email) EditText email;
        @Bind(R.id.avatar) EditText avatar;

        @Bind(R.id.title) TextView title;

        Author author;

        boolean isNew;

        public ViewHolder(Author author, boolean isNew){
            ButterKnife.bind(this, EditAuthorActivity.this);
            this.author = author;
            this.isNew = isNew;
            if(isNew){
                title.setText("Add Author");
            }else{
                title.setText("Edit Author "+author.getId());
                name.setText(author.getName());
                email.setText(author.getEmail());
                URL a = author.getAvatar();
                if(a!=null){
                    avatar.setText(a.toExternalForm());
                }
            }
        }

        @OnClick(R.id.submit)
        public void submit() {
            String n = name.getText().toString();
            if(n.isEmpty()){
                name.setError("Field required");
                return;
            }
            String e = email.getText().toString();
            if(e.isEmpty()){
                email.setError("Field required");
                return;
            }
            String a = avatar.getText().toString();

            synchronized (author.Lock) {
                author.setName(n);
                author.setEmail(e);
                author.setAvatar(a);
                author.save(new Dao.SingleQueryCallback<Author>() {
                    @Override
                    public void onResult(@Nullable Author result) {
                        AuthorActivity.open(EditAuthorActivity.this, author.getId());
                        finish();
                    }
                });
            }
        }

        public void delete(){
            new AlertDialog.Builder(EditAuthorActivity.this)
                    .setMessage("Are you sure you want to delete " + author.getName() + "?")
                    .setCancelable(true)
                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            author.delete(new Dao.SingleQueryCallback<Author>() {
                                @Override
                                public void onResult(@Nullable Author result) {
                                    finish();
                                }
                            });
                        }
                    }).create().show();
        }
    }
}
