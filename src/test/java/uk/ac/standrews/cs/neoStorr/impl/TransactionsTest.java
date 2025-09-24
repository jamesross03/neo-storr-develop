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

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class TransactionsTest extends CommonTest {

    // Combinations to test:

    // create/update
    // auto-commit/manual commit
    // outside transaction/commit/rollback
    // single/multiple records

    // create, auto-commit, outside transaction, single record            - [outside transaction with auto-commit doesn't make sense]
    // create, auto-commit, outside transaction, multiple records         - [outside transaction with auto-commit doesn't make sense]
    // create, auto-commit, commit, single record                         - createWithAutoCommit
    // create, auto-commit, commit, multiple records                      - createMultipleRecordsWithAutoCommit
    // create, auto-commit, rollback, single record                       - [rollback with auto-commit doesn't make sense]
    // create, auto-commit, rollback, multiple records                    - [rollback with auto-commit doesn't make sense]
    // create, manual commit, outside transaction, single record          - createOutsideTransaction
    // create, manual commit, outside transaction, multiple records       - [redundant since exception is expected on first creation]
    // create, manual commit, commit, single record                       - createWithManualCommit
    // create, manual commit, commit, multiple records                    - createMultipleRecordsWithManualCommit
    // create, manual commit, rollback, single record                     - createWithRollback
    // create, manual commit, rollback, multiple records                  - [redundant since exception is expected on first access]
    // update, auto-commit, outside transaction, single record            - [outside transaction with auto-commit doesn't make sense]
    // update, auto-commit, outside transaction, multiple records         - [outside transaction with auto-commit doesn't make sense]
    // update, auto-commit, commit, single record                         - updateWithAutoCommit
    // update, auto-commit, commit, multiple records                      - updateMultipleRecordsWithAutoCommit
    // update, auto-commit, rollback, single record                       - [rollback with auto-commit doesn't make sense]
    // update, auto-commit, rollback, multiple records                    - [rollback with auto-commit doesn't make sense]
    // update, manual commit, outside transaction, single record          - updateOutsideTransaction
    // update, manual commit, outside transaction, multiple records       - [redundant since exception is expected on first update]
    // update, manual commit, commit, single record                       - updateWithManualCommit
    // update, manual commit, commit, multiple records                    - updateMultipleRecordsWithManualCommit
    // update, manual commit, rollback, single record                     - updateWithRollback
    // update, manual commit, rollback, multiple records                  - updateMultipleRecordsWithRollback

    // others:

    // createAndUpdateInSameTransaction

    private static final String NEW_BUCKET_NAME = "BUCKET_23512673";

    private IBucket<Person> bucket;
    private List<Person> people = new ArrayList<>();
    private ITransaction transaction;

    @Before
    public void setUp() throws Exception {

        super.setUp();
        store.getTransactionManager().setAutoCommit(true);
        bucket = repository.makeBucket(NEW_BUCKET_NAME, Person.class);
    }

    @After
    public void tearDown() throws RepositoryException {

        if (transaction != null && transaction.isActive()) transaction.rollback();

        repository.deleteBucket(NEW_BUCKET_NAME);
        super.tearDown();
    }

    @Test
    public void createWithAutoCommit() throws BucketException {

        store.getTransactionManager().setAutoCommit(true);
        makePersistentPerson();

        assertThatPersistentRecordsContain("John");
    }

    @Test
    public void createMultipleRecordsWithAutoCommit() throws BucketException {

        store.getTransactionManager().setAutoCommit(true);
        makePersistentPeople();

        assertThatPersistentRecordsContain("John", "Anna", "Rachel");
    }

    @Test(expected = BucketException.class)
    public void createOutsideTransaction() throws BucketException {

        store.getTransactionManager().setAutoCommit(false);
        makePersistentPerson();
    }

    @Test
    public void createWithManualCommit() throws Exception {

        store.getTransactionManager().setAutoCommit(false);
        transaction = store.getTransactionManager().beginTransaction();

        makePersistentPerson();
        transaction.commit();

        assertThatPersistentRecordsContain("John");
    }

    @Test
    public void createMultipleRecordsWithManualCommit() throws Exception {

        store.getTransactionManager().setAutoCommit(false);
        transaction = store.getTransactionManager().beginTransaction();

        makePersistentPeople();
        transaction.commit();

        assertThatPersistentRecordsContain("John", "Anna", "Rachel");
    }

    @Test(expected = BucketException.class)
    public void createWithRollback() throws Exception {

        store.getTransactionManager().setAutoCommit(false);
        transaction = store.getTransactionManager().beginTransaction();

        makePersistentPerson();
        transaction.rollback();

        bucket.invalidateCache();
        bucket.getObjectById(people.get(0).getId());
    }

    @Test
    public void updateWithAutoCommit() throws Exception {

        store.getTransactionManager().setAutoCommit(true);
        makePersistentPerson();

        updatePerson();

        assertThatInMemoryRecordsContain("Fred");
        assertThatPersistentRecordsContain("Fred");
    }

    @Test
    public void updateMultipleRecordsWithAutoCommit() throws Exception {

        store.getTransactionManager().setAutoCommit(true);
        makePersistentPeople();

        updatePeople();

        assertThatInMemoryRecordsContain("Fred", "Jean", "Sian");
        assertThatPersistentRecordsContain("Fred", "Jean", "Sian");
    }

    @Test(expected = BucketException.class)
    public void updateOutsideTransaction() throws Exception {

        store.getTransactionManager().setAutoCommit(false);
        makePersistentPerson();

        updatePerson();
    }

    @Test
    public void updateWithManualCommit() throws Exception {

        store.getTransactionManager().setAutoCommit(true);
        makePersistentPerson();

        store.getTransactionManager().setAutoCommit(false);
        transaction = store.getTransactionManager().beginTransaction();

        updatePerson();

        assertThatInMemoryRecordsContain("Fred");
        assertThatPersistentRecordsContain("Fred");

        bucket.invalidateCache();

        assertThatInMemoryRecordsContain("Fred");
        assertThatPersistentRecordsContain("John");

        transaction.commit();

        bucket.invalidateCache();

        assertThatInMemoryRecordsContain("Fred");
        assertThatPersistentRecordsContain("Fred");
    }

    @Test
    public void updateMultipleRecordsWithManualCommit() throws Exception {

        store.getTransactionManager().setAutoCommit(true);
        makePersistentPeople();

        store.getTransactionManager().setAutoCommit(false);
        transaction = store.getTransactionManager().beginTransaction();

        updatePeople();

        assertThatInMemoryRecordsContain("Fred", "Jean", "Sian");
        assertThatPersistentRecordsContain("Fred", "Jean", "Sian");

        bucket.invalidateCache();

        assertThatInMemoryRecordsContain("Fred", "Jean", "Sian");
        assertThatPersistentRecordsContain("John", "Anna", "Rachel");

        transaction.commit();

        bucket.invalidateCache();

        assertThatInMemoryRecordsContain("Fred", "Jean", "Sian");
        assertThatPersistentRecordsContain("Fred", "Jean", "Sian");
    }

    @Test
    public void updateWithRollback() throws Exception {

        store.getTransactionManager().setAutoCommit(true);
        makePersistentPerson();

        store.getTransactionManager().setAutoCommit(false);
        transaction = store.getTransactionManager().beginTransaction();

        updatePerson();

        transaction.rollback();

        assertThatInMemoryRecordsContain("John");
        assertThatPersistentRecordsContain("John");
    }

    @Test
    public void updateMultipleRecordsWithRollback() throws Exception {

        store.getTransactionManager().setAutoCommit(true);
        makePersistentPeople();

        store.getTransactionManager().setAutoCommit(false);
        transaction = store.getTransactionManager().beginTransaction();

        updatePeople();

        transaction.rollback();

        assertThatInMemoryRecordsContain("John", "Anna", "Rachel");
        assertThatPersistentRecordsContain("John", "Anna", "Rachel");
    }

    @Test
    public void createAndUpdateInSameTransaction() throws Exception {

        store.getTransactionManager().setAutoCommit(false);
        transaction = store.getTransactionManager().beginTransaction();

        makePersistentPerson();

        updatePerson();

        transaction.commit();

        assertThatInMemoryRecordsContain("Fred");
        assertThatPersistentRecordsContain("Fred");
    }

    private void makePersistentPerson() throws BucketException {

        Person person = new Person("John", "Smith");
        bucket.makePersistent(person);
        people.add(person);
    }

    private void makePersistentPeople() throws BucketException {

        makePersistentPerson();

        Person person2 = new Person("Anna", "Jones");
        bucket.makePersistent(person2);
        people.add(person2);

        Person person3 = new Person("Rachel", "McDonald");
        bucket.makePersistent(person3);
        people.add(person3);
    }

    private void updatePerson() throws BucketException {

        people.get(0).put(Person.FORENAME, "Fred");
        bucket.update(people.get(0));
    }

    private void updatePeople() throws BucketException {

        people.get(0).put(Person.FORENAME, "Fred");
        people.get(1).put(Person.FORENAME, "Jean");
        people.get(2).put(Person.FORENAME, "Sian");
        bucket.update(people.get(0));
        bucket.update(people.get(1));
        bucket.update(people.get(2));
    }

    private void assertThatInMemoryRecordsContain(String... values) {

        for (int i = 0; i < values.length; i++) {
            assertEquals(values[i], people.get(i).get(Person.FORENAME));
        }
    }

    private void assertThatPersistentRecordsContain(String... values) throws BucketException {

        for (int i = 0; i < values.length; i++) {
            assertEquals(values[i], bucket.getObjectById(people.get(i).getId()).get(Person.FORENAME));
        }
    }
}
