package audio.rabid.dev.sampleapp.views.author;

import android.content.Context;
import android.content.Intent;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.net.URL;

import audio.rabid.dev.sampleapp.R;
import audio.rabid.dev.sampleapp.models.Author;
import audio.rabid.dev.network_orm.Dao;
import butterknife.Bind;
import butterknife.ButterKnife;

public class EditAuthorActivity extends AppCompatActivity implements View.OnClickListener {

    public static final String EXTRA_AUTHOR_ID = "EXTRA_AUTHOR_ID";

    @Bind(R.id.name) EditText name;
    @Bind(R.id.email) EditText email;
    @Bind(R.id.avatar) EditText avatar;
    @Bind(R.id.submit) Button submit;

    @Bind(R.id.title) TextView title;

    Author author;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_author);
        ButterKnife.bind(this);

        int authorId = getIntent().getIntExtra(EXTRA_AUTHOR_ID, -1);
        if(authorId == -1){
            author = new Author();
            title.setText("Add Author");
        }else{
            Author.Dao.findByLocalId(authorId, new Dao.SingleQueryCallback<Author>() {
                @Override
                public void onResult(@Nullable Author result) {
                    if(result==null){
                        author = new Author();
                        title.setText("Add Author");
                    }else {
                        author = result;
                        name.setText(author.getName());
                        email.setText(author.getEmail());
                        URL a = author.getAvatar();
                        if(a!=null){
                            avatar.setText(a.toExternalForm());
                        }
                        title.setText("Edit Author "+author.getId());
                    }
                }
            });
        }
        submit.setOnClickListener(this);
    }


    @Override
    public void onClick(View v) {
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

    public static void edit(Context context, int authorId){
        Intent i  = new Intent(context, EditAuthorActivity.class);
        i.putExtra(EXTRA_AUTHOR_ID, authorId);
        context.startActivity(i);
    }

    public static void add(Context context){
        Intent i  = new Intent(context, EditAuthorActivity.class);
        context.startActivity(i);
    }
}
