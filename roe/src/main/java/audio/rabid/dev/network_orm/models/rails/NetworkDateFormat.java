package audio.rabid.dev.network_orm.models.rails;

import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Created by charles on 10/25/15.
 */
public class NetworkDateFormat extends SimpleDateFormat {

    public NetworkDateFormat() {
        super("yyyy-MM-dd'T'HH:mm:ss.SSS", Locale.US);
        setTimeZone(TimeZone.getTimeZone("GMT"));
    }
}
