package audio.rabid.dev.roe.models.network;

/**
 * Created by  charles  on 3/30/16.
 *
 * HTTP methods
 */
public enum  Method {
    GET("GET"),
    PUT("PUT"),
    POST("POST"),
    PATCH("PATCH"),
    DELETE("DELETE");

    private String method;
    Method(String method){
        this.method = method;
    }

    @Override
    public String toString(){
        return method;
    }
}
