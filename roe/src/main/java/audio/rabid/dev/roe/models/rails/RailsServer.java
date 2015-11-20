package audio.rabid.dev.roe.models.rails;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import audio.rabid.dev.roe.models.resource.Resource;
import audio.rabid.dev.roe.models.Server;
import audio.rabid.dev.roe.models.json.Serializer;

/**
 * Created by charles on 11/17/15.
 */
public class RailsServer extends Server {

    public enum Operation {
        SHOW,
        INDEX,
        CREATE,
        UPDATE,
        DELETE
    }

    String rootUrl;

    private Map<Class, RailsResource> resourceMap = new HashMap<>();

    public RailsServer(String rootURL) {
        this.rootUrl = rootURL;
    }

    public boolean checkPermissions(Class clazz, Operation operation){
        return getResource(clazz).getAllowedOperations().contains(operation);
    }

    public RailsResource getResource(Class clazz){
        RailsResource r = resourceMap.get(clazz);
        if(r == null){
            r = new RailsResource(clazz);
            resourceMap.put(clazz, r);
        }
        return r;
    }

    public <T extends Resource> void addResource(Class<T> tClass, RailsResource resource){
        resourceMap.put(tClass, resource);
    }

    public String getRootUrl(){
        return rootUrl;
    }

    @Override
    public <T extends Resource<?, SK>, SK> JSONObject getItem(Class<T> clazz, SK id) throws NetworkException {
        if(checkPermissions(clazz, Operation.SHOW)){
            RailsResource r = getResource(clazz);
            String url = rootUrl+r.getEndpoint()+"/"+String.valueOf(id);
            Response response = request(url, Method.GET, null);
            try{
                if(isErrorResponse(response)){
                    onNonNetworkException(new Exception(response.getResponseBody().getJSONObject("error").toString()));
                }else{
                    return response.getResponseBody().getJSONObject("data");
                }
            }catch (JSONException e){
                onNonNetworkException(e);
            }
        }
        return null;
    }

    @NonNull
    @Override
    public <T extends Resource> List<JSONObject> getItems(Class<T> clazz, JSONObject search) throws NetworkException {
        if(checkPermissions(clazz, Operation.INDEX)){
            RailsResource r = getResource(clazz);
            String url = rootUrl+r.getEndpoint();
            Response response = request(url, Method.GET, search);
            try{
                if(isErrorResponse(response)){
                    onNonNetworkException(new Exception(response.getResponseBody().getJSONObject("error").toString()));
                }else{
                    JSONObject pagination = response.getResponseBody().getJSONObject("pagination");
                    JSONArray array = response.getResponseBody().getJSONArray("data");
                    List<JSONObject> result = new ArrayList<>(array.length());
                    for(int i=0; i<array.length(); i++){
                        result.add(i, array.getJSONObject(i));
                    }
                    if(!search.has("page") && array.length() == pagination.getInt("per_page")){
                        //try and get the rest of the data
                        Response nextPage = request(pagination.getString("next_page"), Method.GET, null);
                        while(!isErrorResponse(nextPage) && !"NONE_FOUND".equals(nextPage.getResponseBody().getString("status"))){
                            pagination = nextPage.getResponseBody().getJSONObject("pagination");
                            array = nextPage.getResponseBody().getJSONArray("data");
                            for(int i=0; i<array.length(); i++){
                                result.add(i, array.getJSONObject(i));
                            }
                            nextPage = request(pagination.getString("next_page"), Method.GET, null);
                        }
                    }
                    return result;
                }
            }catch (JSONException e){
                onNonNetworkException(e);
            }
        }
        return new ArrayList<>(0);
    }

    @Nullable
    @Override
    public <T extends Resource> JSONObject createItem(Class<T> clazz, T item) throws NetworkException {
        if(checkPermissions(clazz, Operation.CREATE)){
            RailsResource r = getResource(clazz);
            String url = rootUrl+r.getEndpoint();
            try{
                Response response = request(url, Method.POST, item.asJSON());
                if(isErrorResponse(response)){
                    onNonNetworkException(new Exception(response.getResponseBody().getJSONObject("error").toString()));
                }else{
                    return response.getResponseBody().getJSONObject("data");
                }
            }catch (JSONException e){
                onNonNetworkException(e);
            }
        }
        return null;
    }

    @Nullable
    @Override
    public <T extends Resource<?,SK>, SK> JSONObject updateItem(Class<T> clazz, T item, String id) throws NetworkException {
        if(checkPermissions(clazz, Operation.UPDATE)){
            RailsResource r = getResource(clazz);
            String url = rootUrl+r.getEndpoint()+"/"+String.valueOf(id);
            try{
                Response response = request(url, Method.PUT, item.asJSON());
                if(isErrorResponse(response)){
                    onNonNetworkException(new Exception(response.getResponseBody().getJSONObject("error").toString()));
                }else{
                    return response.getResponseBody().getJSONObject("data");
                }
            }catch (JSONException e){
                onNonNetworkException(e);
            }
        }
        return null;
    }

    @Override
    public <T extends Resource<?, SK>, SK> void deleteItem(Class<T> clazz, T item, String id) throws NetworkException {
        try {
            if (checkPermissions(clazz, Operation.DELETE)) {
                RailsResource r = getResource(clazz);
                String endpoint = r.getEndpoint() + "/" + String.valueOf(id);
                Response response = request(endpoint, Method.DELETE, null);
                if (isErrorResponse(response)) {
                    onNonNetworkException(new Exception(response.getResponseBody().getJSONObject("error").toString()));
                }
            }
        }catch (JSONException e){
            onNonNetworkException(e);
        }
    }

    public boolean isErrorResponse(Response response){
        return response.getResponseCode()/100 != 2 || response.getResponseBody().has("error");
    }

    public void onNonNetworkException(Exception e){
        //override this method for custom handling
        throw new RuntimeException(e);
    }

    protected static class RailsResource {
        private String endpoint;
        private List<Operation> allowedOperations = Arrays.asList(Operation.values());

        public <T> RailsResource(Class<T> rClass){
            endpoint = rClass.getSimpleName().toLowerCase();
            //try from annotation
            RailsServerResource annotation = rClass.getAnnotation(RailsServerResource.class);
            if(annotation != null) {
                endpoint = annotation.endpoint();
                allowedOperations = Arrays.asList(annotation.allowedOperations());
            }
        }


        public List<Operation> getAllowedOperations() {
            return allowedOperations;
        }

        public String getEndpoint() {
            return endpoint;
        }
    }

    public static DateFormat getDateFormat(){
        DateFormat f = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS", Locale.US); //ISO 8601, always in GMT
        f.setTimeZone(TimeZone.getTimeZone("GMT"));
        return f;
    }

    /**
     * Set your date fields to use this as the serializer
     */
    public static class RailsDateSerializer implements Serializer<Date> {

        private DateFormat dateFormat = getDateFormat();

        @Override
        public Object serialize(Date item) throws JSONException {
            return dateFormat.format(item);
        }

        @Override
        public Date deserialize(Object object) throws JSONException {
            try {
                return dateFormat.parse((String) object);
            } catch (ParseException pe) {
                JSONException je = new JSONException("Problem parsing date");
                je.initCause(pe);
                throw je;
            }
        }
    }
}
