package edu.touro.mco152.bm.Commands;

import edu.touro.mco152.bm.*;
import edu.touro.mco152.bm.observers.Observable;
import edu.touro.mco152.bm.observers.Observer;
import edu.touro.mco152.bm.persist.DiskRun;
import edu.touro.mco152.bm.persist.EM;
import edu.touro.mco152.bm.ui.Gui;
import jakarta.persistence.EntityManager;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import static edu.touro.mco152.bm.App.*;
import static edu.touro.mco152.bm.App.msg;
import static edu.touro.mco152.bm.DiskMark.MarkType.WRITE;

/**
 * This class is a command as it implements the Command Interface, and it does the write part of the Benchmark process
 * using the data passed into the constructor
 */
public class WriteCmd implements CommandInterface, Observable {
    private List<Observer> observers = new ArrayList<>();
    private int numOfBlocks;
    private int numOfMarks;
    PersonalDataLog myDataLogger = new PersonalDataLog();
    private int wUnitsComplete = 0, rUnitsComplete = 0, unitsComplete;
    private int blockSizeKb;
    private DiskRun.BlockSequence blockSequence;
    private int wUnitsTotal;
    private int rUnitsTotal;
    private int unitsTotal;
    private float percentComplete;
    private UIInterface<DiskMark, Boolean> ui;
    private int blockSize;
    private byte[] blockArr;
    private DiskMark wMark;
    private int startFileNum;
    private DiskRun run;

    /**
     * @param ui          - the instance of the UIInterface that you are using to run the benchmark
     * @param numOFMarks  - the number of Marks wanted
     * @param numOfBlocks - desired number of blocks
     * @param blockSizeKb - the size of each block in kbs
     * @param sequence    - how
     */
    public WriteCmd(UIInterface<DiskMark, Boolean> ui, int numOFMarks, int numOfBlocks, int blockSizeKb, DiskRun.BlockSequence sequence) {
        this.numOfMarks = numOFMarks;
        this.ui = ui;
        this.numOfBlocks = numOfBlocks;
        this.blockSizeKb = blockSizeKb;
        this.blockSequence = sequence;
        this.wUnitsTotal = App.writeTest ? numOfBlocks * numOfMarks : 0;
        rUnitsTotal = App.readTest ? numOfBlocks * numOfMarks : 0;
        unitsTotal = wUnitsTotal + rUnitsTotal;
        blockSize = blockSizeKb * KILOBYTE;
        blockArr = new byte[blockSize];
        for (int b = 0; b < blockArr.length; b++) {
            if (b % 2 == 0) {
                blockArr[b] = (byte) 0xFF;
            }
        }
        startFileNum = App.nextMarkNumber;
    }

    /**
     * Made this method as a duplicate of the one in App in order to not have to call that one to ensure
     * the use of the variables of this file not App's
     *
     * @return long
     */
    public long targetTxSizeKb() {
        return (long) blockSizeKb * numOfBlocks * numOfMarks;
    }

    @Override
    public boolean execute() {
        // sending the data to my personal logger to use for testing
        myDataLogger.addToAppInfo((long) numOfMarks);
        myDataLogger.addToAppInfo((long) numOfBlocks);
        myDataLogger.addToAppInfo((long) blockSizeKb);
        myDataLogger.addToAppInfo(targetTxSizeKb());
        myDataLogger.setUtilDiskInfoFile(Util.getDiskInfo(dataDir));

        run = new DiskRun(DiskRun.IOMode.WRITE, this.blockSequence);
        run.setNumMarks(this.numOfMarks);
        run.setNumBlocks(this.numOfBlocks);
        run.setBlockSize(this.blockSizeKb);
        run.setTxSize(this.targetTxSizeKb());
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

            /**
              Begin an outer loop for specified duration (number of 'marks') of benchmark,
              that keeps writing data (in its own loop - for specified # of blocks). Each 'Mark' is timed
              and is reported to the GUI for display as each Mark completes.
             */
        for (int m = startFileNum; m < startFileNum + this.numOfMarks && !ui.isItCancelled(); m++) {

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
                        if (this.blockSequence == DiskRun.BlockSequence.RANDOM) {
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

        notifyObservers();
        return true;
    }


    @Override
    public void registerObserver(Observer observer) {
        observers.add(observer);
    }

    @Override
    public void removeObserver(Observer observer) {
        observers.remove(observer);
    }

    @Override
    public void notifyObservers() {
        for (Observer observer : observers) {
            observer.update(run);
        }
    }
}
