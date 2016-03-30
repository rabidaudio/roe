package audio.rabid.dev.roe.models;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.Callable;

import audio.rabid.dev.roe.Method;

/**
 * Created by  charles  on 3/15/16.
 */
public class RequestOperation implements Callable<RequestOperation.Response> {

    private URL url;
    private Method method;
    private int timeout = 10 * 1000;
    private JSONObject payload;

    public RequestOperation(Method method, URL url, JSONObject params){
        this.method = method;
        this.url = url;
        this.payload = params;
    }

    public int getTimeout() {
        return timeout;
    }

    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    @Override
    public Response call() throws Exception {
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod(method.toString());
        onBeforeConnect(connection);

        connection.setConnectTimeout(timeout);

        // If there's a payload, send it
        if (payload != null) {
            if(method == Method.GET){
                // TODO convert
            }else {
                PrintStream ps = new PrintStream(connection.getOutputStream());
                ps.println(payload.toString());
                ps.flush();
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

        return new Response(responseCode, result.toString());
    }

    public void onBeforeConnect(HttpURLConnection connection){
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setRequestProperty("Accept", "application/json");
    }

    public class Response {

        private int responseCode;
        private String data;

        public Response(int responseCode, String data){
            this.responseCode = responseCode;
            this.data = data;
        }

        public int getResponseCode() {
            return responseCode;
        }

        public String getData() {
            return data;
        }
    }
}