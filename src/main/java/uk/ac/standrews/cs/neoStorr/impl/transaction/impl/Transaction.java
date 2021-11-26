/*
 * Copyright 2021 Systems Research Group, University of St Andrews:
 * <https://github.com/stacs-srg>
 *
 * This file is part of the module neo-storr.
 *
 * neo-storr is free software: you can redistribute it and/or modify it under the terms of the GNU General Public
 * License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * neo-storr is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with neo-storr. If not, see
 * <http://www.gnu.org/licenses/>.
 */
package uk.ac.standrews.cs.neoStorr.impl.transaction.impl;

import org.neo4j.driver.Session;
import uk.ac.standrews.cs.neoStorr.impl.LXP;
import uk.ac.standrews.cs.neoStorr.impl.LXPMetadata;
import uk.ac.standrews.cs.neoStorr.impl.NeoBackedBucket;
import uk.ac.standrews.cs.neoStorr.impl.PersistentObject;
import uk.ac.standrews.cs.neoStorr.impl.exceptions.BucketException;
import uk.ac.standrews.cs.neoStorr.impl.exceptions.StoreException;
import uk.ac.standrews.cs.neoStorr.impl.transaction.interfaces.ITransaction;
import uk.ac.standrews.cs.neoStorr.interfaces.IBucket;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by al on May 2021
 */
public class Transaction implements ITransaction {

    private final String transaction_id;
    private final Session session;
    private final List<OverwriteRecord> undo_log = new ArrayList<>();

    private org.neo4j.driver.Transaction tx;

    Transaction(TransactionManager transaction_manager) {

        transaction_id = String.valueOf(Thread.currentThread().getId()); // TODO this is good enough for a single machine - need to do more work for multiple node support
        session = transaction_manager.getBridge().getNewSession();
        tx = session.beginTransaction();
    }

    @Override
    public synchronized void commit() throws StoreException {

        tx.commit();
        close();
    }

    @Override
    public synchronized void rollback() throws IllegalStateException {

        // TODO does it throw illegal state exception?

        tx.rollback();
        for (OverwriteRecord undo_state : undo_log) {

            LXP obj = undo_state.obj;
            NeoBackedBucket b = undo_state.bucket;
            LXPMetadata md = obj.getMetaData();

            // Replace the state with that from the store.

            try {
                PersistentObject shadow = b.loader(obj.getId());
                Map<String, Object> undo_map = shadow.serializeFieldsToMap();
                // Overwrite the fields of the in memory copy with the data from the store.

                for (Map.Entry<String, Object> entry : undo_map.entrySet()) {
                    String field_name = entry.getKey();
                    if (!field_name.equals(LXP.STORR_ID_KEY)) {
                        obj.put(md.getSlot(field_name), undo_map.get(field_name));
                    }
                }
            } catch (BucketException e) {
                throw new RuntimeException(e);
            }
        }
        close();
    }

    private void close() {

        session.close();
        tx.close();
        tx = null;
    }

    @Override
    public synchronized void add(IBucket bucket, LXP lxp) {

        // TODO exception if not active?
        if (isActive()) {
            if (!(bucket instanceof NeoBackedBucket)) {
                throw new RuntimeException("Transactions only support NeoBackedBuckets");
            }
            undo_log.add(new OverwriteRecord((NeoBackedBucket) bucket, lxp));
        }
    }

    @Override
    public boolean isActive() {
        return tx != null;
    }

    @Override
    public String getId() {
        return transaction_id;
    }

    @Override
    public org.neo4j.driver.Transaction getNeoTransaction() {
        return tx;
    }
}
