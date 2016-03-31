package audio.rabid.dev.roe.models.network;

import org.json.JSONObject;

import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Created by  charles  on 3/30/16.
 */
public class Response {
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
        return String.format(Locale.US, "[%d]: %s", responseCode, responseBody == null ? "null" : responseBody.toString());
    }
}
