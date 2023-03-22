package edu.touro.mco152.bm;

import java.beans.PropertyChangeListener;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;

/** This interface defines what it means to be a UI in order to run a benchmark through the DiskWorker class  */

public interface UIInterface<T , V> {

    boolean benchmarkWork() throws Exception;

    boolean isItCancelled();

    void updateProgress(int progress);

    void sendData(T... chunks);

    V getStatus() throws InterruptedException, ExecutionException;

    boolean cancelTask(boolean mayInterruptIfRunning);

    void addAPropertyChangeListener(PropertyChangeListener listener);

    void runOnOpenThread();

    void setBenchmarkWork(Callable<Boolean> benchmarkWork);
}
