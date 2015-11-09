package audio.rabid.dev.roe.models;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Created by charles on 11/9/15.
 */
@Target(FIELD)
@Retention(RUNTIME)
public @interface NetworkId {

}
