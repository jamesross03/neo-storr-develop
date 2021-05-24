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
import uk.ac.standrews.cs.neoStorr.impl.exceptions.BucketException;
import uk.ac.standrews.cs.neoStorr.impl.exceptions.IllegalKeyException;
import uk.ac.standrews.cs.neoStorr.impl.exceptions.RepositoryException;
import uk.ac.standrews.cs.neoStorr.interfaces.IBucket;

import java.io.IOException;

import static org.junit.Assert.assertEquals;

public class JPOTypedBucketTest extends CommonTest {

    static final String JPO_BUCKET_NAME = "JPOBucket";

    @Test
    public synchronized void testJPOCreation() throws RepositoryException, IllegalKeyException, BucketException, IOException {

        if( ! repository.bucketExists(JPO_BUCKET_NAME)) {
            repository.makeBucket(JPO_BUCKET_NAME, Person.class);
        }

        final IBucket<Person> bucket = repository.getBucket(JPO_BUCKET_NAME, Person.class);

        final Person p1 = new Person(42, "home");

        bucket.makePersistent(p1);

        final long id = p1.getId();
        final Person p2 = bucket.getObjectById(id);
        assertEquals(p2, p1);


    }
}
