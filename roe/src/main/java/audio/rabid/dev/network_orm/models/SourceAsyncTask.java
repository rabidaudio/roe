package audio.rabid.dev.network_orm.models;

import android.support.annotation.Nullable;

import java.util.List;

import audio.rabid.dev.network_orm.BackgroundThread;

/**
 * Created by charles on 11/1/15.
 */
public class SourceAsyncTask {
    /**
     * Handles running queries in background and callbacks on main thread
     *
     * @param <A> the data type returned by the query
     */
    protected abstract static class SingleSourceAsyncTask<A extends TypedObservable> implements Runnable {

        @Nullable
        private Source.OperationCallback<A> callback;

        public SingleSourceAsyncTask(@Nullable Source.OperationCallback<A> callback) {
            this.callback = callback;
        }

        public void execute() {
            BackgroundThread.postBackground(this);
        }

        public void run() {
            final A result = runInBackground();
            if (callback != null)
                BackgroundThread.postMain(new Runnable() {
                    @Override
                    public void run() {
                        callback.onResult(result);
                        if(result!=null) {
                            result.notifyObservers();
                        }
                    }
                });
        }

        protected abstract A runInBackground();
    }

    /**
     * Handles running queries in background and callbacks on main thread
     *
     * @param <A> the data type returned by the query
     */
    protected abstract static class MultipleSourceAsyncTask<A extends TypedObservable> implements Runnable {

        @Nullable
        private Source.OperationCallback<List<A>> callback;

        public MultipleSourceAsyncTask(@Nullable Source.OperationCallback<List<A>> callback) {
            this.callback = callback;
        }

        public void execute() {
            BackgroundThread.postBackground(this);
        }

        public void run() {
            final List<A> results = runInBackground();
            if (callback != null)
                BackgroundThread.postMain(new Runnable() {
                    @Override
                    public void run() {
                        callback.onResult(results);
                        for (A result : results) {
                            result.notifyObservers();
                        }
                    }
                });
        }

        protected abstract List<A> runInBackground();
    }
}
