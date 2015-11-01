package audio.rabid.dev.sampleapp;

import audio.rabid.dev.sampleapp.models.Author;
import audio.rabid.dev.sampleapp.models.Post;

/**
 * Created by charles on 10/23/15.
 */
public class Application extends android.app.Application {

    public void onCreate() {
        super.onCreate();

        Database.getInstance(this); //initialize database

        Author.Source.sync(null);
        Post.Source.sync(null);
    }
}
