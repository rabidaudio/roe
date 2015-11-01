package audio.rabid.dev.network_orm;

import audio.rabid.dev.network_orm.models.TypedObservable;
import audio.rabid.dev.network_orm.models.TypedObserver;

/**
 * Created by charles on 11/1/15.
 *
 * A simple observable which detects changes in an object.
 */
public class ChangeDetectorObserver<T extends TypedObservable> implements TypedObserver<T> {

    private boolean sawChange = false;

    private Thread callingThread;

    @Override
    public void update(T observable, Object data) {
        sawChange = true;
        callingThread = Thread.currentThread();
    }

    public boolean sawChange() {
        if (sawChange) {
            sawChange = false;
            return true;
        } else {
            sawChange = true;
            return false;
        }
    }

    public Thread getCallingThread(){
        return callingThread;
    }
}