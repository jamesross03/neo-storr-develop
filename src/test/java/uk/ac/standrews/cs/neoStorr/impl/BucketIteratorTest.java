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

import org.junit.Test;
import uk.ac.standrews.cs.neoStorr.impl.exceptions.RepositoryException;
import uk.ac.standrews.cs.neoStorr.interfaces.IBucket;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import static org.junit.Assert.*;

/**
 * @author Simone I. Conte "sic2@st-andrews.ac.uk"
 */
public class BucketIteratorTest extends CommonTest {

    private static final String UNIQUE_BUCKET_NAME1 = Double.toString( new Random().nextDouble() );
    private static final String UNIQUE_BUCKET_NAME2 = Double.toString( new Random().nextDouble() );
    private static final String UNIQUE_BUCKET_NAME3 = Double.toString( new Random().nextDouble() );
    private static final String UNIQUE_BUCKET_NAME4 = Double.toString( new Random().nextDouble() );


    @Test
    public void checkRepoExistsTest() throws RepositoryException {

        assertTrue(store.repositoryExists(REPOSITORY_NAME));
    }

    @Test
    public void nameIteratorTest() throws RepositoryException {

        store = new Store();

        repository = store.makeRepository("ITER523463645754");

        repository.makeBucket(UNIQUE_BUCKET_NAME1);
        repository.makeBucket(UNIQUE_BUCKET_NAME2);
        repository.makeBucket(UNIQUE_BUCKET_NAME3);
        repository.makeBucket(UNIQUE_BUCKET_NAME4);

        List<String> seen = new ArrayList<>();

        Iterator<String> iter = repository.getBucketNameIterator();
        while( iter.hasNext() ) {
            String name = iter.next();
            assertFalse( seen.contains( name ) );
            seen.add(name);
            assertTrue( name.equals(UNIQUE_BUCKET_NAME1) || name.equals(UNIQUE_BUCKET_NAME2) || name.equals(UNIQUE_BUCKET_NAME3) || name.equals(UNIQUE_BUCKET_NAME4) );
        }
        assertEquals( seen.size(), 4 );

        repository.deleteBucket(UNIQUE_BUCKET_NAME1);
        repository.deleteBucket(UNIQUE_BUCKET_NAME2);
        repository.deleteBucket(UNIQUE_BUCKET_NAME3);
        repository.deleteBucket(UNIQUE_BUCKET_NAME4);

        store.deleteRepository("ITER523463645754");

    }

    @Test
    public void repoIteratorTest() throws IOException, RepositoryException {

        store = new Store();

        repository = store.makeRepository("ITER523463645754");

        repository.makeBucket(UNIQUE_BUCKET_NAME1, BBB.class);
        repository.makeBucket(UNIQUE_BUCKET_NAME2, BBB.class);
        repository.makeBucket(UNIQUE_BUCKET_NAME3, BBB.class);
        repository.makeBucket(UNIQUE_BUCKET_NAME4, DDD.class);

        List<String> seen = new ArrayList<>();

        Iterator<IBucket<BBB>> iter = repository.getIterator(BBB.class);

        while (iter.hasNext()) {
            IBucket<BBB> b = iter.next();
            String name = b.getName();
            assertFalse(seen.contains(name));
            seen.add(name);
            assertTrue(name.equals(UNIQUE_BUCKET_NAME1) || name.equals(UNIQUE_BUCKET_NAME2) || name.equals(UNIQUE_BUCKET_NAME3));
        }
        assertEquals(seen.size(), 3); // The DDD should not match

        repository.deleteBucket(UNIQUE_BUCKET_NAME1);
        repository.deleteBucket(UNIQUE_BUCKET_NAME2);
        repository.deleteBucket(UNIQUE_BUCKET_NAME3);
        repository.deleteBucket(UNIQUE_BUCKET_NAME4);

        store.deleteRepository("ITER523463645754");
    }
}