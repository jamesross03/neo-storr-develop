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
package uk.ac.standrews.cs.neoStorr.impl;


import org.junit.Before;
import org.junit.Test;
import uk.ac.standrews.cs.neoStorr.impl.exceptions.BucketException;
import uk.ac.standrews.cs.neoStorr.impl.exceptions.IllegalKeyException;
import uk.ac.standrews.cs.neoStorr.impl.exceptions.RepositoryException;
import uk.ac.standrews.cs.neoStorr.interfaces.IBucket;

import java.io.IOException;
import java.net.URISyntaxException;

import static org.junit.Assert.assertTrue;

public class StoreInegrityTest extends CommonTest {

    private static String generic_bucket_name1 = "BUCKET1";
    private IBucket b;
    private BBB al;
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

    @Test
    public synchronized void testBucketIntegrity() throws RepositoryException, IllegalKeyException, BucketException {
        assertTrue( b.getName().equals("BUCKET1"));
        assertTrue( repository.getBucket("BUCKET1").equals(b));
    }

    @Test
    public synchronized void testRepoIntegrity() throws RepositoryException, IllegalKeyException, BucketException {
        repository.getName().equals("TEST_REPO");
        assertTrue( store.getRepository("TEST_REPO").equals(repository));
    }


}
