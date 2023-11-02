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
import uk.ac.standrews.cs.neoStorr.impl.testData.DynamicPersonReference;
import uk.ac.standrews.cs.neoStorr.impl.testData.Person;
import uk.ac.standrews.cs.neoStorr.impl.testData.StaticPersonReference;
import uk.ac.standrews.cs.neoStorr.interfaces.IBucket;
import uk.ac.standrews.cs.neoStorr.interfaces.IStoreReference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ObjectsWithReferencesTest extends CommonTest {

    static final String TYPED_BUCKET_NAME1 = "Typed Bucket 1";
    static final String TYPED_BUCKET_NAME2 = "Typed Bucket 2";
    static final String UNTYPED_BUCKET_NAME = "Untyped Bucket";

    private IBucket<Person> typed_bucket1;
    private IBucket<StaticPersonReference> typed_bucket2;
    private IBucket untyped_bucket;

    @Before
    public void setUp() throws Exception {

        super.setUp();
        store.getTransactionManager().setAutoCommit(true);

        typed_bucket1 = repository.makeBucket(TYPED_BUCKET_NAME1, Person.class);
        typed_bucket2 = repository.makeBucket(TYPED_BUCKET_NAME2, StaticPersonReference.class);
        untyped_bucket = repository.makeBucket(UNTYPED_BUCKET_NAME);
    }

    @After
    public void tearDown() throws RepositoryException {

        repository.deleteBucket(TYPED_BUCKET_NAME1);
        repository.deleteBucket(TYPED_BUCKET_NAME2);
        repository.deleteBucket(UNTYPED_BUCKET_NAME);

        super.tearDown();
    }

    @Test
    public void dereferenceAPersistentDynamicReference() throws Exception {

        final Person al = new Person("Al", "Dearle");
        persistRecord(al, typed_bucket1);

        final DynamicPersonReference referer = new DynamicPersonReference(al);
        persistRecord(referer, untyped_bucket);

        final DynamicPersonReference retrieved_referer = (DynamicPersonReference) untyped_bucket.getInputStream().iterator().next();
        final IStoreReference<Person> reference = (IStoreReference<Person>) retrieved_referer.get(DynamicPersonReference.REFERENCE_FIELD_NAME);

        assertEquals(al, reference.getReferend());
    }

    @Test
    public void deferenceAPersistentStaticReference() throws Exception {

        final Person al = new Person("Al", "Dearle");
        persistRecord(al, typed_bucket1);

        final StaticPersonReference referer = new StaticPersonReference(al);
        persistRecord(referer, typed_bucket2);

        final StaticPersonReference retrieved_referer = typed_bucket2.getInputStream().iterator().next();
        final IStoreReference<Person> reference = retrieved_referer.getRef(StaticPersonReference.MY_FIELD);

        assertEquals(al, reference.getReferend());
    }

    @Test
    public void dereferenceAReference() throws BucketException, RepositoryException {

        final Person al = new Person("Al", "Dearle");
        IStoreReference<Person> al_ref = new LXPReference<>(repository, typed_bucket1, al);

        assertEquals(al, al_ref.getReferend());
        assertEquals(al, al_ref.getReferend(Person.class));
    }

    private void persistRecord(LXP record, IBucket bucket) throws BucketException {

        bucket.makePersistent(record);
        final long record_id = record.getId();

        assertTrue(bucket.contains(record_id));
        assertEquals(record, bucket.getObjectById(record_id));
    }
}
