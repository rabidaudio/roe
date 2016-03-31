package audio.rabid.dev.roe.models.network;

import android.net.Uri;
import android.support.annotation.Nullable;
import android.util.Pair;

import org.jdeferred.Promise;
import org.jdeferred.android.AndroidDeferredManager;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
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
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

/**
 * Created by  charles  on 3/30/16.
 */
public class NetworkOperation {

    private Method method;
    private String path;
    @Nullable
    private JSONObject parameters;

    public NetworkOperation(Method method, String relativePath, @Nullable JSONObject parameters){
        this.method = method;
        this.path = relativePath;
        this.parameters = parameters;
    }

//    public Promise<Response, Throwable, Void> promise(final String rootURL, final int timeout, AndroidDeferredManager deferredManager, final RequestCallbacks callbacks){
//        return deferredManager.when(new Callable<Response>() {
//            @Override
//            public Response call() throws Exception {
//                return request(rootURL, timeout, callbacks);
//            }
//        });
//    }

    public Response request(String rootURL, int timeout, RequestCallbacks callbacks) throws Exception {
        URL url = null;
        try {
            if (parameters != null && method == Method.GET) {
                url = buildQueryString(rootURL+path, parameters);
            } else {
                url = new URL(rootURL + path);
            }

            HttpURLConnection connection = (HttpURLConnection) url.openConnection();

            connection.setRequestMethod(method.toString());
            connection.setConnectTimeout(timeout);

            callbacks.onBeforeConnection(connection, parameters);

            if (parameters != null) {
                switch (method) {
                    case PUT:
                    case PATCH:
                    case POST:
                        PrintStream ps = new PrintStream(connection.getOutputStream());
                        ps.println(parameters.toString());
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

            //            onResponse(url, r);
            return new Response(responseCode, new JSONObject(result.toString()), connection.getHeaderFields());

        } catch (MalformedURLException | ProtocolException e) {
            throw new IllegalArgumentException(e);
        } catch (UnsupportedEncodingException e) {
            // who doesn't support UTF-8??
            throw new RuntimeException(e);
        } catch (IOException e) {
//            onResponse(url, null);
            throw new NetworkException(e);
        }
    }

    public interface RequestCallbacks {
        void onBeforeConnection(HttpURLConnection connection, JSONObject payload);
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
}
