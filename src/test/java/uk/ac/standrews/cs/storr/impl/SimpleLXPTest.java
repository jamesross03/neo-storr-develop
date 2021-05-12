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
import uk.ac.standrews.cs.storr.interfaces.IBucket;

import java.io.IOException;
import java.net.URISyntaxException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class SimpleLXPTest extends CommonTest {

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

    @Test
    public synchronized void testNoBucket() throws RepositoryException, IllegalKeyException, BucketException {

        assertTrue( repository.bucketExists(generic_bucket_name1) );
    }

    @Test
    public synchronized void testLXPCreation() throws RepositoryException, IllegalKeyException, BucketException {
        IBucket b = repository.getBucket(generic_bucket_name1);
        DynamicLXP lxp = new DynamicLXP();
        lxp.put("age", "42");
        lxp.put("address", "home");
        b.makePersistent(lxp);

        long id = lxp.getId();
        LXP retrievedLXP = (LXP) b.getObjectById(id);
        assertEquals(retrievedLXP, lxp);
    }
}
