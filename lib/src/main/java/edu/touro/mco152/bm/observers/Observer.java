package edu.touro.mco152.bm.observers;

import edu.touro.mco152.bm.persist.DiskRun;

/**
 * interface for all classes that will observe observables
 */
public interface Observer {
    /**
     * this will be called when an observable calls notifyObservers
     * @param run Instance of the DiskRun with information from the Read/Write
     */
    public void update(DiskRun run);

}
