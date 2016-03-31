package audio.rabid.roe.abstractquery;

import android.annotation.TargetApi;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Parcelable;
import android.support.annotation.Nullable;
import android.util.Size;
import android.util.SizeF;
import android.util.SparseArray;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by  charles  on 3/30/16.
 */
public class Util {

    public static JSONObject bundleToJSON(Bundle bundle) throws JSONException {
        JSONObject o = new JSONObject();
        for(String key : bundle.keySet()){
            Object val = bundle.get(key);

            //Objects
            if(val instanceof Bundle) {
                o.put(key, bundleToJSON((Bundle) val));

            //Arrays: boolean, long, int, double, or Object (String, Bundle, other)
            }else if(val instanceof String[]) {
                JSONArray a = new JSONArray();
                for(String s : (String[]) val){
                    a.put(s);
                }
                o.put(key, a);
            }else if(val instanceof Bundle[]){
                JSONArray a = new JSONArray();
                for(Bundle oo : (Bundle[]) val){
                    a.put(bundleToJSON(oo));
                }
                o.put(key, a);

            }else if(val instanceof int[]) {
                JSONArray a = new JSONArray();
                for(int s : (int[]) val){
                    a.put(s);
                }
                o.put(key, a);
            }else if(val instanceof double[]) {
                JSONArray a = new JSONArray();
                for (double s : (double[]) val) {
                    a.put(s);
                }
                o.put(key, a);
            }else if(val instanceof boolean[]){
                JSONArray a = new JSONArray();
                for(boolean s : (boolean[]) val){
                    a.put(s);
                }
                o.put(key, a);
            }else if(val instanceof char[]) {
                JSONArray a = new JSONArray();
                for (char s : (char[]) val) {
                    a.put(s);
                }
                o.put(key, a);
            }else if(val instanceof float[]) {
                JSONArray a = new JSONArray();
                for (float s : (float[]) val) {
                    a.put(s);
                }
                o.put(key, a);
            }else if(val instanceof byte[]) {
                JSONArray a = new JSONArray();
                for (byte s : (byte[]) val) {
                    a.put(s);
                }
                o.put(key, a);
            }else if(val instanceof Object[]){
                JSONArray a = new JSONArray();
                for(Object s : (Object[]) val){
                    a.put(s);
                }
                o.put(key, a);
            }else if(val instanceof List){
                JSONArray a = new JSONArray();
                for(Object s : (List) val){
                    if(s instanceof Bundle){
                        a.put(bundleToJSON((Bundle) s));
                    }else {
                        a.put(s);
                    }
                }
                o.put(key, a);

            // everything else
            }else {
                o.put(key, val);
            }
        }
        return o;
    }

//    public static Bundle jsonObjectToBundle(JSONObject object){
//
//    }
}
