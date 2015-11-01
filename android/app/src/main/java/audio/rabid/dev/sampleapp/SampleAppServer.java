package audio.rabid.dev.sampleapp;

import android.support.annotation.Nullable;
import android.util.Log;

import org.json.JSONObject;

import java.net.HttpURLConnection;
import java.net.URL;

import audio.rabid.dev.network_orm.models.rails.RailsServer;

/**
 * Created by charles on 10/28/15.
 */
public class SampleAppServer extends RailsServer {

    private static final String ROOT = "http://4786e086.ngrok.io/";

    private static SampleAppServer instance = new SampleAppServer();

    public static SampleAppServer getInstance() {
        return instance;
    }

    private SampleAppServer() {
        super(ROOT);
    }

    @Override
    protected void onBeforeConnection(HttpURLConnection connection, @Nullable JSONObject payload) {
        super.onBeforeConnection(connection, payload);
        Log.d("server", "Opening connection to " + connection.getURL().toString()
                + (payload == null ? "" : "\r\t" + payload.toString()));
    }

    @Override
    protected void onResponse(@Nullable URL url, @Nullable Response response) {
        Log.d("server", String.format("%s [%d]: %s", url == null ? "null" : url.toString(),
                response == null ? 0 : response.getResponseCode(),
                response == null ? "null" : response.getResponseBody().toString()));
    }
}
