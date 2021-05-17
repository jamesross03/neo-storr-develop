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
import uk.ac.standrews.cs.neoStorr.interfaces.IRepository;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Random;

import static org.junit.Assert.assertFalse;

public class DeleteRepoTest extends CommonTest {

    private static String repo_name1 = Double.toString( new Random().nextDouble() );
    private static String bucket_name1 = Double.toString( new Random().nextDouble() );
    private static String bucket_name2 = Double.toString( new Random().nextDouble() );

    @Before
    public void setUp() throws RepositoryException, IOException, URISyntaxException, BucketException {

        super.setUp();
    }

    @Test
    public synchronized void testRepoCreateAndDelete() throws RepositoryException, IllegalKeyException, BucketException {

        IRepository repo = store.makeRepository(repo_name1);
        repo.makeBucket(bucket_name1);
        repo.makeBucket(bucket_name2);

        store.deleteRepository(repo_name1);

        assertFalse( store.repositoryExists(repo_name1) );

        // should be able to recreate that again:

        repo = store.makeRepository(repo_name1);
        repo.makeBucket(bucket_name1);
        repo.makeBucket(bucket_name2);

        store.deleteRepository(repo_name1);

        assertFalse( store.repositoryExists(repo_name1) );
    }



}
