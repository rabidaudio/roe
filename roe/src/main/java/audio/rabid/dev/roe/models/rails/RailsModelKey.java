package audio.rabid.dev.roe.models.rails;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Created by charles on 11/3/15.
 */
@Target(FIELD)
@Retention(RUNTIME)
public @interface RailsModelKey {

    String endpoint() default "";

    String singularJSONKey();

    String pluralJSONKey();
}
