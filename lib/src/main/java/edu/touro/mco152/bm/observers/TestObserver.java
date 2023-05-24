package edu.touro.mco152.bm.observers;

import edu.touro.mco152.bm.persist.DiskRun;

public class TestObserver implements Observer{
    private boolean observed = false;

    @Override
    public void update(DiskRun run) {
        observed = true;
    }

    public boolean isObserved(){
        return observed;
    }
}
