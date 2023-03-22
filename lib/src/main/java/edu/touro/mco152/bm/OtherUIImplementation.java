package edu.touro.mco152.bm;

import edu.touro.mco152.bm.ui.Gui;

import javax.swing.*;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.List;
import java.util.concurrent.*;
import java.util.logging.Logger;
//import sun.swing.AccumulativeRunnable;

import static edu.touro.mco152.bm.App.dataDir;

public class OtherUIImplementation implements UIInterface<DiskMark, Boolean>{

//    private AccumulativeRunnable<Runnable> doSubmit;
    private Callable<Boolean> benchmarkWorkType;
    boolean lastStatus;
    int progress;
    PropertyChangeSupport changeSupport;
    private ExecutorService executer = Executors.newSingleThreadExecutor();
    FutureTask<Boolean> future =  new FutureTask<Boolean>(benchmarkWorkType) {
        @Override
        protected void done() {
            doneEDT();
            //setState(SwingWorker.StateValue.DONE);
        }
    };


    @Override
    public boolean benchmarkWork() throws Exception {
        future.run();
        return true;
    }

    @Override
    public boolean isItCancelled() {
        return future.isCancelled();
    }

    @Override
    public void updateProgress(int progress) {
        if (progress < 0 || progress > 100) {
            throw new IllegalArgumentException("the value should be from 0 to 100");
        }
        if (this.progress == progress) {
            return;
        }
        this.progress = progress;
    }

    @Override
    public void sendData(DiskMark... chunks) {
            process(List.of(chunks));
    }

    @Override
    public Boolean getStatus() throws InterruptedException, ExecutionException {
        return future.get();
    }

    @Override
    public boolean cancelTask(boolean mayInterruptIfRunning) {
        return future.cancel(mayInterruptIfRunning);
    }

    @Override
    public void addAPropertyChangeListener(PropertyChangeListener listener) {
            changeSupport.addPropertyChangeListener(listener);
    }

    @Override
    public void runOnOpenThread() {

    }

    @Override
    public void setBenchmarkWork(Callable<Boolean> benchmarkWork) {
        benchmarkWorkType = benchmarkWork;
    }

    /**
     * Invokes {@code done} on the EDT.
     */
    private void doneEDT() {
        Runnable doDone =
                new Runnable() {
                    public void run() {
                        done();
                    }
                };
//        if (this.executer) {
//            doDone.run();
//        } else {
//            doSubmit.add(doDone);
//        }
    }
    protected void done() {

        try {
            lastStatus = (Boolean) getStatus();   // record for future access
        } catch (Exception e) {
            Logger.getLogger(App.class.getName()).warning("Problem obtaining final status: " + e.getMessage());
        }

        if (App.autoRemoveData) {
            Util.deleteDirectory(dataDir);
        }
        App.state = App.State.IDLE_STATE;
        Gui.mainFrame.adjustSensitivity();
    }

    protected void process(List<DiskMark> markList) {
        markList.stream().forEach((dm) -> {
            if (dm.type == DiskMark.MarkType.WRITE) {
                Gui.addWriteMark(dm);
            } else {
                Gui.addReadMark(dm);
            }
        });
    }

}
