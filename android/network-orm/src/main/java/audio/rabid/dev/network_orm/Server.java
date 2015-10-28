package audio.rabid.dev.network_orm;

import org.apache.http.client.utils.URIBuilder;
import org.jetbrains.annotations.Nullable;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.Iterator;

/**
 * Created by charles on 10/28/15.
 */
public class Server {

    String rootURL;

    int timeout = 10*1000;

    public enum Method {
        GET("GET"),
        PUT("PUT"),
        PATCH("PATCH"),
        POST("POST"),
        DELETE("DELETE");

        String s;
        private Method(String s){
            this.s = s;
        }
        public String toString(){
            return s;
        }
    }

    public Server(String rootURL){
        this.rootURL = rootURL;
    }

    public void setTimeout(int ms){
        timeout = ms;
    }

    protected void onBeforeConnection(HttpURLConnection connection){
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setRequestProperty("Accept", "application/json");
    }

    /**
     * Override this to, for example, log requests
     */
    protected void onResponse(@Nullable URL request, @Nullable Response r){
        //default: no-op
    }

    public Response index(String endpoint, @Nullable JSONObject search) throws NetworkException {
        return request(endpoint, Method.GET, search);
    }

    public Response show(String endpoint, int serverId) throws NetworkException {
        return request(endpoint+"/"+serverId, Method.GET, null);
    }

    public Response show(String endpoint, Resource item) throws NetworkException {
        return request(endpoint+""+item.getServerId(), Method.GET, null);
    }

    public Response create(String endpoint, Resource item) throws NetworkException {
        return request(endpoint, Method.POST, item.toJSON());
    }

    public Response update(String endpoint, Resource item) throws NetworkException {
        return request(endpoint+"/"+item.getServerId(), Method.PUT, item.toJSON());
    }

    public Response destroy(String endpoint, Resource item) throws NetworkException {
        return request(endpoint+"/"+item.getServerId(), Method.DELETE, null);
    }

    public final Response request(String endpoint, Method method, @Nullable JSONObject payload) throws NetworkException {
        URL url = null;
        try {
            if (payload != null && method == Method.GET) {
                URIBuilder builder = new URIBuilder(rootURL+endpoint);
                Iterator<String> keys = payload.keys();
                while (keys.hasNext()){
                    String key = keys.next();
                    Object o = payload.get(key);
                    builder.addParameter(key, String.valueOf(o));
                }
                url = builder.build().toURL();
            }else{
                url = new URL(rootURL + endpoint);
            }

            HttpURLConnection connection = (HttpURLConnection) url.openConnection();

            connection.setRequestMethod(method.toString());
            connection.setConnectTimeout(timeout);

            onBeforeConnection(connection);

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

            Response r = new Response(responseCode, new JSONObject(result.toString()));
            onResponse(url, r);
            return r;

        }catch (MalformedURLException | ProtocolException e){
            throw new IllegalArgumentException(e);
        }catch (UnsupportedEncodingException e) {
            // who doesn't support UTF-8??
            throw new RuntimeException(e);
        }catch (Exception e){
            onResponse(url, null);
            throw new NetworkException(e);
        }
    }

    public static class Response {
        private int responseCode;
        private JSONObject responseBody;
        public Response(int code, JSONObject body){
            responseCode = code;
            responseBody = body;
        }

        public int getResponseCode() {
            return responseCode;
        }

        public JSONObject getResponseBody() {
            return responseBody;
        }

        public boolean wasError(){
            return (responseCode/100 != 2) || responseBody.has("error");
        }
    }

    public static class NetworkException extends Exception {
        public NetworkException(Throwable e){
            super(e);
        }
    }
}
