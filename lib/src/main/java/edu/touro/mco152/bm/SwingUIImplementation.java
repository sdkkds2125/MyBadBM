package edu.touro.mco152.bm;

import edu.touro.mco152.bm.persist.DiskRun;
import edu.touro.mco152.bm.persist.EM;
import edu.touro.mco152.bm.ui.Gui;
import jakarta.persistence.EntityManager;

import javax.swing.*;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;

import static edu.touro.mco152.bm.App.*;
import static edu.touro.mco152.bm.DiskMark.MarkType.READ;
import static edu.touro.mco152.bm.DiskMark.MarkType.WRITE;

public class SwingUIImplementation extends SwingWorker<Boolean, DiskMark> implements UIInterface <DiskMark , Boolean>{

    Callable<Boolean> benchmarkWorkType;
    boolean lastStatus;

    @Override
    protected Boolean doInBackground() throws Exception {
        return benchmarkWork();
    }

    @Override
    public boolean benchmarkWork() throws Exception {
        return benchmarkWorkType.call();
    }

    @Override
    public boolean isItCancelled() {
        return isCancelled();
    }

    @Override
    public void updateProgress(int progress) {
        setProgress(progress);
    }

    @Override
    public void sendData(DiskMark... chunks) {
        publish(chunks);
    }

    @Override
    public Boolean getStatus() throws InterruptedException, ExecutionException {
        return get();
    }

    @Override
    public boolean cancelTask(boolean mayInterruptIfRunning) {
        return cancel(mayInterruptIfRunning);
    }

    @Override
    public void addAPropertyChangeListener(PropertyChangeListener listener) {
        addPropertyChangeListener(listener);
    }

    @Override
    public void runOnOpenThread() {
        execute();
    }

    @Override
    public void setBenchmarkWork(Callable<Boolean> benchmarkWork) {
        benchmarkWorkType = benchmarkWork;
    }

    @Override
    protected void done() {
        // Obtain final status, might from doInBackground ret value, or SwingWorker error
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

    /**
     * Process a list of 'chunks' that have been processed, ie that our thread has previously
     * published to Swing. For my info, watch Professor Cohen's video -
     * Module_6_RefactorBadBM Swing_DiskWorker_Tutorial.mp4
     *
     * @param markList a list of DiskMark objects reflecting some completed benchmarks
     */
    @Override
    protected void process(List<DiskMark> markList) {
        markList.stream().forEach((dm) -> {
            if (dm.type == DiskMark.MarkType.WRITE) {
                Gui.addWriteMark(dm);
            } else {
                Gui.addReadMark(dm);
            }
        });
    }

    public Boolean getLastStatus() {
        return lastStatus;
    }
}
