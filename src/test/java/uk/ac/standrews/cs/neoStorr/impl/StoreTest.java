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
import uk.ac.standrews.cs.neoStorr.impl.testData.Car;
import uk.ac.standrews.cs.neoStorr.impl.testData.JPOPerson;
import uk.ac.standrews.cs.neoStorr.impl.testData.Person;
import uk.ac.standrews.cs.neoStorr.interfaces.IBucket;
import uk.ac.standrews.cs.neoStorr.interfaces.IOutputStream;
import uk.ac.standrews.cs.neoStorr.interfaces.IRepository;

import java.util.*;

import static org.junit.Assert.*;

public class StoreTest extends CommonTest {

    private static final List<String> LEGAL_NAMES = Arrays.asList("bucket", "a bucket");
    private static final List<String> ILLEGAL_NAMES = Arrays.asList("a: bucket", "a/bucket", "a<bucket", "a\\bucket?");

    private static final String NEW_REPOSITORY_NAME = "REPO_2324634";
    private static final String NEW_BUCKET_NAME = "BUCKET_837463";

    private static final List<String> BUCKET_NAMES = Arrays.asList("345324", "938i9457349", "384756234", "83457");

    @Test
    public void createAndDeleteRepository() throws RepositoryException {

        assertFalse(store.repositoryExists(NEW_REPOSITORY_NAME));
        final IRepository repository = store.makeRepository(NEW_REPOSITORY_NAME);

        assertTrue(store.repositoryExists(NEW_REPOSITORY_NAME));
        assertEquals(NEW_REPOSITORY_NAME, repository.getName());
        assertEquals(store.getRepository(NEW_REPOSITORY_NAME), repository);

        store.deleteRepository(NEW_REPOSITORY_NAME);
        assertFalse(store.repositoryExists(NEW_REPOSITORY_NAME));
    }

    @Test
    public void createAndDeleteRepositoryWithContent() throws Exception {

        final IRepository repository = store.makeRepository(NEW_REPOSITORY_NAME);
        repository.makeBucket(BUCKET_NAME);

        assertTrue(store.repositoryExists(NEW_REPOSITORY_NAME));
        store.deleteRepository(NEW_REPOSITORY_NAME);
        assertFalse(store.repositoryExists(NEW_REPOSITORY_NAME));
    }

    @Test
    public synchronized void createAndDeleteBucket() throws Exception {

        assertFalse(repository.bucketExists(NEW_BUCKET_NAME));
        final IBucket bucket = repository.makeBucket(NEW_BUCKET_NAME);

        assertTrue(repository.bucketExists(NEW_BUCKET_NAME));
        assertEquals(NEW_BUCKET_NAME, bucket.getName());
        assertEquals(repository.getBucket(NEW_BUCKET_NAME), bucket);

        repository.deleteBucket(NEW_BUCKET_NAME);
        assertFalse(repository.bucketExists(NEW_BUCKET_NAME));
    }

    @Test
    public synchronized void createAndDeleteDynamicLXP() throws Exception {

        final DynamicLXP lxp = new DynamicLXP();
        final long id = lxp.getId();
        lxp.put("age", "42");
        lxp.put("address", "home");

        final IBucket bucket = repository.getBucket(BUCKET_NAME);
        bucket.makePersistent(lxp);

        final LXP retrieved = (LXP) bucket.getObjectById(id);
        assertEquals(retrieved, lxp);
        assertTrue(bucket.contains(id));

        bucket.delete(id);
        assertFalse(bucket.contains(id));
    }

    @Test
    public synchronized void createAndDeleteStaticLXP() throws Exception {

        final IBucket<Person> bucket = repository.makeBucket(NEW_BUCKET_NAME, Person.class);

        final Person al = new Person();
        final long id = al.getId();
        al.put(Person.FORENAME, "Al");
        al.put(Person.SURNAME, "Dearle");


        bucket.makePersistent(al);

        final LXP retrieved = bucket.getObjectById(al.getId());
        assertEquals(retrieved, al);
        assertTrue(bucket.contains(id));

        bucket.delete(id);
        assertFalse(bucket.contains(id));

        repository.deleteBucket(NEW_BUCKET_NAME);
    }

    @Test
    public synchronized void createAndDeleteJPO() throws Exception {

        final IBucket<JPOPerson> bucket = repository.makeBucket(NEW_BUCKET_NAME, JPOPerson.class);

        final JPOPerson p1 = new JPOPerson(42, "home");
        final long id = p1.getId();
        bucket.makePersistent(p1);

        final JPOPerson retrieved = bucket.getObjectById(p1.getId());
        assertEquals(retrieved, p1);
        assertTrue(bucket.contains(id));

        bucket.delete(id);
        assertFalse(bucket.contains(id));

        repository.deleteBucket(NEW_BUCKET_NAME);
    }

    @Test
    public void iterateOverUntypedBuckets() throws RepositoryException {

        for (String bucket_name : BUCKET_NAMES) {
            repository.makeBucket(bucket_name);
        }

        List<String> seen = new ArrayList<>();

        Iterator<String> iter = repository.getBucketNameIterator();
        while (iter.hasNext()) {
            String name = iter.next();
            assertFalse(seen.contains(name));
            seen.add(name);
        }

        for (String bucket_name : BUCKET_NAMES) {
            assertTrue(seen.contains(bucket_name));
            repository.deleteBucket(bucket_name);
        }
    }

    @Test
    public void iterateOverTypedBuckets() throws Exception {

        repository.makeBucket(BUCKET_NAMES.get(0), Person.class);
        repository.makeBucket(BUCKET_NAMES.get(1), Person.class);
        repository.makeBucket(BUCKET_NAMES.get(2), Person.class);
        repository.makeBucket(BUCKET_NAMES.get(3), Car.class);

        List<String> seen = new ArrayList<>();

        Iterator<IBucket<Person>> iter = repository.getIterator(Person.class);

        while (iter.hasNext()) {
            IBucket<Person> b = iter.next();
            String name = b.getName();
            assertFalse(seen.contains(name));
            seen.add(name);
            assertTrue(BUCKET_NAMES.subList(0, 3).contains(name));
        }
        assertEquals(seen.size(), BUCKET_NAMES.size() - 1); // The last one should not match

        for (String bucket_name : BUCKET_NAMES) {
            repository.deleteBucket(bucket_name);
        }
    }

    @Test
    public void bucketSizeIsConsistent() throws Exception {

        final IBucket<Person> bucket = repository.makeBucket(NEW_BUCKET_NAME, Person.class);

        final int number_of_people = 10;
        final List<Long> ids = new ArrayList<>();

        for (int i = 0; i < number_of_people; i++) {

            Person birth = new Person();
            birth.put(Person.FORENAME, "forename" + i);
            birth.put(Person.SURNAME, "surname" + i);
            bucket.makePersistent(birth);

            ids.add(birth.getId());
        }

        assertEquals(number_of_people, bucket.size());
        assertTrue(bucket.getOids().containsAll(ids));
        assertTrue(ids.containsAll(bucket.getOids()));

        repository.deleteBucket(NEW_BUCKET_NAME);
    }

    @Test
    public void readFromBucketStream() throws Exception {

        final IBucket<Person> bucket = repository.makeBucket(NEW_BUCKET_NAME, Person.class);

        final int number_of_people = 10;
        final Set<Person> births = new HashSet<>();

        for (int i = 0; i < number_of_people; i++) {

            Person birth = new Person();
            birth.put(Person.FORENAME, "forename" + i);
            birth.put(Person.SURNAME, "surname" + i);
            bucket.makePersistent(birth);
            births.add(birth);
        }

        int count = 0;

        for (Person p : bucket.getInputStream()) {
            assertTrue(births.contains(p));
            count++;
        }

        assertEquals(count, births.size());
    }

    @Test
    public void writeToBucketStream() throws Exception {

        final IBucket<Person> bucket = repository.makeBucket(NEW_BUCKET_NAME, Person.class);

        final int number_of_people = 10;
        final Set<Person> birth_set = new HashSet<>();

        IOutputStream<Person> out_stream = bucket.getOutputStream();

        for (int i = 0; i < number_of_people; i++) {

            Person birth = new Person();
            birth.put(Person.FORENAME, "forename" + i);
            birth.put(Person.SURNAME, "surname" + i);
            out_stream.add(birth);
            birth_set.add(birth);
        }

        for (Person birth : birth_set) {
            assertTrue(bucket.contains(birth.getId()));
        }

        assertEquals(number_of_people, bucket.size());
    }

    @Test
    public void consistentBucketType() throws Exception {

        final IBucket<Person> bucket = repository.makeBucket(NEW_BUCKET_NAME, Person.class);

        assertEquals(Person.class, bucket.getBucketType());
    }

    @Test
    public void typeNamesBucketContainsExpectedEntry() throws Exception {

        repository.makeBucket(NEW_BUCKET_NAME, Person.class);

        final IRepository types_repository = store.getRepository("Types_repository");
        final IBucket<LXP> type_names_bucket = types_repository.getBucket("Type_names");

        int count = 0;

        for (LXP type_name : type_names_bucket.getInputStream()) {

            final Object name_field = type_name.get("name");
            if (name_field.equals(Person.class.getSimpleName())) count++;
        }

        assertEquals(1, count);
    }

    @Test
    public void typeRepsBucketContainsExpectedEntry() throws Exception {

        repository.makeBucket(NEW_BUCKET_NAME, Person.class);

        final IRepository types_repository = store.getRepository(TypeFactory.TYPES_REPOSITORY_NAME);
        final IBucket<LXP> type_names_bucket = types_repository.getBucket(TypeFactory.TYPE_NAMES_BUCKET_NAME);
        final IBucket type_reps_bucket = types_repository.getBucket(TypeFactory.TYPE_REPS_BUCKET_NAME);

        for (LXP type_name : type_names_bucket.getInputStream()) {

            final Object name_field = type_name.get(TypeFactory.NAME_FIELD_NAME);
            if (name_field.equals(Person.class.getSimpleName())) {

                final long rep_id = (Long) type_name.get(TypeFactory.KEY_FIELD_NAME);
                assertTrue(type_reps_bucket.contains(rep_id));

                LXP type_rep = (LXP) type_reps_bucket.getObjectById(rep_id);
                assertEquals("STRING", type_rep.get("FORENAME"));
                assertEquals("STRING", type_rep.get("SURNAME"));
                return;
            }
        }

        // Haven't encountered entry for Person.
        fail();
    }

    @Test
    public void nameLegality() {

        for (String name : LEGAL_NAMES) {
            assertTrue(Repository.bucketNameIsLegal(name));
        }

        for (String name : ILLEGAL_NAMES) {
            assertFalse(Repository.bucketNameIsLegal(name));
        }
    }
}
