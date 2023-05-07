package edu.touro.mco152.bm;

import edu.touro.mco152.bm.persist.DiskRun;

import java.io.File;
import java.util.ArrayList;

/**
 * This class is used to collect DiskRun data generated when the benchmark is run for testing purposes
 * @see DiskWorker
 */

public class PersonalDataLog {
    private static ArrayList<Long> appInfo = new ArrayList<>();
    private static String utilDiskInfoFile;
    private static ArrayList<Long> diskRunInfo = new ArrayList<>();
    private static String diskRunInfoFile;
    private static DiskRun run;


    public static ArrayList<Long> getAppInfo() {
        return appInfo;
    }

    public static DiskRun getRun() {
        return run;
    }

    public static void setRun(DiskRun run) {
        PersonalDataLog.run = run;
    }

    public void addToAppInfo(Long info){
        appInfo.add(info);
    }

    public static ArrayList<Long> getDiskRunInfo() {
        return diskRunInfo;
    }

    public void addToDiskRunInfo(Long info){
        diskRunInfo.add(info);
    }

    public static String getDiskRunInfoFile() {
        return diskRunInfoFile;
    }

    public void setDiskRunInfoFile(String diskRunInfoFile) {
        this.diskRunInfoFile = diskRunInfoFile;
    }

    public static String getUtilDiskInfoFile() {
        return utilDiskInfoFile;
    }

    public void setUtilDiskInfoFile(String utilDiskInfoFile){
        this.utilDiskInfoFile = utilDiskInfoFile;
    }
}
