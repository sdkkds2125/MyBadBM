package edu.touro.mco152.bm.observers;

/**
 * Interface for all classes that will be observed
 */
public interface Observable {
    /**
     * adds an observer to this subjects list of observers
     * @param observer
     */
    public void registerObserver(Observer observer);

    /**
     * This removes the passed in observer from the list
     * @param observer
     */
    public void removeObserver(Observer observer);

    /**
     * This loops through all observers on the list and call their update methods
     */
    public void notifyObservers();

}
