package audio.rabid.dev.roe;

import android.os.Handler;
import android.os.Looper;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Created by charles on 11/11/15.
 *
 * TODO in future, find a way to support paralleled network operations with the option to serialize when necessary
 */
public class ThreadHandler {

    private Handler mainHandler;
//    private ExecutorService parallelExecutorService;
    private ExecutorService seriesExecutorService;

    private static ThreadHandler instance = new ThreadHandler();

    private ThreadHandler() {
        mainHandler = new Handler(Looper.getMainLooper());
//        parallelExecutorService = Executors.newCachedThreadPool();
        seriesExecutorService = Executors.newSingleThreadExecutor();
    }

    public static void postMain(Runnable r) {
        instance.mainHandler.post(r);
    }

    public static Future postBackground(Runnable r) {
        return instance.seriesExecutorService.submit(r);
//        return instance.parallelExecutorService.submit(r);
    }

    public static <T> Future<T> postBackground(Callable<T> callable) {
        return instance.seriesExecutorService.submit(callable);
//        return instance.parallelExecutorService.submit(callable);
    }
}
