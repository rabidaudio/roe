package audio.rabid.dev.sampleapp;

/**
 * Created by charles on 10/23/15.
 */
public class Application extends android.app.Application {

    public void onCreate() {
        super.onCreate();

        Database.createInstance(this); //initialize database

//        Author.Source.sync(null);
//        Post.Source.sync(null);
    }
}
