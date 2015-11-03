package audio.rabid.dev.network_orm.models;

import org.json.JSONObject;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Created by charles on 11/2/15.
 *
 * Use this annotation for any field you want to be included in the mapping between {@link Resource}s
 * and {@link org.json.JSONObject}s.
 *
 * @see Resource#toJSON()
 * @see Resource#updateFromJSON(JSONObject)
 */
@Target(FIELD)
@Retention(RUNTIME)
public @interface JSONField {

    /**
     * Control whether the field be included when creating JSON representation of the resource
     */
    boolean export() default true;

    /**
     * Control whether the field should be updated when populating fields from JSON
     */
    boolean accept() default true;

    /**
     * The name of the key to use for the field. Defaults to the field name.
     */
    String key() default "";
}
