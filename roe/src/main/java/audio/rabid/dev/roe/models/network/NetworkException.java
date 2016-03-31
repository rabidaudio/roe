package audio.rabid.dev.roe.models.network;

/**
 * Created by  charles  on 3/30/16.
 */
public class NetworkException extends Exception {
    public NetworkException(Throwable e) {
        super(e);
    }

    public NetworkException(Response failedResponse) {
        super("Error response received: " + failedResponse.toString());
    }
}
