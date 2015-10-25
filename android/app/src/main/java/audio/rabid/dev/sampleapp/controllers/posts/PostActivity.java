package audio.rabid.dev.sampleapp.controllers.posts;

import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;

import audio.rabid.dev.network_orm.Dao;
import audio.rabid.dev.sampleapp.R;
import audio.rabid.dev.sampleapp.models.Author;
import audio.rabid.dev.sampleapp.models.Post;
import audio.rabid.dev.sampleapp.views.PostViewHolder;

public class PostActivity extends AppCompatActivity {

    public static final String EXTRA_POST_ID = ".POST_ID";

    PostViewHolder postViewHolder = new PostViewHolder(this);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post);

        int postID = getIntent().getIntExtra(EXTRA_POST_ID, -1);
        Post.Dao.findByLocalId(postID, new Dao.SingleQueryCallback<Post>() {
            @Override
            public void onResult(@Nullable Post result) {
                postViewHolder.setItem(result);
            }
        });
    }
}
