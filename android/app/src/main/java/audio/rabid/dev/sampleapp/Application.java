package audio.rabid.dev.sampleapp;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import com.jakewharton.disklrucache.DiskLruCache;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import audio.rabid.dev.sampleapp.backend.Database;

/**
 * Created by charles on 10/23/15.
 */
public class Application extends android.app.Application {

    public Application(){
        super();
        Database.getInstance(this); //initialize database
        try {
            iCache = DiskLruCache.open(getCacheDir(), BuildConfig.VERSION_CODE, 50, 1024 * 1024 * 50);
        }catch (IOException e){
            throw new RuntimeException(e);
        }
    }

    private static DiskLruCache iCache;

    /**
     * Created by charles on 10/23/15.
     *
     * Wrapper for DiskLruCache which has a messy syntax
     */
    public static class ImageCache {

        public static Bitmap get(String key){
            String hashed = keyToHash(key);
            if(hashed==null) return null;
            try{
                return BitmapFactory.decodeStream(iCache.get(hashed).getInputStream(0));
            }catch (IOException e){
                return null;
            }
        }

        public static void put(String key, Bitmap bitmap){
            String hashed = keyToHash(key);
            if(hashed==null) return;
            try {
                DiskLruCache.Editor e = iCache.edit(hashed);
                OutputStream os = e.newOutputStream(0);
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, os);
                e.commit();
            }catch (IOException e){
                e.printStackTrace();
            }
        }

        public static void remove(String key){
            String hashed = keyToHash(key);
            if(hashed==null) return;
            try {
                iCache.remove(hashed);
            }catch (IOException e){
                e.printStackTrace();
            }
        }

        private static String keyToHash(String key){
            try {
                return new String(MessageDigest.getInstance("MD5").digest(key.getBytes("UTF-8")));
            }catch (NoSuchAlgorithmException | UnsupportedEncodingException e){
                e.printStackTrace();
                return null;
            }
        }
    }
}
