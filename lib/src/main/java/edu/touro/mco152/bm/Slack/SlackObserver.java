package edu.touro.mco152.bm.Slack;

import edu.touro.mco152.bm.observers.Observer;
import edu.touro.mco152.bm.persist.DiskRun;

/*
 This class is an observer that will send a slack message when notified
 and the conditions put forth by the meetsRules method are met
 */
public class SlackObserver implements Observer {


    @Override
    public void update(DiskRun run) {
        if (meetsRules(run)) {
            sendMessage();
        }
    }

    /*
    Sends a slack message
     */
    private void sendMessage() {
        SlackManager slackManager = new SlackManager("BadBm");
        slackManager.postMsg2OurChannel("The max time of the Read benchmark was more than 3% of the benchmarks's average time");
    }

    private boolean meetsRules(DiskRun run) {
        return isPercentOfMaxOverAverage3(run)  && (run.getIoMode() == DiskRun.IOMode.READ);
    }

    /*
    Checks if the max run is more than 3% of the run avg
     */
    private boolean isPercentOfMaxOverAverage3(DiskRun run) {
        return (run.getRunMax() > (run.getRunAvg() * 1.03));
    }
}

