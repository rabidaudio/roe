package audio.rabid.dev.utils;

import android.graphics.Bitmap;
import android.util.LruCache;

/**
 * Created by charles on 10/23/15.
 */
public class ImageCache extends LruCache<String, Bitmap> {

    private static ImageCache instance;

    public static ImageCache getInstance() {
        if (instance == null) {
            instance = new ImageCache();
        }
        return instance;
    }

    private ImageCache() {
        super(50);
    }
}
