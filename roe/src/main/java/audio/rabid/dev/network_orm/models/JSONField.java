package audio.rabid.dev.network_orm.models;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Created by charles on 11/2/15.
 */
@Target(FIELD)
@Retention(RUNTIME)
public @interface JSONField {

    boolean export() default true;

    boolean accept() default true;

    String key() default "";
}
