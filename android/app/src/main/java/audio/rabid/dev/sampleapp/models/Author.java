package audio.rabid.dev.sampleapp.models;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import audio.rabid.dev.sampleapp.Application;
import audio.rabid.dev.sampleapp.backend.Dao;
import audio.rabid.dev.sampleapp.backend.Database;
import audio.rabid.dev.sampleapp.backend.Resource;

/**
 * Created by charles on 10/23/15.
 */
@DatabaseTable(tableName = "authors")
public class Author extends Resource<Author> {

    @SuppressWarnings("unchecked")
    public static Dao<Author> Dao = new Dao<>(Database.getDBDao(Author.class));

    @DatabaseField
    private String name;

    @DatabaseField
    private String email;

    @DatabaseField
    private String avatar;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public URL getAvatar(){
        if(avatar==null) return null;
        try {
            return new URL(avatar);
        }catch (MalformedURLException e){
            return null;
        }
    }

    private Bitmap avatarBitmap;
    public void getAvatarBitmap(final Dao.SingleQueryCallback<Bitmap> callback) {
        //no image saved
        if(avatar==null){
            callback.onResult(null);
            return;
        }

        //image cached locally
        if(avatarBitmap!=null){
            callback.onResult(avatarBitmap);
            return;
        }
        URL url = getAvatar();
        //invalid url
        if(url==null){
            callback.onResult(null);
            return;
        }
        //fetch from network
        (new AsyncTask<URL, Void, Bitmap>(){
            @Override
            protected Bitmap doInBackground(URL... params) {
                Bitmap b = Application.ImageCache.get(avatar);
                if(b!=null){
                    return b;
                }else {
                    try {
                        HttpURLConnection connection = (HttpURLConnection) params[0].openConnection();
                        return BitmapFactory.decodeStream(connection.getInputStream());
                    } catch (IOException e) {
                        return null;
                    }
                }
            }
            @Override
            protected void onPostExecute(Bitmap bitmap){
                avatarBitmap = bitmap;
                Application.ImageCache.put(avatar, bitmap);
                callback.onResult(bitmap);
            }
        }).execute(url);
    }

    public void setAvatar(String avatar) {
        try{
            new URL(avatar);
            this.avatar = avatar;
            //clear saved images
            Application.ImageCache.remove(avatar);
            avatarBitmap = null;
        }catch (MalformedURLException e){
            //oops
        }
    }


    @Override
    public Dao<Author> getDao() {
        return Dao;
    }

    public JSONObject toJSON() throws JSONException{
        return new JSONObject()
                .put("id", id)
                .put("serverId", serverId)
                .put("name", name)
                .put("email", email)
                .put("avatar", avatar);
    }
}
