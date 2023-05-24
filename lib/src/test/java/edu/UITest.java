package edu;

import edu.touro.mco152.bm.*;
import edu.touro.mco152.bm.persist.DiskRun;
import edu.touro.mco152.bm.ui.Gui;
import edu.touro.mco152.bm.ui.MainFrame;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class UITest {

    /**
     * Bruteforce setup of static classes/fields to allow DiskWorker to run.
     *
     * @author lcmcohen
     */
    private void setupDefaultAsPerProperties() {
        /// Do the minimum of what  App.init() would do to allow to run.
        Gui.mainFrame = new MainFrame();
        App.p = new Properties();
        App.loadConfig();
        System.out.println(App.getConfigString());
        Gui.progressBar = Gui.mainFrame.getProgressBar(); //must be set or get Nullptr

        // configure the embedded DB in .jDiskMark
        System.setProperty("derby.system.home", App.APP_CACHE_DIR);

        // code from startBenchmark
        //4. create data dir reference
        App.dataDir = new File(App.locationDir.getAbsolutePath() + File.separator + App.DATADIRNAME);

        //5. remove existing test data if exist
        if (App.dataDir.exists()) {
            if (App.dataDir.delete()) {
                App.msg("removed existing data dir");
            } else {
                App.msg("unable to remove existing data dir");
            }
        } else {
            App.dataDir.mkdirs(); // create data dir if not already present
        }
    }
    DiskWorker worker = new DiskWorker(new NonSwingUI());
    NonSwingUI currentUI = (NonSwingUI) worker.getUi();

    @Test
    void progressTest() throws Exception {
        //Arrange
        setupDefaultAsPerProperties();
        currentUI.benchmarkWork();
        ArrayList<Integer> intermediateResults = currentUI.getProgresses();
        //Act
        for (Integer num : intermediateResults) {
            //Assert
            assertTrue(num >= 0 && num <= 100);
        }
    }

    @Test
    void finishedTest(){
        //Arrange
        setupDefaultAsPerProperties();
        currentUI.benchmarkWork();
        //assert
        assertTrue(currentUI.getMethodResult());
    }

    @Test
    void diskRunDataTest(){
        //Arrange
        ArrayList<Long> appInfo = PersonalDataLog.getAppInfo();
        ArrayList<Long> diskRunInfo = PersonalDataLog.getDiskRunInfo();
        String utilDiskInfoFile = PersonalDataLog.getUtilDiskInfoFile();
        String diskRunInfoFile = PersonalDataLog.getDiskRunInfoFile();
        setupDefaultAsPerProperties();
        currentUI.benchmarkWork();
        //Act

        //Assert
        assertEquals(utilDiskInfoFile,diskRunInfoFile);
        assertTrue(PersonalDataLog.getRun().getRunMax() < 7500);
        assertTrue(PersonalDataLog.getRun().getRunMin() > 0);
        assertTrue(PersonalDataLog.getRun().getRunAvg() < 6000);



    }
}
