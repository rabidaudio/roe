package audio.rabid.dev.roe.models.rails;

/**
 * Created by charles on 11/17/15.
 */

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Target(TYPE)
@Retention(RUNTIME)
public @interface RailsServerResource {

//    /**
//     * The name of the endpoint to use. For example, if your API root url is http://example.com/api/
//     * and you have a resource "Vehicle", then typically your pluralized resources controller would
//     * heave endpoints http://example.com/api/vehicles, http://example.com/api/vehicles:id, etc.
//     * In this case, "vehicles" is your endpoint.
//     *
//     * Defaults to the lowercase class name plus an 's.
//     */
    String endpoint();
//
//    /**
//     * RailsServer assumes all objects need to be wrapped in an object container. For example, a SHOW
//     * call to "vehicles" would respond with JSON like:
//     * {
//     *     "vehicle": {
//     *         ....
//     *     }
//     * }
//     * That way, it can include other details in the response JSON as well (status, etc). In this case,
//     * "vehicle" is your singular key. It is the key in the response JSON that actually represents the
//     * object you care about.
//     *
//     * It is used for CREATE and UPDATE requests and SHOW, CREATE, and UPDATE responses.
//     *
//     * Defaults to the lowercase class name.
//     */
//    String singularKey() default "";
//
//    /**
//     * RailsServer assumes all objects need to be wrapped in an object container. For example, an INDEX
//     * call to "vehicles" would respond with JSON like:
//     * {
//     *     "vehicles": [
//     *          { ... },
//     *          { ... }
//     *     ]
//     * }
//     * That way, it can include other details in the response JSON as well (status, etc). In this case,
//     * "vehicles" is your plural key. It is the key in the response JSON that actually represents the
//     * object you care about.
//     *
//     * It is used as for INDEX responses.
//     * Defaults to the lowercase class name plus an 's.
//     */
//    String pluralKey() default "";

    /**
     * Methods allowed to be run on the server. Defaults to all
     */
    RailsServer.Operation[] allowedOperations() default {
            RailsServer.Operation.CREATE,
            RailsServer.Operation.SHOW,
            RailsServer.Operation.INDEX,
            RailsServer.Operation.UPDATE,
            RailsServer.Operation.DELETE
    };
}
