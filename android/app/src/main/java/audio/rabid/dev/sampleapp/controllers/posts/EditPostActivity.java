package audio.rabid.dev.sampleapp.controllers.posts;

import android.app.SearchManager;
import android.app.SearchableInfo;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CursorAdapter;
import android.widget.EditText;
import android.widget.SearchView;

import audio.rabid.dev.network_orm.Dao;
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

    @Bind(R.id.title) EditText title;
    @Bind(R.id.body) EditText body;

//    @Bind(R.id.searchAuthor) SearchView searchAuthor;

    AuthorViewHolder authorViewHolder;

    Post post;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_post);
        ButterKnife.bind(this);
        authorViewHolder = new AuthorViewHolder(findViewById(R.id.author));

        final int postId = getIntent().getIntExtra(EXTRA_POST_ID, -1);
        final int authorId = getIntent().getIntExtra(EXTRA_AUTHOR_ID, -1);
        if(postId == -1){
            post = new Post();
            Author.Dao.findByLocalId(authorId, new Dao.SingleQueryCallback<Author>() {
                @Override
                public void onResult(Author result) {
                    post.setAuthor(result);
                    authorViewHolder.setItem(result);
                }
            });
        }else{
            Post.Dao.findByLocalId(postId, new Dao.SingleQueryCallback<Post>() {
                @Override
                public void onResult(Post result) {
                    if(result==null){
                        post = new Post();
                    }else{
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
    void submit(){
        String t = title.getText().toString();
        if(t.isEmpty()){
            title.setError("Field required");
            return;
        }
        String b = body.getText().toString();
        if(b.isEmpty()){
            body.setError("Field required");
            return;
        }
        synchronized (post.Lock){
            post.setTitle(t);
            post.setBody(b);
            post.save(new Dao.SingleQueryCallback<Post>() {
                @Override
                public void onResult(Post result) {
                    setResult(RESULT_OK);
                    finish();
                }
            });
        }
    }

    public static void edit(Context context, int postId){
        Intent i = new Intent(context, EditPostActivity.class);
        i.putExtra(EXTRA_POST_ID, postId);
        context.startActivity(i);
    }

    public static void newPost(Context context, int authorId){
        Intent i = new Intent(context, EditPostActivity.class);
        i.putExtra(EXTRA_AUTHOR_ID, authorId);
        context.startActivity(i);
    }
}
