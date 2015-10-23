package audio.rabid.dev.sampleapp;

import audio.rabid.dev.sampleapp.backend.Database;

/**
 * Created by charles on 10/23/15.
 */
public class Application extends android.app.Application {

    public Application(){
        super();
        Database.getInstance(this); //initialize database
    }
}
