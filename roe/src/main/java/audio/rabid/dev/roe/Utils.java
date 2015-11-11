package audio.rabid.dev.roe;

import java.lang.reflect.Field;

/**
 * Created by charles on 11/11/15.
 */
public class Utils {

    public static <T> void copyFields(Class<T> tClass, T from, T to) throws IllegalAccessException {
        for (Class<?> classWalk = tClass; classWalk != null; classWalk = classWalk.getSuperclass()) {
            for (Field field : classWalk.getDeclaredFields()) {
                try {
                    field.setAccessible(true);
                    field.set(to, field.get(from));
                } finally {
                    field.setAccessible(false);
                }
            }
        }
    }
}
