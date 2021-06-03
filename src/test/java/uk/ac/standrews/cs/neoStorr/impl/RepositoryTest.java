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

import org.junit.Test;
import uk.ac.standrews.cs.neoStorr.impl.exceptions.RepositoryException;
import uk.ac.standrews.cs.neoStorr.interfaces.IBucket;
import uk.ac.standrews.cs.neoStorr.interfaces.IRepository;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

import static org.junit.Assert.*;
import static uk.ac.standrews.cs.neoStorr.impl.Repository.bucketNameIsLegal;

/**
 * @author Simone I. Conte "sic2@st-andrews.ac.uk"
 */
public class RepositoryTest extends CommonTest {

    private static final String UNIQUE_BUCKET_NAME = Double.toString( new Random().nextDouble() );
    private static final List<String> LEGAL_NAMES = Arrays.asList("bucket", "a bucket");
    private static final List<String> ILLEGAL_NAMES = Arrays.asList("a: bucket","a/bucket","a<bucket","a\\bucket?");

    @Test
    public void checkRepoExistsTest() throws RepositoryException {

        assertTrue(store.repositoryExists(REPOSITORY_NAME));
    }

    @Test
    public void createBucketTest() throws RepositoryException {

        repository.makeBucket(UNIQUE_BUCKET_NAME);

        assertTrue(repository.bucketExists(UNIQUE_BUCKET_NAME));
        assertEquals(UNIQUE_BUCKET_NAME, repository.getBucket(UNIQUE_BUCKET_NAME).getName());

        repository.deleteBucket(UNIQUE_BUCKET_NAME);

        assertFalse(repository.bucketExists(UNIQUE_BUCKET_NAME));
    }

    @Test
    public void nameLegalityTest() {

        for (String name : LEGAL_NAMES) {
            assertTrue(bucketNameIsLegal(name));
        }

        for (String name : ILLEGAL_NAMES) {
            assertFalse(bucketNameIsLegal(name));
        }
    }

    @Test
    public void createDeleteEmptyRepo() throws RepositoryException {

        store.makeRepository( "REPO65981737563412");
        assertTrue( store.repositoryExists("REPO65981737563412"));
        store.deleteRepository("REPO65981737563412");
        assertFalse( store.repositoryExists("REPO65981737563412"));
    }

    @Test
    public void createDeleteRepoWithContent() throws RepositoryException {

        IRepository repo = store.makeRepository("REPO65983317363412");
        assertTrue( store.repositoryExists("REPO65983317363412"));
        IBucket bucket = repo.makeBucket("BUCKET327823978");
        store.deleteRepository("REPO65983317363412");
        assertFalse( store.repositoryExists("REPO65983317363412"));
    }
}