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

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import uk.ac.standrews.cs.neoStorr.impl.exceptions.BucketException;
import uk.ac.standrews.cs.neoStorr.impl.exceptions.RepositoryException;
import uk.ac.standrews.cs.neoStorr.impl.testData.Person;
import uk.ac.standrews.cs.neoStorr.impl.transaction.interfaces.ITransaction;
import uk.ac.standrews.cs.neoStorr.interfaces.IBucket;

import static org.junit.Assert.*;

public class TransactionsTest extends CommonTest {

    private static final String NEW_BUCKET_NAME = "BUCKET_23512673";

    private IBucket<Person> bucket;
    private Person p;

    @Before
    public void setUp() throws Exception {

        super.setUp();
        bucket = repository.makeBucket(NEW_BUCKET_NAME, Person.class);

        p = new Person();
        p.put(Person.FORENAME, "Old");
        p.put(Person.SURNAME, "Smith");
        bucket.makePersistent(p);
     }

    @After
    public void tearDown() throws RepositoryException {

        repository.deleteBucket(NEW_BUCKET_NAME);
        super.tearDown();
    }

    @Test
    public synchronized void updateWithinTransaction() throws Exception {

        store.getTransactionManager().beginTransaction();

        p.put(Person.FORENAME, "New");
        bucket.update(p);

        assertThatInMemoryRecordContains("New");
        assertThatPersistentRecordContains("New");

        bucket.invalidateCache();

        assertThatInMemoryRecordContains("New");
        assertThatPersistentRecordContains("Old");
    }

    @Test(expected = BucketException.class)
    public synchronized void updateOutsideTransaction() throws Exception {

        p.put(Person.FORENAME, "New");
        bucket.update(p);
    }

    @Test
    public synchronized void commit() throws Exception {

        ITransaction t = store.getTransactionManager().beginTransaction();

        p.put(Person.FORENAME, "New");
        bucket.update(p);

        t.commit();

        assertThatInMemoryRecordContains("New");
        assertThatPersistentRecordContains("New");
    }

    @Test
    public synchronized void rollback() throws Exception {

        ITransaction t = store.getTransactionManager().beginTransaction();

        p.put(Person.FORENAME, "New");
        bucket.update(p);

        t.rollback();

        assertThatInMemoryRecordContains("Old");
        assertThatPersistentRecordContains("Old");
    }

    private void assertThatInMemoryRecordContains(String value) {
        assertEquals(value, p.get(Person.FORENAME));
    }

    private void assertThatPersistentRecordContains(String value) throws uk.ac.standrews.cs.neoStorr.impl.exceptions.BucketException {
        assertEquals(value, bucket.getObjectById(p.getId()).get(Person.FORENAME));
    }
}
