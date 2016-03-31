package audio.rabid.dev.roe.models.json;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Created by charles on 11/11/15.
 */
@Target(FIELD)
@Retention(RUNTIME)
public @interface JSONField {

    /**
     * The key to use for this element. Defaults to the field name.
     */
    String key() default "";

    /**
     * Should this field be included in the resulting JSON?
     */
    boolean serialize() default true;

    /**
     * Should this field by overwritten by JSON?
     */
    boolean deserialize() default true;

    /**
     * Supply a custom serializer which implements {@link Serializer}.
     * Must have default constructor.
     */
    Class<?> serializer() default Void.class;

    /**
     * Serialize and deserialize field using a {@link java.text.SimpleDateFormat} with this format string.
     * Only applies to {@link java.util.Date}s.
     */
    String dateFormat() default "";

    /**
     * We are unable to determine the type of items stored in a List via reflection. For primitive
     * types, we treat them as Objects and they work out okay. But for JSONifyables they end up remaining
     * as JSONObjects because we don't know how to cast them. In that case, set this field to the data
     * type your List holds.
     */
    Class<?> childType() default Object.class;
}
