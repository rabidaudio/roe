package audio.rabid.dev.roe;

import audio.rabid.dev.roe.models.TypedObservable;
import audio.rabid.dev.roe.models.TypedObserver;

/**
 * Created by charles on 11/1/15.
 *
 * A simple observable which detects changes in an object.
 */
public class ChangeDetectorObserver<T extends TypedObservable> implements TypedObserver<T> {

    private int seenChanges = 0;

    private Thread callingThread;

    @Override
    public synchronized void update(T observable, Object data) {
        seenChanges++;
        callingThread = Thread.currentThread();
    }

    public synchronized boolean sawChange() {
        if (seenChanges > 0) {
            clearChanges();
            return true;
        } else {
            return false;
        }
    }

    public synchronized void clearChanges(){
        seenChanges = 0;
    }

    public synchronized int seenChanges(){
        return seenChanges;
    }

    public synchronized Thread getCallingThread(){
        return callingThread;
    }
}