/*
 * Copyright 2017 Systems Research Group, University of St Andrews:
 * <https://github.com/stacs-srg>
 *
 * This file is part of the module storr.
 *
 * storr is free software: you can redistribute it and/or modify it under the terms of the GNU General Public
 * License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * storr is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with storr. If not, see
 * <http://www.gnu.org/licenses/>.
 */
package uk.ac.standrews.cs.storr.impl.transaction.impl;

import org.neo4j.driver.Session;
import uk.ac.standrews.cs.storr.impl.exceptions.StoreException;
import uk.ac.standrews.cs.storr.impl.transaction.exceptions.TransactionFailedException;
import uk.ac.standrews.cs.storr.impl.transaction.interfaces.ITransaction;

/**
 * Created by al on May 2021
 */
public class Transaction implements ITransaction {

    private final TransactionManager transaction_manager;
    private final String transaction_id;
    private org.neo4j.driver.Transaction tx;
    private final Session session;

    Transaction(TransactionManager transaction_manager) throws TransactionFailedException {
        this.transaction_manager = transaction_manager;
        transaction_id = Long.toString(Thread.currentThread().getId()); // TODO this is good enough for a single machine - need to do more work for multiple node support
        session = transaction_manager.getBridge().getNewSession();
        tx = session.beginTransaction();
    }

    @Override
    public void commit() throws TransactionFailedException, StoreException {
        tx.commit();
        close();
    }

    @Override
    public void rollback() throws IllegalStateException {
        tx.rollback();
        close();
    }

    private void close() {
        session.close();
        tx.close();
        tx = null;
    }

    public boolean isActive() { return tx != null; }

    public String getId() { return transaction_id; }

    @Override
    public org.neo4j.driver.Transaction getNeoTransaction() {
        return tx;
    }

}
