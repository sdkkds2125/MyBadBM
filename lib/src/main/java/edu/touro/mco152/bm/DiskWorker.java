package edu.touro.mco152.bm;

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
    PersonalDataLog myDataLogger = new PersonalDataLog();

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

    /*
      init local vars that keep track of benchmarks, and a large read/write buffer
     */
                int wUnitsComplete = 0, rUnitsComplete = 0, unitsComplete;
                int wUnitsTotal = App.writeTest ? numOfBlocks * numOfMarks : 0;
                int rUnitsTotal = App.readTest ? numOfBlocks * numOfMarks : 0;
                int unitsTotal = wUnitsTotal + rUnitsTotal;
                float percentComplete;

                int blockSize = blockSizeKb * KILOBYTE;
                byte[] blockArr = new byte[blockSize];
                for (int b = 0; b < blockArr.length; b++) {
                    if (b % 2 == 0) {
                        blockArr[b] = (byte) 0xFF;
                    }
                }

                DiskMark wMark, rMark;  // declare vars that will point to objects used to pass progress to UI

                Gui.updateLegend();  // init chart legend info

                if (App.autoReset) {
                    App.resetTestData();
                    Gui.resetTestData();
                }

                int startFileNum = App.nextMarkNumber;

        /*
          The GUI allows a Write, Read, or both types of BMs to be started. They are done serially.
         */
                if (App.writeTest) {
                    // sending the data to my personal logger to use for testing
                    myDataLogger.addToAppInfo((long) numOfMarks);
                    myDataLogger.addToAppInfo((long) numOfBlocks);
                    myDataLogger.addToAppInfo((long) blockSizeKb);
                    myDataLogger.addToAppInfo(targetTxSizeKb());
                    myDataLogger.setUtilDiskInfoFile(Util.getDiskInfo(dataDir));

                    DiskRun run = new DiskRun(DiskRun.IOMode.WRITE, App.blockSequence);
                    run.setNumMarks(App.numOfMarks);
                    run.setNumBlocks(App.numOfBlocks);
                    run.setBlockSize(App.blockSizeKb);
                    run.setTxSize(App.targetTxSizeKb());
                    run.setDiskInfo(Util.getDiskInfo(dataDir));

                    // sending the data to my personal logger to use for testing
                    myDataLogger.addToDiskRunInfo((long) run.getNumMarks());
                    myDataLogger.addToDiskRunInfo((long) run.getNumBlocks());
                    myDataLogger.addToDiskRunInfo((long) run.getBlockSize());
                    myDataLogger.addToDiskRunInfo(run.getTxSize());
                    myDataLogger.setDiskRunInfoFile(run.getDiskInfo());


                    // Tell logger and GUI to display what we know so far about the Run
                    msg("disk info: (" + run.getDiskInfo() + ")");

                    Gui.chartPanel.getChart().getTitle().setVisible(true);
                    Gui.chartPanel.getChart().getTitle().setText(run.getDiskInfo());

                    // Create a test data file using the default file system and config-specified location
                    if (!App.multiFile) {
                        testFile = new File(dataDir.getAbsolutePath() + File.separator + "testdata.jdm");
                    }

            /*
              Begin an outer loop for specified duration (number of 'marks') of benchmark,
              that keeps writing data (in its own loop - for specified # of blocks). Each 'Mark' is timed
              and is reported to the GUI for display as each Mark completes.
             */
                    for (int m = startFileNum; m < startFileNum + App.numOfMarks && !ui.isItCancelled(); m++) {

                        if (App.multiFile) {
                            testFile = new File(dataDir.getAbsolutePath()
                                    + File.separator + "testdata" + m + ".jdm");
                        }
                        wMark = new DiskMark(WRITE);    // starting to keep track of a new benchmark
                        wMark.setMarkNum(m);
                        long startTime = System.nanoTime();
                        long totalBytesWrittenInMark = 0;

                        String mode = "rw";
                        if (App.writeSyncEnable) {
                            mode = "rwd";
                        }

                        try {
                            try (RandomAccessFile rAccFile = new RandomAccessFile(testFile, mode)) {
                                for (int b = 0; b < numOfBlocks; b++) {
                                    if (App.blockSequence == DiskRun.BlockSequence.RANDOM) {
                                        int rLoc = Util.randInt(0, numOfBlocks - 1);
                                        rAccFile.seek((long) rLoc * blockSize);
                                    } else {
                                        rAccFile.seek((long) b * blockSize);
                                    }
                                    rAccFile.write(blockArr, 0, blockSize);
                                    totalBytesWrittenInMark += blockSize;
                                    wUnitsComplete++;
                                    unitsComplete = rUnitsComplete + wUnitsComplete;
                                    percentComplete = (float) unitsComplete / (float) unitsTotal * 100f;

                            /*
                              Report to GUI what percentage level of Entire BM (#Marks * #Blocks) is done.
                             */
                                    ui.updateProgress((int) percentComplete);
                                }
                            }
                        } catch (IOException ex) {
                            Logger.getLogger(App.class.getName()).log(Level.SEVERE, null, ex);
                        }

                /*
                  Compute duration, throughput of this Mark's step of BM
                 */
                        long endTime = System.nanoTime();
                        long elapsedTimeNs = endTime - startTime;
                        double sec = (double) elapsedTimeNs / (double) 1000000000;
                        double mbWritten = (double) totalBytesWrittenInMark / (double) MEGABYTE;
                        wMark.setBwMbSec(mbWritten / sec);
                        msg("m:" + m + " write IO is " + wMark.getBwMbSecAsString() + " MB/s     "
                                + "(" + Util.displayString(mbWritten) + "MB written in "
                                + Util.displayString(sec) + " sec)");
                        App.updateMetrics(wMark);

                /*
                  Let the GUI know the interim result described by the current Mark
                 */
                        ui.sendData(wMark);

                        // Keep track of statistics to be displayed and persisted after all Marks are done.
                        run.setRunMax(wMark.getCumMax());
                        run.setRunMin(wMark.getCumMin());
                        run.setRunAvg(wMark.getCumAvg());
                        run.setEndTime(new Date());
                        myDataLogger.setRun(run);
                    } // END outer loop for specified duration (number of 'marks') for WRITE benchmark

            /*
              Persist info about the Write BM Run (e.g. into Derby Database) and add it to a GUI panel
             */
                    EntityManager em = EM.getEntityManager();
                    em.getTransaction().begin();
                    //em.persist(run);
                    em.getTransaction().commit();

                    Gui.runPanel.addRun(run);
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
                    // sending the data to my personal logger to use for testing
                    myDataLogger.addToAppInfo((long) numOfMarks);
                    myDataLogger.addToAppInfo((long) numOfBlocks);
                    myDataLogger.addToAppInfo((long) blockSizeKb);
                    myDataLogger.addToAppInfo(targetTxSizeKb());
                    myDataLogger.setUtilDiskInfoFile(Util.getDiskInfo(dataDir));

                    DiskRun run = new DiskRun(DiskRun.IOMode.READ, App.blockSequence);
                    run.setNumMarks(App.numOfMarks);
                    run.setNumBlocks(App.numOfBlocks);
                    run.setBlockSize(App.blockSizeKb);
                    run.setTxSize(App.targetTxSizeKb());
                    run.setDiskInfo(Util.getDiskInfo(dataDir));

                    // sending the data to my personal logger to use for testing
                    myDataLogger.addToDiskRunInfo((long) run.getNumMarks());
                    myDataLogger.addToDiskRunInfo((long) run.getNumBlocks());
                    myDataLogger.addToDiskRunInfo((long) run.getBlockSize());
                    myDataLogger.addToDiskRunInfo(run.getTxSize());
                    myDataLogger.setDiskRunInfoFile(run.getDiskInfo());


                    msg("disk info: (" + run.getDiskInfo() + ")");

                    Gui.chartPanel.getChart().getTitle().setVisible(true);
                    Gui.chartPanel.getChart().getTitle().setText(run.getDiskInfo());

                    for (int m = startFileNum; m < startFileNum + App.numOfMarks && !ui.isItCancelled(); m++) {

                        if (App.multiFile) {
                            testFile = new File(dataDir.getAbsolutePath()
                                    + File.separator + "testdata" + m + ".jdm");
                        }
                        rMark = new DiskMark(READ);  // starting to keep track of a new benchmark
                        rMark.setMarkNum(m);
                        long startTime = System.nanoTime();
                        long totalBytesReadInMark = 0;

                        try (RandomAccessFile rAccFile = new RandomAccessFile(testFile, "r")) {
                            for (int b = 0; b < numOfBlocks; b++) {
                                if (App.blockSequence == DiskRun.BlockSequence.RANDOM) {
                                    int rLoc = Util.randInt(0, numOfBlocks - 1);
                                    rAccFile.seek((long) rLoc * blockSize);
                                } else {
                                    rAccFile.seek((long) b * blockSize);
                                }
                                rAccFile.readFully(blockArr, 0, blockSize);
                                totalBytesReadInMark += blockSize;
                                rUnitsComplete++;
                                unitsComplete = rUnitsComplete + wUnitsComplete;
                                percentComplete = (float) unitsComplete / (float) unitsTotal * 100f;
                                ui.updateProgress((int) percentComplete);
                            }
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                        long endTime = System.nanoTime();
                        long elapsedTimeNs = endTime - startTime;
                        double sec = (double) elapsedTimeNs / (double) 1000000000;
                        double mbRead = (double) totalBytesReadInMark / (double) MEGABYTE;
                        rMark.setBwMbSec(mbRead / sec);
                        msg("m:" + m + " READ IO is " + rMark.getBwMbSec() + " MB/s    "
                                + "(MBread " + mbRead + " in " + sec + " sec)");
                        App.updateMetrics(rMark);
                        ui.sendData(rMark);

                        run.setRunMax(rMark.getCumMax());
                        run.setRunMin(rMark.getCumMin());
                        run.setRunAvg(rMark.getCumAvg());
                        run.setEndTime(new Date());
                        myDataLogger.setRun(run);
                    }

            /*
              Persist info about the Read BM Run (e.g. into Derby Database) and add it to a GUI panel
             */
                    EntityManager em = EM.getEntityManager();
                    em.getTransaction().begin();
                  //  em.persist(run);
                    em.getTransaction().commit();

                    Gui.runPanel.addRun(run);
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
