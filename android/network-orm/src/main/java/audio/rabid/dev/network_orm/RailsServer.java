package audio.rabid.dev.network_orm;

import org.jetbrains.annotations.Nullable;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by charles on 10/29/15.
 *
 * A server which expects standard Rails `resources` endpoints. Specifically, the pluralized endpoints
 * except for `new` and `edit`.
 *
 * E.g. `resources :posts`:
 *
 * index (return multiple posts, possibly with query parameters to filter) - GET /posts => { posts: [ {...}, ...] }
 * show (return single post by id) - GET /posts/:id => { post: {...} }
 * create (create new post) - POST /posts with { post: {...} } => { post: {...} }
 * update (update existing) - PUT/PATCH /posts/:id with { post: {...} } => { post: {...} }
 * delete (delete existing) - DELETE /posts/:id => { post: {...} }
 */
public class RailsServer extends Server {

    public RailsServer(String rootURL) {
        super(rootURL);
    }

    public Response index(String endpoint, @Nullable JSONObject search) throws NetworkException {
        return request(endpoint, Method.GET, search);
    }

    public Response show(String endpoint, int serverId) throws NetworkException {
        return request(endpoint+"/"+serverId, Method.GET, null);
    }

    public Response create(String endpoint, JSONObject data) throws NetworkException {
        return request(endpoint, Method.POST, data);
    }

    public Response update(String endpoint, int serverId, JSONObject data) throws NetworkException {
        return request(endpoint + "/" + serverId, Method.PUT, data);
    }

    public Response destroy(String endpoint, int serverId) throws NetworkException {
        return request(endpoint + "/" + serverId, Method.DELETE, null);
    }

    @Override
    public boolean isErrorResponse(Response response){
        return (response.getResponseCode()/100 != 2) || !response.getResponseBody().isNull("error");
    }
}
