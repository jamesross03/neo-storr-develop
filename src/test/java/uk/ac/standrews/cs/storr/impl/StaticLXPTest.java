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
package uk.ac.standrews.cs.storr.impl;


import org.junit.Before;
import org.junit.Test;
import uk.ac.standrews.cs.storr.impl.exceptions.BucketException;
import uk.ac.standrews.cs.storr.impl.exceptions.IllegalKeyException;
import uk.ac.standrews.cs.storr.impl.exceptions.RepositoryException;
import uk.ac.standrews.cs.storr.impl.transaction.exceptions.TransactionFailedException;
import uk.ac.standrews.cs.storr.impl.transaction.interfaces.ITransaction;
import uk.ac.standrews.cs.storr.interfaces.IBucket;

import java.io.IOException;
import java.net.URISyntaxException;

import static org.junit.Assert.*;

public class StaticLXPTest extends CommonTest {

    private static String generic_bucket_name1 = "BUCKET1";
    private IBucket b;
    private Birth al;
    private long al_id;

    @Before
    public void setUp() throws RepositoryException, IOException, URISyntaxException, BucketException {

        super.setUp();

        try {
            repository.deleteBucket(generic_bucket_name1);
        } catch ( Exception e ) {
            System.out.println( "Bucket: " + generic_bucket_name1 + " did not exist before test - that is ok.");
        }

        b = repository.makeBucket(generic_bucket_name1);
    }


    public void tearDown() throws BucketException {
//        System.out.println( "Als id = " + al.getId() );
//        b.delete( al_id );
//        assertFalse( b.contains( al_id ) );
    }

    @Test
    public synchronized void testStaticLXPCreation() throws RepositoryException, IllegalKeyException, BucketException {
        al = new Birth();
        al.put( Birth.FORENAME,"Al" );
        al.put( Birth.SURNAME,"Dearle" );
        b.makePersistent( al );
        al_id = al.getId();
        assertTrue( b.contains( al_id ) );
    }

    @Test
    public synchronized void testStaticLXPCreateDelete() throws RepositoryException, IllegalKeyException, BucketException {
        al = new Birth();
        al.put( Birth.FORENAME,"Al" );
        al.put( Birth.SURNAME,"Dearle" );
        b.makePersistent( al );
        al_id = al.getId();
        assertTrue( b.contains( al_id ) );
        b.delete(al_id);
        assertFalse( b.contains( al_id ) );
    }

    @Test
    public synchronized void testStaticUpdate() throws RepositoryException, IllegalKeyException, BucketException, TransactionFailedException {

        al = new Birth();
        al.put( Birth.FORENAME,"Al" );
        al.put( Birth.SURNAME,"Dearle" );
        b.makePersistent( al );
        al_id = al.getId();

        ITransaction t = store.getTransactionManager().beginTransaction();
        al.put( Birth.FORENAME,"Alan");
        b.update(al);
        t.commit();
        assertEquals( al.get(Birth.FORENAME),"Alan" );
        //---------
        ITransaction t2 = store.getTransactionManager().beginTransaction();
        al.put( Birth.FORENAME,"Al");
        b.update(al);
        t2.commit();
        assertEquals( al.get(Birth.FORENAME),"Al" );
    }

    @Test
    public synchronized void testStaticAbort() throws RepositoryException, IllegalKeyException, BucketException, TransactionFailedException {

        al = new Birth();
        al.put( Birth.FORENAME,"Al" );
        al.put( Birth.SURNAME,"Dearle" );
        b.makePersistent( al );

        ITransaction t = store.getTransactionManager().beginTransaction();
        al.put( Birth.FORENAME,"Graham");
        b.update(al);
        t.rollback();
        assertEquals( al.get(Birth.FORENAME),"Al" );
    }



}
