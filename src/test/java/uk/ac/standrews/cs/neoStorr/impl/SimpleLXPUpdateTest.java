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
package uk.ac.standrews.cs.neoStorr.impl;


import org.junit.Before;
import org.junit.Test;
import uk.ac.standrews.cs.neoStorr.impl.exceptions.BucketException;
import uk.ac.standrews.cs.neoStorr.impl.exceptions.IllegalKeyException;
import uk.ac.standrews.cs.neoStorr.impl.exceptions.RepositoryException;
import uk.ac.standrews.cs.neoStorr.impl.transaction.exceptions.TransactionFailedException;
import uk.ac.standrews.cs.neoStorr.impl.transaction.interfaces.ITransaction;
import uk.ac.standrews.cs.neoStorr.interfaces.IBucket;

import java.io.IOException;
import java.net.URISyntaxException;

import static org.junit.Assert.assertEquals;

public class SimpleLXPUpdateTest extends CommonTest {

    private static String generic_bucket_name1 = "BUCKET1";

    @Before
    public void setUp() throws RepositoryException, IOException, URISyntaxException, BucketException {

        super.setUp();

        try {
            repository.deleteBucket(generic_bucket_name1);
        } catch ( Exception e ) {
            System.out.println( "Bucket: " + generic_bucket_name1 + " did not exist before test - that is ok.");
        }

        repository.makeBucket(generic_bucket_name1);
    }

    @Test(expected=BucketException.class)
    public synchronized void testLXPIllegalUpdate() throws RepositoryException, IllegalKeyException, BucketException {
        IBucket b = repository.getBucket(generic_bucket_name1);
        DynamicLXP lxp = new DynamicLXP();
        lxp.put("age", "42");
        lxp.put("address", "home");
        b.makePersistent(lxp);

        lxp.put("age", "43");

        b.update(lxp); // should throw an exception - no transaaction
    }

    @Test
    public synchronized void testLXPLegalUpdate() throws RepositoryException, IllegalKeyException, BucketException, TransactionFailedException {
        IBucket b = repository.getBucket(generic_bucket_name1);
        DynamicLXP lxp = new DynamicLXP();
        lxp.put("age", "42");
        lxp.put("address", "home");
        b.makePersistent(lxp);

        ITransaction t = store.getTransactionManager().beginTransaction();
        lxp.put("age", "43");

        b.update(lxp);

        t.commit();

        assertEquals( lxp.get("age"),"43" );

        LXP lxp2 = (LXP) b.getObjectById(lxp.getId());

        assertEquals(lxp,lxp2);
        assertEquals( lxp2.get("age"),"43" );
    }

    @Test
    public synchronized void testLXPAbortUpdate() throws RepositoryException, IllegalKeyException, BucketException, TransactionFailedException {
        IBucket b = repository.getBucket(generic_bucket_name1);
        DynamicLXP lxp = new DynamicLXP();
        lxp.put("age", "4");
        lxp.put("address", "not home");
        b.makePersistent(lxp);

        ITransaction t = store.getTransactionManager().beginTransaction();
        lxp.put("age", "5");

        b.update(lxp);

        t.rollback();

        assertEquals( lxp.get("age"),"4" );

        LXP lxp2 = (LXP) b.getObjectById(lxp.getId());

        assertEquals(lxp,lxp2);
        assertEquals( lxp2.get("age"),"4" );
    }
}
