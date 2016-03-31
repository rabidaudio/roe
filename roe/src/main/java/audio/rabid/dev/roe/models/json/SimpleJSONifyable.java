package audio.rabid.dev.roe.models.json;

import android.util.Pair;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Created by charles on 11/11/15.
 *
 * Tries to serialize and deserialize itself using reflection and the {@link JSONField} annotations.
 *
 * Any field you want to be included must be annotated with {@link JSONField}. It includes options for
 * configuring serialization and deserialization. Note that while simple data types like numbers and
 *  strings should work okay, more complicated data types may not. It can handle nested arrays and
 *  nested JSONifyables, but it can only handle Lists of one depth, and you must specify the data type
 *  contained in the List using {@link JSONField#childType()}, or else your list will be full of JSONObjects.
 *  Arrays end up getting copied twice on deserialization. Because JSON only stores numbers ints or
 *  doubles, you might prefer to use those types instead of shorts, floats, and bytes where possible.
 *  If you want extended capability (or you want increased performance, as reflection can be slow
 *  particularly on older devices), use your own implementation of {@link JSONifyable} instead.
 */
public abstract class SimpleJSONifyable implements JSONifyable {

    /**
     * The format used by {@link Date#toString()}.
     */
    private static SimpleDateFormat defaultDateFormat = new SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy", Locale.US);

    /**
     * A cache of the relevant fields of the class, indexed by their JSON key
     */
    private Map<String, Pair<JSONField, Field>> allFields = null;

    /**
     * Used for populating {@link #allFields}. Includes fields that are private!
     */
    private Map<String, Pair<JSONField, Field>> getAllFields(){
        if(allFields == null){
            allFields = new HashMap<>();
            for (Class<?> classWalk = getClass(); classWalk != null; classWalk = classWalk.getSuperclass()) {
                for (Field field : classWalk.getDeclaredFields()) {
                    JSONField jsonField = field.getAnnotation(JSONField.class);
                    if(jsonField != null) {
                        String key = jsonField.key();
                        if(key.isEmpty()){
                            key = field.getName();
                        }
                        allFields.put(key, new Pair<>(jsonField, field));
                    }
                }
            }
        }
        return allFields;
    }

    @SuppressWarnings("unchecked")
    @Override
    public synchronized JSONObject asJSON() throws JSONException {
        Map<String, Object> values = new HashMap<>(getAllFields().size());
        for(Map.Entry<String, Pair<JSONField, Field>> entry : getAllFields().entrySet()){
            JSONField jsonField = entry.getValue().first;
            Field field = entry.getValue().second;
            if(jsonField.serialize()){
                String key = entry.getKey();
                try{
                    field.setAccessible(true);
                    Object value = field.get(this);
                    if(!jsonField.serializer().equals(Void.class) && Serializer.class.isAssignableFrom(jsonField.serializer())){
                        Serializer s = (Serializer) jsonField.serializer().newInstance();
                        values.put(key, s.serialize(value));
                    }else if(Date.class.isAssignableFrom(field.getType()) && !jsonField.dateFormat().isEmpty()) {
                            values.put(key, new SimpleDateFormat(jsonField.dateFormat(), Locale.US).format(value));
                    }else{
                        values.put(key, serializeRaw(value, field.getType()));
                    }
                }catch (Exception e){
                    JSONException je = new JSONException("Unable to convert key "+key+" using reflection. " +
                            "Please override toJSON() for "+getClass().getSimpleName());
                    je.initCause(e);
                    throw je;
                }finally {
                    field.setAccessible(false);
                }
            }
        }
        return new JSONObject(values);
    }

    private Object serializeRaw(Object value, Class fClass) throws Exception {
        if(value == null) {
            return JSONObject.NULL;
        }else if(JSONifyable.class.isAssignableFrom(fClass)) {
            return ((JSONifyable) value).asJSON();
        }else if(List.class.isAssignableFrom(fClass)) {
            JSONArray a = new JSONArray();
            for (Object o : (List) value) {
                a.put(serializeRaw(o, o.getClass()));
            }
            return a;
        }else if(Object[].class.isAssignableFrom(fClass)) {
            JSONArray a = new JSONArray();
            for (Object o : (Object[]) value) {
                a.put(serializeRaw(o, o.getClass()));
            }
            return a;
        }else if(Date.class.isAssignableFrom(fClass)) {
            return defaultDateFormat.format(value);
        }else{
            return value;
        }
    }

    @Override
    public synchronized void fromJSON(JSONObject object) throws JSONException {
        for(Map.Entry<String, Pair<JSONField, Field>> entry : getAllFields().entrySet()){
            JSONField jsonField = entry.getValue().first;
            if(jsonField.deserialize()){
                String key = entry.getKey();
                if(object.has(key)){
                    Field field = entry.getValue().second;
                    try{
                        field.setAccessible(true);
                        Object value = object.get(key);
                        if(!jsonField.serializer().equals(Void.class) && Serializer.class.isAssignableFrom(jsonField.serializer())) {
                            Serializer s = (Serializer) jsonField.serializer().newInstance();
                            field.set(this, s.deserialize(value));
                        }else if(Date.class.isAssignableFrom(field.getType()) && value instanceof String && !jsonField.dateFormat().isEmpty()) {
                            field.set(this, new SimpleDateFormat(jsonField.dateFormat(), Locale.US).parse((String) value));
                        }else {
                            field.set(this, deserializeRaw(value, field.getType(), jsonField.childType(), field.get(this)));
                        }
                    }catch (Exception e){
                        JSONException je = new JSONException("Unable to convert key "+key+" using reflection. " +
                                "Please override fromJSON() for "+getClass().getSimpleName());
                        je.initCause(e);
                        throw je;
                    }finally {
                        field.setAccessible(false);
                    }
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    private Object deserializeRaw(Object value, Class fClass, Class child, Object current) throws Exception {
        if( value.equals(JSONObject.NULL)) {
            return null;
        }else if(JSONifyable.class.isAssignableFrom(fClass)){
            JSONifyable o = (JSONifyable) fClass.newInstance();
            o.fromJSON((JSONObject) value);
            return o;
        }else if(value instanceof JSONArray) {
            JSONArray v = (JSONArray) value;
            List l = new ArrayList(v.length());
            for (int i = 0; i < v.length(); i++) {
                Object o = v.get(i);
                if(child.equals(Object.class) && fClass.getComponentType()!= null) child = fClass.getComponentType();
                Class subChild = child.getComponentType();
                if(subChild==null) subChild = Object.class;
                l.add(i, deserializeRaw(o, child, subChild, null));
            }
            if (List.class.isAssignableFrom(fClass)) {
                List currentList = (List) current;
                if (currentList == null) {
                    return l;
                } else {
                    currentList.clear();
                    currentList.addAll(l);
                    return currentList;
                }
            } else {
                Object[] c = (Object[]) fClass.cast(current);
                if(c == null) c = (Object[]) Array.newInstance(child, l.size());
                l.toArray(c);
                return Arrays.copyOf(c, l.size());
            }
        }else if(Date.class.isAssignableFrom(fClass)) {
            if (value instanceof String) {
                return defaultDateFormat.parse((String) value);
            } else {
                return new Date((long) value);
            }
        }else if(fClass.isEnum()){
            return Enum.valueOf(fClass, ((String) value).toUpperCase());
        }else if(float.class.isAssignableFrom(fClass) || Float.class.isAssignableFrom(fClass)) {
            //JSONObject stores Number types as Doubles
            return new Float((double) value);
        }else if(short.class.isAssignableFrom(fClass) || Short.class.isAssignableFrom(fClass)){
            return new Short((short)(int) value); //lol
        }else if(byte.class.isAssignableFrom(fClass) || Byte.class.isAssignableFrom(fClass)){
            return new Byte((byte)(int) value);
        }else {
            return value;//fClass.cast(value);
        }
    }
}
