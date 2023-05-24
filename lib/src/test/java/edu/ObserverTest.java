package edu;

import edu.touro.mco152.bm.App;
import edu.touro.mco152.bm.Commands.Executor;
import edu.touro.mco152.bm.Commands.ReadCmd;
import edu.touro.mco152.bm.NonSwingUI;
import edu.touro.mco152.bm.observers.TestObserver;
import edu.touro.mco152.bm.persist.DiskRun;
import edu.touro.mco152.bm.ui.Gui;
import edu.touro.mco152.bm.ui.MainFrame;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.Properties;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


public class ObserverTest {
    @BeforeAll
    public static void setupDefaultAsPerProperties() {
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
    NonSwingUI currentUI = new NonSwingUI();
    Executor executor = new Executor();
    static TestObserver testObserver = new TestObserver();

    @Test
    void calledObserver(){
        ReadCmd cmd = new ReadCmd(currentUI,25,128,2048, DiskRun.BlockSequence.SEQUENTIAL);
        cmd.registerObserver(testObserver);
        executor.executeCommand(cmd);
    }

    @AfterAll
    static void checkIfObserverCalled(){
        assertTrue(testObserver.isObserved());
    }

}
