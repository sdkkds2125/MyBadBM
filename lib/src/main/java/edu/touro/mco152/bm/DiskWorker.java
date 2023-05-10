package edu.touro.mco152.bm;

import edu.touro.mco152.bm.Commands.Executor;
import edu.touro.mco152.bm.Commands.ReadCmd;
import edu.touro.mco152.bm.Commands.WriteCmd;
import edu.touro.mco152.bm.persist.DiskRun;
import edu.touro.mco152.bm.persist.EM;
import edu.touro.mco152.bm.ui.Gui;

import jakarta.persistence.EntityManager;

import javax.swing.*;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;

import static edu.touro.mco152.bm.App.*;
import static edu.touro.mco152.bm.DiskMark.MarkType.READ;
import static edu.touro.mco152.bm.DiskMark.MarkType.WRITE;

/**
 * Run the disk benchmarking with any UI that implements the UIInterface for example in swing:
 * Swing-compliant thread (only one of these threads can run at
 * once.) Cooperates with Swing to provide and make use of interim and final progress and
 * information, which is also recorded as needed to the persistence store, and log.
 * <p>
 * Depends on static values that describe the benchmark to be done having been set in App and Gui classes.
 * The DiskRun class is used to keep track of and persist info about each benchmark at a higher level (a run),
 * while the DiskMark class described each iteration's result, which is displayed by the UI as the benchmark run
 * progresses.
 * <p>
 * This class only knows how to do 'read' or 'write' disk benchmarks. It is instantiated by the
 * startBenchmark() method.
 * <p>
 */

public class DiskWorker {


    Boolean lastStatus = null;  // so far unknown
    UIInterface<DiskMark, Boolean> ui;
    Executor executor = new Executor();

    public DiskWorker(UIInterface<DiskMark, Boolean> ui) {
        this.ui = ui;
        ui.setBenchmarkWork(new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {

            /*
          We 'got here' because: 1: End-user clicked 'Start' on the benchmark UI,
          which triggered the start-benchmark event associated with the App::startBenchmark()
          method.  2: startBenchmark() then instantiated a DiskWorker, and called
          its (super class's) execute() method, causing Swing to eventually
          call this doInBackground() method.
          or some other way with a different UI like using cmd line.
         */
                Logger.getLogger(App.class.getName()).log(Level.INFO, "*** New worker thread started ***");
                msg("Running readTest " + App.readTest + "   writeTest " + App.writeTest);
                msg("num files: " + App.numOfMarks + ", num blks: " + App.numOfBlocks
                        + ", blk size (kb): " + App.blockSizeKb + ", blockSequence: " + App.blockSequence);


                Gui.updateLegend();  // init chart legend info

                if (App.autoReset) {
                    App.resetTestData();
                    Gui.resetTestData();
                }


        /*
          The GUI allows a Write, Read, or both types of BMs to be started. They are done serially.
         */
                if (App.writeTest) {
                    executor.executeCommand(new WriteCmd(ui,25,32,512, DiskRun.BlockSequence.SEQUENTIAL));
                }

        /*
          Most benchmarking systems will try to do some cleanup in between 2 benchmark operations to
          make it more 'fair'. For example a networking benchmark might close and re-open sockets,
          a memory benchmark might clear or invalidate the Op Systems TLB or other caches, etc.
         */

                // try renaming all files to clear catch
                if (App.readTest && App.writeTest && !ui.isItCancelled()) {
                    JOptionPane.showMessageDialog(Gui.mainFrame,
                            """
                                    For valid READ measurements please clear the disk cache by
                                    using the included RAMMap.exe or flushmem.exe utilities.
                                    Removable drives can be disconnected and reconnected.
                                    For system drives use the WRITE and READ operations\s
                                    independantly by doing a cold reboot after the WRITE""",
                            "Clear Disk Cache Now", JOptionPane.PLAIN_MESSAGE);
                }

                // Same as above, just for Read operations instead of Writes.
                if (App.readTest) {
                    executor.executeCommand(new ReadCmd(ui,25,32,512, DiskRun.BlockSequence.SEQUENTIAL));
                }
                App.nextMarkNumber += App.numOfMarks;
                return true;
            }
        });
    }

    public void setUi(UIInterface ui) {
        this.ui = ui;
    }

    public UIInterface getUi() {
        return ui;
    }

}
