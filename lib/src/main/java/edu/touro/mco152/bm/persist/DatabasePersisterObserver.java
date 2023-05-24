package edu.touro.mco152.bm.persist;

import edu.touro.mco152.bm.observers.Observer;
import jakarta.persistence.EntityManager;

public class DatabasePersisterObserver implements Observer {
    /*
       Persist info about the Read or Write BM Run (e.g. into Derby Database)
     */
    @Override
    public void update(DiskRun run) {
        EntityManager em = EM.getEntityManager();
        em.getTransaction().begin();
        em.persist(run);
        em.getTransaction().commit();
    }
}
