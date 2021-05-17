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
import uk.ac.standrews.cs.neoStorr.interfaces.IBucket;
import uk.ac.standrews.cs.neoStorr.interfaces.IInputStream;
import uk.ac.standrews.cs.neoStorr.interfaces.IOutputStream;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class StaticLXPStreamsTest extends CommonTest {

    private static String generic_bucket_name1 = "BUCKET1";
    private IBucket<Birth> b;

    @Before
    public void setUp() throws RepositoryException, IOException, URISyntaxException, BucketException {

        super.setUp();

        try {
            repository.deleteBucket(generic_bucket_name1);
        } catch ( Exception e ) {
            System.out.println( "Bucket: " + generic_bucket_name1 + " did not exist before test - that is ok.");
        }

        b = repository.makeBucket(generic_bucket_name1,Birth.class);
    }

    @Test
    public synchronized void testInputStream() throws RepositoryException, IllegalKeyException, BucketException {

        Set<Birth> birth_set = new HashSet<>();

        for( int i = 0; i < 10; i++ ) {
            Birth birth = new Birth();
            birth.put(Birth.FORENAME, Integer.toString(i));
            birth.put(Birth.SURNAME, "Input");
            b.makePersistent(birth);
            birth_set.add(birth);
        }
        IInputStream<Birth> in_stream = b.getInputStream();
        int i = 0;
        for( Birth b : in_stream ) {
            assertTrue( birth_set.contains(b) );
            i++;
        }
        assertEquals( i, birth_set.size() );
    }

    @Test
    public synchronized void testOutputStream() throws RepositoryException, IllegalKeyException, BucketException {

        Set<Birth> birth_set = new HashSet<>();

        IOutputStream<Birth> out_stream = b.getOutputStream();

        for( int i = 0; i < 10; i++ ) {
            Birth birth = new Birth();
            birth.put(Birth.FORENAME, Integer.toString(i));
            birth.put(Birth.SURNAME, "Output");
            out_stream.add(birth);
            birth_set.add(birth);
        }

        for( Birth birth : birth_set ) {
            assertTrue( b.contains( birth.getId() ) );
        }
    }

}
