package audio.rabid.dev.network_orm;

import android.os.Handler;
import android.os.Looper;

/**
 * Created by charles on 10/28/15.
 */
public class BackgroundThread extends Thread {

    private Handler handler;

    private Handler mainHandler = new Handler(Looper.getMainLooper());

    private Object semaphore = new Object();

    private BackgroundThread(){
        super("SourceBackgroundLooperTask");
        try {
            semaphore.wait();
        }catch (InterruptedException e){
            throw new RuntimeException("Problem creating background thread", e);
        }
    }

    public void run(){
        Looper.prepare();
        handler = new Handler(Looper.myLooper());
        semaphore.notify();
        Looper.loop();
    }

    private static final BackgroundThread instance = new BackgroundThread();

    public static Handler getBackgroundHandler(){
        return instance.handler;
    }

    public static Handler getMainHandler(){
        return instance.mainHandler;
    }

    public static void postMain(Runnable r){
        getMainHandler().post(r);
    }

    public static void postBackground(Runnable r){
        getBackgroundHandler().post(r);
    }
}
