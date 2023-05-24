package edu.touro.mco152.bm.Commands;

import edu.touro.mco152.bm.*;
import edu.touro.mco152.bm.observers.Observable;
import edu.touro.mco152.bm.observers.Observer;
import edu.touro.mco152.bm.persist.DiskRun;
import edu.touro.mco152.bm.persist.EM;
import edu.touro.mco152.bm.ui.Gui;
import jakarta.persistence.EntityManager;

import javax.swing.*;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import static edu.touro.mco152.bm.App.*;
import static edu.touro.mco152.bm.App.msg;
import static edu.touro.mco152.bm.DiskMark.MarkType.READ;
/**
 * This class is a command as it implements the Command Interface, and it does the Read part of the Benchmark process
 * using the data passed into the constructor
 * It also implements the Observable interface, so it registers and notifies observers as necessary
 */
public class ReadCmd implements CommandInterface, Observable {
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
    private DiskMark rMark;
    private int startFileNum;
    private DiskRun run;

    public ReadCmd(UIInterface<DiskMark, Boolean> ui, int numOFMarks, int numOfBlocks, int blockSizeKb, DiskRun.BlockSequence sequence) {
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

    /** Made this method as a duplicate of the one in App in order to not have to call that one to ensure
     * the use of the variables of this file not App's
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

        run = new DiskRun(DiskRun.IOMode.READ, this.blockSequence);
        run.setNumMarks(this.numOfMarks);
        run.setNumBlocks(this.numOfBlocks);
        run.setBlockSize(this.blockSizeKb);
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

        for (int m = startFileNum; m < startFileNum + this.numOfMarks && !ui.isItCancelled(); m++) {

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
                    if (this.blockSequence == DiskRun.BlockSequence.RANDOM) {
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
            } catch (FileNotFoundException ex) {
                Logger.getLogger(App.class.getName()).log(Level.SEVERE, null, ex);
                String emsg = "May not have done Write Benchmarks, so no data available to read." +
                        ex.getMessage();
                JOptionPane.showMessageDialog(Gui.mainFrame, emsg, "Unable to READ", JOptionPane.ERROR_MESSAGE);
                msg(emsg);
                return false;
            } catch (IOException ex) {
                throw new RuntimeException(ex);
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
        for (Observer observer: observers){
            observer.update(run);
        }
    }
}
