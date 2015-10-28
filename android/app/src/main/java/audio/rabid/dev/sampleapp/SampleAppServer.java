package audio.rabid.dev.sampleapp;

import java.net.HttpURLConnection;

import audio.rabid.dev.network_orm.Server;

/**
 * Created by charles on 10/28/15.
 */
public class SampleAppServer extends Server {

    private static final String ROOT = "http://asdf.com/";

    private static SampleAppServer instance = new SampleAppServer();

    public static SampleAppServer getInstance(){
        return instance;
    }

    private SampleAppServer() {
        super(ROOT);
    }
}
