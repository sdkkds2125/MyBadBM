package edu.touro.mco152.bm;

import java.beans.PropertyChangeListener;
import java.util.concurrent.ExecutionException;

interface UIInterface<T> {
    boolean isItCancelled();

    void updateProgress(int progress);

    void sendData(T... chunks);

    T get() throws InterruptedException, ExecutionException;

    boolean cancelTask(boolean mayInterruptIfRunning);

    void addAPropertyChangeListener(PropertyChangeListener listener);

    void runOnOpenThread();
}
