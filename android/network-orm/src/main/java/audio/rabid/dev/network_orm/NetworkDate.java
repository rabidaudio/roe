package audio.rabid.dev.network_orm;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Created by charles on 10/25/15.
 */
public class NetworkDate extends SimpleDateFormat {

    private static NetworkDate instance = new NetworkDate();

    private NetworkDate(){
        super("yyyy-MM-dd'T'HH:mm:ss.SSS", Locale.US);
        setTimeZone(TimeZone.getTimeZone("GMT"));
    }

    public static String encode(Date date){
        return instance.format(date);
    }

    public static Date decode(String date){
        try {
            return instance.parse(date);
        }catch (ParseException e){
            return null;
        }
    }
}
