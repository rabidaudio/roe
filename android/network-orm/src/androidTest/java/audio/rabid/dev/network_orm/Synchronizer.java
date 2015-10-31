package audio.rabid.dev.network_orm;

import java.util.concurrent.TimeoutException;

/**
 * Created by charles on 10/30/15.
 *
 * This allows tests to block until an async task is finished so that you can test async methods in
 * a usable way. You don't ever want to use something like this outside of a test!
 *
 * Make your asynchronous call in {@link #run()}, and be sure to call {@link #setResult(Object)} when
 * it has finished. By default, the block will timeout after 5 seconds. To change the timeout length,
 * use {@link #Synchronizer(int)}. Call it with a timeout of 0 to block forever.
 */
public abstract class Synchronizer<T> implements Runnable{
    private final Object lock = new Object();

    private T result = null;

    private int timeout = 5*1000;

    public Synchronizer(){

    }

    private boolean resultSet = false;

    public Synchronizer(int timeout){
        this.timeout = timeout;
    }

    public void setResult(T result){
        this.result = result;
        resultSet = true;
        synchronized (lock) {
            lock.notify();
        }
    }

    public T blockUntilFinished() throws InterruptedException, TimeoutException {
        run();
        synchronized (lock){
            lock.wait(timeout);
        }
        if(!resultSet){
            throw new TimeoutException("Async task timed out");
        }else{
            return result;
        }
    }
}