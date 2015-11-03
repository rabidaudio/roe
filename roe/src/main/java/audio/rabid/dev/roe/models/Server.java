package audio.rabid.dev.roe.models;

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

    private int timeout = 10 * 1000;

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

    public abstract Response getItem(Class<?> clazz, int serverId) throws NetworkException;

    public abstract Response createItem(Class<?> clazz, JSONObject item) throws NetworkException;

    public abstract Response getItems(Class<?> clazz, JSONObject search) throws NetworkException;

    public abstract Response updateItem(Class<?> clazz, int serverId, JSONObject data) throws NetworkException;

    public abstract Response deleteItem(Class<?> clazz, int serverId) throws NetworkException;

    public abstract boolean isErrorResponse(Response response);

//    public abstract Response getItems(JSONObject searchQ)

    /**
     * @param endpoint
     * @param method
     * @param payload
     * @return
     * @throws NetworkException
     */
    public final Response request(String endpoint, Method method, @Nullable JSONObject payload) throws NetworkException {
        URL url = null;
        try {
            if (payload != null && method == Method.GET) {
                url = buildQueryString(rootURL+endpoint, payload);
            } else {
                url = new URL(rootURL + endpoint);
            }

            HttpURLConnection connection = (HttpURLConnection) url.openConnection();

            connection.setRequestMethod(method.toString());
            connection.setConnectTimeout(timeout);

            onBeforeConnection(connection, payload);

            if (payload != null) {
                switch (method) {
                    case PUT:
                    case PATCH:
                    case POST:
                        PrintStream ps = new PrintStream(connection.getOutputStream());
                        ps.println(payload.toString());
                        ps.flush();
                        break;
                }
            }

//            connection.setReadTimeout(timeout);
            int responseCode = connection.getResponseCode();


            BufferedReader streamReader;

            if (responseCode / 100 == 2) {
                streamReader = new BufferedReader(new InputStreamReader(connection.getInputStream(), "UTF-8"));
            } else {
                streamReader = new BufferedReader(new InputStreamReader(connection.getErrorStream(), "UTF-8"));
            }

            StringBuilder result = new StringBuilder();
            String inputStr;
            while ((inputStr = streamReader.readLine()) != null)
                result.append(inputStr);

            Response r = new Response(responseCode, new JSONObject(result.toString()), connection.getHeaderFields());
            onResponse(url, r);
            return r;

        } catch (MalformedURLException | ProtocolException e) {
            throw new IllegalArgumentException(e);
        } catch (UnsupportedEncodingException e) {
            // who doesn't support UTF-8??
            throw new RuntimeException(e);
        } catch (Exception e) {
            onResponse(url, null);
            throw new NetworkException(e);
        }
    }

    protected static URL buildQueryString(String url, JSONObject query) throws URISyntaxException, JSONException, MalformedURLException {
        if(query==null){
            return new URL(url);
        }
        Uri.Builder builder = Uri.parse(url).buildUpon();
        Iterator<String> keys = query.keys();
        while (keys.hasNext()) {
            String key = keys.next();
            Object object = query.get(key);
            List<Pair<String, String>> params = new ArrayList<>();
            jsonToMap(key, object, params);
            for(Pair<String,String> entry : params){
                builder.appendQueryParameter(entry.first, entry.second);
            }
        }
        return new URL(builder.build().toString());
    }

    /**
     * Recursively create key-value pairs from JSON
     */
    private static void jsonToMap(String key, Object object, List<Pair<String, String>> currentMap) throws JSONException {
        if (object instanceof JSONArray) {
            for (int i = 0; i < ((JSONArray) object).length(); i++) {
                jsonToMap(key + "[]", ((JSONArray) object).get(i), currentMap);
            }
        } else if (object instanceof JSONObject) {
            Iterator<String> nestedKeys = ((JSONObject) object).keys();
            while (nestedKeys.hasNext()) {
                String nestedKey = nestedKeys.next();
                jsonToMap(key + "[" + nestedKey + "]", ((JSONObject) object).get(nestedKey), currentMap);
            }
        } else {
            currentMap.add(new Pair<>(key, object==null ? "null" : String.valueOf(object)));
        }
    }


    /**
     * An object containing the response to a request.
     */
    public static class Response {
        private int responseCode;
        private JSONObject responseBody;
        private Map<String, List<String>> headers;

        public Response(int code, JSONObject body, Map<String, List<String>> headers) {
            responseCode = code;
            responseBody = body;
            this.headers = headers;
        }

        public int getResponseCode() {
            return responseCode;
        }

        public JSONObject getResponseBody() {
            return responseBody;
        }

        public Map<String, List<String>> getHeaders() {
            return headers;
        }
    }

    public static class NetworkException extends Exception {
        public NetworkException(Throwable e) {
            super(e);
        }
    }
}
