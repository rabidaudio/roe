package audio.rabid.dev.roe.models;

import android.net.Uri;
import android.support.annotation.NonNull;
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
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import audio.rabid.dev.roe.models.resource.Resource;

/**
 * Created by charles on 11/11/15.
 *
 * NOTE: Servers should only throw a NetworkException if the request could not complete (no access,
 * server not found, connection lost in processing, 5XX response, etc)
 *
 * It should NOT throw a NetworkException if the request succeeded but there was some problem with the
 * request (unauthorized, invalid format, duplicate items, etc), because the request will be retried
 * later with the same arguments. Your Server implementation should handle those cases.
 *
 * If you want to disallow a particular method on the network, just provide an empty implementation and
 * the framework will assume the request was completed successfully.
 */
public abstract class Server {

//    private String rootURL;

    private int timeout = 1000;

//    public Server() {
//        this.rootURL = rootURL;
//    }

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

    /**
     * A request to get a single item from the server by its server id. You should make the request
     * and return a JSONObject representation of the item to create or update with, or null if no
     * item exists.
     */
    public abstract <T extends Resource<?, SK>, SK> JSONObject getItem(Class<T> clazz, SK id) throws NetworkException;

//    public abstract <T extends Resource<?, SK>, SK> JSONObject parseGetItemResponse(Class<T> clazz, Response response) throws NetworkException;

    /**
     * A request to get a collection of items from the server using an optional set of filters. You should make the request
     * and return a list of JSONObject representations of items to create or update, or an empty list
     * items exist.
     */
    @NonNull
    public abstract <T extends Resource> List<JSONObject> getItems(Class<T> clazz, JSONObject search) throws NetworkException;

    /**
     * A request to create a single item from the server. You should make the request
     * and return a JSONObject to update the item's fields with, or null if you don't want to alter the item.
     * You should only throw a NetworkException if you want the request to be retried later.
     */
    @Nullable
    public abstract <T extends Resource> JSONObject createItem(Class<T> clazz, T item) throws NetworkException;

    /**
     * A request to update a single item from the server. You should make the request
     * and return a JSONObject to update the item's fields with, or null if you don't want to alter the item.
     * You should only throw a NetworkException if you want the request to be retried later.
     */
    @Nullable
    public abstract <T extends Resource<?, SK>, SK> JSONObject updateItem(Class<T> clazz, T item, String id) throws NetworkException;

    /**
     * A request to delete a single item from the server. If you want to disallow deleting of a resource, simply
     * return.
     * You should only throw a NetworkException if you want the request to be retried later.
     */
    public abstract <T extends Resource<?, SK>, SK> void deleteItem(Class<T> clazz, T item, String id) throws NetworkException;

    public Response request(String url, Method method, @Nullable JSONObject payload) throws NetworkException {
//        String endpoint = request.getEndpoint();
//        Method method = request.getMethod();
//        JSONObject payload = request.getPayload();
        URL urlo;
        try {
            if (payload != null && method == Method.GET) {
                urlo = buildQueryString(url, payload);
            } else {
                urlo = new URL(url);
            }
        } catch (MalformedURLException | URISyntaxException | JSONException e) {
            throw new IllegalArgumentException("Problem building URL for request", e);
        }

        try {
            HttpURLConnection connection = (HttpURLConnection) urlo.openConnection();

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

            connection.setReadTimeout(timeout);
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
            onResponse(urlo, r);
            return r;

        } catch (UnsupportedEncodingException e) {
            // who doesn't support UTF-8??
            throw new RuntimeException(e);
        } catch (Exception e) {
            onResponse(urlo, null);
            throw new NetworkException(e, url, method, payload);
        }
    }


    protected static URL buildQueryString(String url, JSONObject query) throws URISyntaxException, JSONException, MalformedURLException {
        if (query == null) {
            return new URL(url);
        }
        Uri.Builder builder = Uri.parse(url).buildUpon();
        Iterator<String> keys = query.keys();
        while (keys.hasNext()) {
            String key = keys.next();
            Object object = query.get(key);
            List<Pair<String, String>> params = new ArrayList<>();
            jsonToMap(key, object, params);
            for (Pair<String, String> entry : params) {
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
            currentMap.add(new Pair<>(key, object == null ? "null" : String.valueOf(object)));
        }
    }

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


    public static class Request {
        private String endpoint;
        private Method method;
        @Nullable
        private JSONObject payload;

        public Request(String endpoint, Method method, @Nullable JSONObject payload){
            this.endpoint = endpoint;
            this.method = method;
            this.payload = payload;
        }

        public String getEndpoint() {
            return endpoint;
        }

        public Method getMethod() {
            return method;
        }

        @Nullable
        public JSONObject getPayload() {
            return payload;
        }
    }

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

        public String toString() {
            return String.format("[%d]: %s", responseCode, responseBody == null ? "null" : responseBody.toString());
        }
    }

    public static class NetworkException extends Exception {

        public String endpoint;

        public Method method;

        @Nullable
        public JSONObject payload;

        public NetworkException(Throwable e, String endpoint, Method method, @Nullable JSONObject payload) {
            super(e);
            this.endpoint = endpoint;
            this.method = method;
            this.payload = payload;
        }
    }
}
