package audio.rabid.dev.sampleapp;

import com.j256.ormlite.dao.Dao;

import java.util.concurrent.Callable;

import audio.rabid.dev.roe.BackgroundThread;
import audio.rabid.dev.sampleapp.models.Author;
import audio.rabid.dev.sampleapp.models.Post;

/**
 * Created by charles on 10/23/15.
 */
public class Application extends android.app.Application {

    public void onCreate() {
        super.onCreate();

        Database.createInstance(this); //initialize database

        Author.Source.sync(null);
        Post.Source.sync(null);
    }
}
