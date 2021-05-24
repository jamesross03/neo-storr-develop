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
import uk.ac.standrews.cs.neoStorr.impl.exceptions.RepositoryException;
import uk.ac.standrews.cs.neoStorr.interfaces.IBucket;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertTrue;

public class StaticGetOidsTest extends CommonTest {

    private static String generic_bucket_name1 = "BUCKET1";
    private IBucket<BBB> b;

    @Before
    public void setUp() throws RepositoryException, IOException, URISyntaxException, BucketException {

        super.setUp();

        try {
            repository.deleteBucket(generic_bucket_name1);
        } catch ( Exception e ) {
            System.out.println( "Bucket: " + generic_bucket_name1 + " did not exist before test - that is ok.");
        }

        b = repository.makeBucket(generic_bucket_name1, BBB.class);
    }


    @Test
    public synchronized void testGetOids() throws RepositoryException, IllegalKeyException, BucketException {

        Set<Long> created_oids = new HashSet<>();

        for( int i = 0; i < 10; i++ ) {
            BBB birth = new BBB();
            birth.put(BBB.FORENAME, Integer.toString(i));
            birth.put(BBB.SURNAME, "Dearle");
            created_oids.add( birth.getId() );
            b.makePersistent(birth);
        }

        List<Long> stored_oids = b.getOids();

        assertTrue( stored_oids.containsAll( created_oids ) );
        assertTrue( created_oids.containsAll( stored_oids ) );
    }

}
