package edu.touro.mco152.bm;

import edu.touro.mco152.bm.ui.Gui;

import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.logging.Logger;

import static edu.touro.mco152.bm.App.dataDir;
/** This is a non-swing UI implementation of the UIInterface made for testing purposes to see if all
 * dependencies on swing have been removed
 * */

public class NonSwingUI implements UIInterface<DiskMark, Boolean> {

    private Callable<Boolean> benchmarkWorkType;
    int progress;
    int oldProgress;
    ArrayList<Integer> progresses = new ArrayList<>();
    boolean methodResult;

    @Override
    public boolean benchmarkWork() {
        try {
            methodResult = benchmarkWorkType.call();
            done();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return methodResult;
    }

    @Override
    public boolean isItCancelled() {
        return false;
    }

    @Override
    public void updateProgress(int progress) {
        if (progress < 0 || progress > 100) {
            throw new IllegalArgumentException("the value should be from 0 to 100");
        }
        oldProgress = this.progress;
        this.progress = progress;
        if (!(oldProgress == this.progress)) {
            progresses.add(progress);
        }
    }

    @Override
    public void sendData(DiskMark... chunks) {
        process(List.of(chunks));
    }

    @Override
    public Boolean getStatus() throws InterruptedException, ExecutionException {
        return methodResult;
    }

    @Override
    public boolean cancelTask(boolean mayInterruptIfRunning) {
        return false;
    }

    @Override
    public void addAPropertyChangeListener(PropertyChangeListener listener) {

    }

    @Override
    public void runOnOpenThread() {

    }

    @Override
    public void setBenchmarkWork(Callable<Boolean> benchmarkWork) {
        benchmarkWorkType = benchmarkWork;
    }

    public int getProgress() {
        return progress;
    }

    protected void process(List<DiskMark> markList) {

    }

    protected void done() {
        // Obtain final status, might from doInBackground ret value, or SwingWorker error
        try {
            methodResult = (Boolean) getStatus();   // record for future access
            System.out.println("done");
        } catch (Exception e) {
            Logger.getLogger(App.class.getName()).warning("Problem obtaining final status: " + e.getMessage());
        }

        if (App.autoRemoveData) {
            Util.deleteDirectory(dataDir);
        }
        App.state = App.State.IDLE_STATE;
        Gui.mainFrame.adjustSensitivity();
    }

    public Boolean getMethodResult() {
        return methodResult;
    }

    public ArrayList<Integer> getProgresses() {
        return progresses;
    }
}
