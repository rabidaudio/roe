package audio.rabid.dev.network_orm.models.rails;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Created by charles on 11/3/15.
 */
@Target(TYPE)
@Retention(RUNTIME)
public @interface RailsResource {

    String endpoint() default "";

    String singularJSONKey();

    String pluralJSONKey();
}
