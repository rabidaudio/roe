//package audio.rabid.dev.roe.models;
//
//import android.content.Context;
//import android.net.NetworkRequest;
//import android.support.annotation.Nullable;
//
//import com.snappydb.DB;
//import com.snappydb.DBFactory;
//import com.snappydb.SnappydbException;
//
//import org.jdeferred.AlwaysCallback;
//import org.jdeferred.DoneCallback;
//import org.jdeferred.DoneFilter;
//import org.jdeferred.DonePipe;
//import org.jdeferred.FailCallback;
//import org.jdeferred.FailFilter;
//import org.jdeferred.FailPipe;
//import org.jdeferred.ProgressCallback;
//import org.jdeferred.ProgressFilter;
//import org.jdeferred.ProgressPipe;
//import org.jdeferred.Promise;
//import org.jdeferred.android.AndroidDeferredManager;
//import org.jdeferred.impl.DeferredObject;
//import org.json.JSONObject;
//
//import java.util.concurrent.Callable;
//
//import audio.rabid.dev.roe.models.cache.ResourceCache;
//import audio.rabid.dev.roe.models.cache.WeakMapResourceCache;
//import audio.rabid.dev.roe.models.network.NetworkOperation;
//import audio.rabid.dev.roe.models.network.Response;
//
///**
// * Created by  charles  on 3/30/16.
// */
//public abstract class RoeManager implements NetworkOperation.RequestCallbacks {
//
//    private DB database;
//
//    private ResourceCache cache;
//
//    private AndroidDeferredManager deferredManager = new AndroidDeferredManager();
//
//    private String rootURL;
//
//    public RoeManager(Context context, @Nullable String name, String rootURL, @Nullable ResourceCache cache) throws SnappydbException {
//        this.rootURL = rootURL;
//        if(name != null){
//            database = DBFactory.open(context, name);
//        }else{
//            database = DBFactory.open(context);
//        }
//        if(cache == null){
//            this.cache = new WeakMapResourceCache();
//        }else{
//            this.cache = cache;
//        }
//    }
//
//    public <T> Promise<T, Throwable, Void> getObject(final String id, final Model<T> model){
//        // if in cache, use that value
//        if(cache.has(id)){
//            T cacheResult = cache.get(id);
//            return new RoeDeferredObject<T>().resolve(cacheResult);
//        }
////        database.getObject(id, model.getModelClass());
//        NetworkOperation request = model.createShowRequest(id);
//        request.promise(rootURL, 1000, deferredManager, RoeManager.this).then(new DoneFilter<Response, T>() {
//            @Override
//            public T filterDone(Response result) {
//                return model.fromJSON(result.getResponseBody());
//            }
//        }).then(new DonePipe<T, T, Throwable, Void>() {
//            @Override
//            public Promise<T, Throwable, Void> pipeDone(T result) {
//                // store new values in database
//                database.put(id, result);
//                cache.put(id, result);
//                return null;
//            }
//        })
//
//    }
//
//    @Override
//    protected void finalize() throws Throwable {
//        super.finalize();
//        database.close();
//        cache.clear();
//    }
//
//    public class RoeDeferredObject<T> extends DeferredObject<T, Throwable, Void> {
//
////        @Override
////        public RoePromise<T> promise() {
////            return new RoePromise<>(super.promise());
////        }
//    }
//
//    // Wrapper for generic promise
////    public class RoePromise<T> implements Promise<T, Exception, Void> {
////
////        private Promise<T, Exception, Void> promise;
////
////        private RoePromise(Promise<T, Exception, Void> promise){
////            this.promise = promise;
////        }
////
////        @Override
////        public State state() {
////            return promise.state();
////        }
////
////        @Override
////        public boolean isPending() {
////            return promise.isPending();
////        }
////
////        @Override
////        public boolean isResolved() {
////            return promise.isResolved();
////        }
////
////        @Override
////        public boolean isRejected() {
////            return promise.isRejected();
////        }
////
////        @Override
////        public Promise<T, Exception, Void> then(DoneCallback<T> doneCallback) {
////            return promise.then(doneCallback);
////        }
////
////        @Override
////        public Promise<T, Exception, Void> then(DoneCallback<T> doneCallback, FailCallback<Exception> failCallback) {
////            return promise.then(doneCallback, failCallback);
////        }
////
////        @Override
////        public Promise<T, Exception, Void> then(DoneCallback<T> doneCallback, FailCallback<Exception> failCallback, ProgressCallback<Void> progressCallback) {
////            return promise.then(doneCallback, failCallback, progressCallback);
////        }
////
////        @Override
////        public <D_OUT, F_OUT, P_OUT> Promise<D_OUT, F_OUT, P_OUT> then(DoneFilter<T, D_OUT> doneFilter) {
////            return promise.then(doneFilter);
////        }
////
////        @Override
////        public <D_OUT, F_OUT, P_OUT> Promise<D_OUT, F_OUT, P_OUT> then(DoneFilter<T, D_OUT> doneFilter, FailFilter<Exception, F_OUT> failFilter) {
////            return promise.then(doneFilter, failFilter);
////        }
////
////        @Override
////        public <D_OUT, F_OUT, P_OUT> Promise<D_OUT, F_OUT, P_OUT> then(DoneFilter<T, D_OUT> doneFilter, FailFilter<Exception, F_OUT> failFilter, ProgressFilter<Void, P_OUT> progressFilter) {
////            return promise.then(doneFilter, failFilter, progressFilter);
////        }
////
////        @Override
////        public <D_OUT, F_OUT, P_OUT> Promise<D_OUT, F_OUT, P_OUT> then(DonePipe<T, D_OUT, F_OUT, P_OUT> donePipe) {
////            return promise.then(donePipe);
////        }
////
////        @Override
////        public <D_OUT, F_OUT, P_OUT> Promise<D_OUT, F_OUT, P_OUT> then(DonePipe<T, D_OUT, F_OUT, P_OUT> donePipe, FailPipe<Exception, D_OUT, F_OUT, P_OUT> failPipe) {
////            return promise.then(donePipe, failPipe);
////        }
////
////        @Override
////        public <D_OUT, F_OUT, P_OUT> Promise<D_OUT, F_OUT, P_OUT> then(DonePipe<T, D_OUT, F_OUT, P_OUT> donePipe, FailPipe<Exception, D_OUT, F_OUT, P_OUT> failPipe, ProgressPipe<Void, D_OUT, F_OUT, P_OUT> progressPipe) {
////            return promise.then(donePipe, failPipe, progressPipe);
////        }
////
////        @Override
////        public Promise<T, Exception, Void> done(DoneCallback<T> callback) {
////            return promise.done(callback);
////        }
////
////        @Override
////        public Promise<T, Exception, Void> fail(FailCallback<Exception> callback) {
////            return promise.fail(callback);
////        }
////
////        @Override
////        public Promise<T, Exception, Void> always(AlwaysCallback<T, Exception> callback) {
////            return promise.always(callback);
////        }
////
////        @Override
////        public Promise<T, Exception, Void> progress(ProgressCallback<Void> callback) {
////            return promise.progress(callback);
////        }
////
////        @Override
////        public void waitSafely() throws InterruptedException {
////            promise.waitSafely();
////        }
////
////        @Override
////        public void waitSafely(long timeout) throws InterruptedException {
////            promise.waitSafely(timeout);
////        }
////    }
//}
