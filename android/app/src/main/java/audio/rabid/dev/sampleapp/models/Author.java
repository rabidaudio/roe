package audio.rabid.dev.sampleapp.models;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import audio.rabid.dev.network_orm.AllowedOps;
import audio.rabid.dev.network_orm.Resource;
import audio.rabid.dev.network_orm.ResourceCreator;
import audio.rabid.dev.network_orm.Source;
import audio.rabid.dev.sampleapp.Database;
import audio.rabid.dev.sampleapp.SampleAppServer;
import audio.rabid.dev.utils.ImageCache;

/**
 * Created by charles on 10/23/15.
 */
@DatabaseTable(tableName = "authors")
public class Author extends Resource<Author> {

    @SuppressWarnings("unchecked")
    public static final Source<Author> Source = new Source<>(SampleAppServer.getInstance(),
            Database.getDaoOrThrow(Author.class), "authors", "authors", new AuthorResourceCreator(),
            new AllowedOps(AllowedOps.Op.CREATE, AllowedOps.Op.READ, AllowedOps.Op.UPDATE));

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
    public void getAvatarBitmap(final Source.QueryCallback<Bitmap> callback) {
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
                Bitmap b = ImageCache.getInstance().get(avatar);
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
                ImageCache.getInstance().put(avatar, bitmap);
                callback.onResult(bitmap);
            }
        }).execute(url);
    }

    public void setAvatar(String avatar) {
        try{
            new URL(avatar);
            this.avatar = avatar;
            //clear saved images
            ImageCache.getInstance().remove(avatar);
            avatarBitmap = null;
        }catch (MalformedURLException e){
            //oops
        }
    }

    public void sendEmail(Context context){
        if(email==null) return;
        Intent i = new Intent(Intent.ACTION_SENDTO, Uri.fromParts("mailto", getEmail(), null));
        i.putExtra(Intent.EXTRA_EMAIL, new String[]{getEmail()});
        context.startActivity(Intent.createChooser(i, "Contact the author"));
    }


    @Override
    public Source<Author> getSource(){
        return Source;
    }

    @Override
    public JSONObject toJSON() throws JSONException{
        return super.toJSON()
                .put("name", name)
                .put("email", email)
                .put("avatar", avatar);
    }

    @Override
    protected synchronized boolean updateFromJSON(JSONObject data) throws JSONException{
        boolean changed = super.updateFromJSON(data);
        String n = data.getString("name");
        String e = data.getString("email");
        String a = data.getString("avatar");
        if(!n.equals(name)){
            name = n;
            changed = true;
        }
        if(!e.equals(email)){
            email = n;
            changed = true;
        }
        if(!a.equals(avatar)) {
            setAvatar(a);
            changed = true;
        }
        return changed;
    }

    private static class AuthorResourceCreator implements ResourceCreator<Author> {
        @Override
        public Author createFromJSON(JSONObject json) throws JSONException {
            Author a = new Author();
            a.updateFromJSON(json);
            return a;
        }
    }
}
