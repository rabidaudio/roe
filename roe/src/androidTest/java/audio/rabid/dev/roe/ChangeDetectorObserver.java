package audio.rabid.dev.roe;

import audio.rabid.dev.roe.models.TypedObserver;

/**
 * Created by charles on 11/1/15.
 *
 * A simple observable which detects changes in an object.
 */
public class ChangeDetectorObserver<T> implements TypedObserver<T> {

    private int seenChanges = 0;

    private Thread callingThread;

    private boolean deleted;

    @Override
    public synchronized void update(T observable, boolean deleted) {
        seenChanges++;
        this.deleted = deleted;
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

    public synchronized boolean wasDeleted() {
        return deleted;
    }

    public synchronized Thread getCallingThread(){
        return callingThread;
    }
}