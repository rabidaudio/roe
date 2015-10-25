package audio.rabid.dev.sampleapp.controllers.posts;

import android.app.SearchManager;
import android.app.SearchableInfo;
import android.content.Context;
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
import audio.rabid.dev.sampleapp.models.Post;
import audio.rabid.dev.sampleapp.views.AuthorViewHolder;
import butterknife.Bind;
import butterknife.ButterKnife;

public class EditPostActivity extends AppCompatActivity {

    public static final String EXTRA_POST_ID = "EXTRA_POST_ID";

    @Bind(R.id.title) EditText title;
    @Bind(R.id.body) EditText body;

    @Bind(R.id.searchAuthor) SearchView searchAuthor;
    @Bind(R.id.submit) Button submit;

    AuthorViewHolder authorViewHolder;

    Post post;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_post);
        ButterKnife.bind(this);
        authorViewHolder = new AuthorViewHolder(findViewById(R.id.author));

        final int postId = getIntent().getIntExtra(EXTRA_POST_ID, -1);
        if(postId == -1){
            post = new Post();
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
}
