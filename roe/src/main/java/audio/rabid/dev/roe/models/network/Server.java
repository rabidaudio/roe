package audio.rabid.dev.roe.models.network;

import android.net.Uri;
import android.support.annotation.Nullable;
import android.util.Pair;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Created by charles on 10/28/15.
 * <p/>
 * The class that makes HTTP api calls to get data remotely (if available). {@link #request(String, Method, JSONObject)}
 * is the method that does the work. Override {@link #onBeforeConnection(HttpURLConnection, JSONObject)}
 * to set additional headers or change other connection settings before open.
 */
public abstract class Server {

    private String rootURL;

    private int timeout = 1000;

    /**
     * Available HTTP verbs
     */
    public enum Method {
        GET("GET"),
        PUT("PUT"),
        PATCH("PATCH"),
        POST("POST"),
        DELETE("DELETE");

        String s;

        Method(String s) {
            this.s = s;
        }

        public String toString() {
            return s;
        }
    }

    public Server(String rootURL) {
        this.rootURL = rootURL;
    }

    /**
     * Control the amount of time to wait for a connection or read to occur before connection fails.
     *
     * @param ms the wait time in milliseconds
     */
    public void setTimeout(int ms) {
        timeout = ms;
    }

    /**
     * Override this to set custom headers, log requests, or tweak connection settings before open.
     * Be sure to call super though!
     *
     * @param connection the HTTPUrlConnection about to be open
     * @param payload    the payload included with the request, if any. If it is a GET request, it has
     *                   been worked into the query string already. Otherwise it's about to be written.
     */
    protected void onBeforeConnection(HttpURLConnection connection, @Nullable JSONObject payload) {
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setRequestProperty("Accept", "application/json");
    }

    /**
     * Override this to, for example, log requests.
     *
     * @param url
     * @param response If a {@link NetworkException} occurs, this will be null
     */
    protected void onResponse(URL url, @Nullable Response response) {
        //default: no-op
    }

    public abstract JSONObject getItem(Class<?> clazz, String serverId) throws NetworkException;

    public abstract JSONObject createItem(Class<?> clazz, JSONObject item) throws NetworkException;

    public abstract List<JSONObject> getItems(Class<?> clazz, JSONObject search) throws NetworkException;

    public abstract JSONObject updateItem(Class<?> clazz, String serverId, JSONObject data) throws NetworkException;

    public abstract JSONObject deleteItem(Class<?> clazz, String serverId) throws NetworkException;
}
