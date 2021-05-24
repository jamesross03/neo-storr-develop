/*
 * Copyright 2017 Systems Research Group, University of St Andrews:
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
package uk.ac.standrews.cs.neoStorr.impl;


import org.junit.Before;
import org.junit.Test;
import uk.ac.standrews.cs.neoStorr.impl.exceptions.BucketException;
import uk.ac.standrews.cs.neoStorr.impl.exceptions.IllegalKeyException;
import uk.ac.standrews.cs.neoStorr.impl.exceptions.PersistentObjectException;
import uk.ac.standrews.cs.neoStorr.impl.exceptions.RepositoryException;
import uk.ac.standrews.cs.neoStorr.impl.transaction.exceptions.TransactionFailedException;
import uk.ac.standrews.cs.neoStorr.interfaces.IBucket;

import java.io.IOException;
import java.net.URISyntaxException;

import static org.junit.Assert.assertTrue;

public class RefTest extends CommonTest {

    private static String generic_bucket_name1 = "BUCKET1";
    private static String births_bucket_name = "Births_ref_test";
    private static String static_lxp_bucket_name = "StaticLXPBucket";
    private IBucket<BBB> b;
    private IBucket d;
    private IBucket<AClassContainingBBBRef> e;

    @Before
    public void setUp() throws RepositoryException, IOException, URISyntaxException, BucketException {

        super.setUp();

        System.out.println( "This test uses existing buckets if they exist and doesn't clean up!");
        try {
            b = repository.getBucket(births_bucket_name, BBB.class);
        } catch( RepositoryException e ) {
            b = repository.makeBucket(births_bucket_name, BBB.class);
        }
        try {
            d = repository.getBucket(generic_bucket_name1);
        } catch( RepositoryException e ) {
            d = repository.makeBucket(generic_bucket_name1);
        }
        try {
            e = repository.getBucket(static_lxp_bucket_name, AClassContainingBBBRef.class);
        } catch( RepositoryException ex ) {
            e = repository.makeBucket(static_lxp_bucket_name, AClassContainingBBBRef.class);
        }
    }


    @Test
    public synchronized void testCreateDynamicLXPWithRef() throws RepositoryException, IllegalKeyException, BucketException, TransactionFailedException, PersistentObjectException {

        BBB al = new BBB("Al", "Dearle");
        b.makePersistent( al );
        long al_id = al.getId();

        DynamicLXPWithRef cwr = new DynamicLXPWithRef(al);
        long cwr_id = cwr.getId();

        d.makePersistent(cwr);

        assertTrue( b.contains(al_id));
        assertTrue( d.contains(cwr_id));
    }


    @Test
    public synchronized void testCreateStaticLXPObjectWithRef() throws RepositoryException, IllegalKeyException, BucketException, TransactionFailedException, PersistentObjectException {

        BBB graham = new BBB("Graham", "Dearle");
        b.makePersistent( graham );
        long graham_id = graham.getId();

        AClassContainingBBBRef slwr = new AClassContainingBBBRef(graham);
        long slwr_id = slwr.getId();

        e.makePersistent(slwr);
        System.out.println( "ID OF AClassContainingBBBRef IS: " + slwr_id );

        assertTrue( b.contains(graham_id));
        assertTrue( e.contains(slwr_id));

    }

}
