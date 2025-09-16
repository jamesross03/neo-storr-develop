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

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import org.neo4j.driver.*;
import org.neo4j.driver.types.Node;
import uk.ac.standrews.cs.neoStorr.impl.exceptions.*;
import uk.ac.standrews.cs.neoStorr.impl.transaction.interfaces.ITransaction;
import uk.ac.standrews.cs.neoStorr.interfaces.*;
import uk.ac.standrews.cs.neoStorr.types.Types;
import uk.ac.standrews.cs.neoStorr.util.NeoDbCypherBridge;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import static uk.ac.standrews.cs.neoStorr.impl.Repository.LEGAL_CHARS_PATTERN;

public class NeoBackedBucket<T extends LXP> implements IBucket<T> {

    private static final String LXP_EXISTS_QUERY = "MATCH (o:STORR_LXP { STORR_ID:$id } ) RETURN o";
    private static final String CREATE_LXP_QUERY = "CREATE (n:STORR_LXP $props) RETURN n";
    private static final String ADD_LXP_TO_BUCKET_QUERY = "MATCH(b:STORR_BUCKET),(l:STORR_LXP) WHERE id(b)=$bucket_id AND id(l)=$new_id CREATE (b)-[r:STORR_MEMBER]->(l)";
    private static final String GET_LXPS_QUERY = "MATCH(b:STORR_BUCKET)-[r:STORR_MEMBER]-(l:STORR_LXP) WHERE id(b)=$bucket_id RETURN l";
    private static final String GET_LXP_BY_STORR_ID_QUERY = "MATCH(b:STORR_BUCKET)-[r:STORR_MEMBER]-(l:STORR_LXP) WHERE id(b)=$bucket_id AND l.STORR_ID=$storr_id RETURN l";
    private static final String UPDATE_LXP_PARTIAL_QUERY_BY_STORR_ID = "MATCH (l:STORR_LXP { STORR_ID:$storr_id } ) SET l={ STORR_ID : ";
    private static final String GET_LXP_OIDS_QUERY = "MATCH(b:STORR_BUCKET)-[r:STORR_MEMBER]-(l:STORR_LXP) WHERE id(b)=$bucket_id RETURN l.STORR_ID";
    private static final String GET_TYPE_LABEL_QUERY = "MATCH(b:STORR_BUCKET) WHERE id(b)=$bucket_id RETURN b.TYPE_LABEL_ID";
    private static final String SET_TYPE_LABEL_QUERY = "MATCH(b:STORR_BUCKET) WHERE id(b)=$bucket_id SET b.TYPE_LABEL_ID =$type_label";
    private static final String DELETE_OBJECT_QUERY = "MATCH(b:STORR_BUCKET)-[r:STORR_MEMBER]-(l:STORR_LXP) WHERE id(b)=$bucket_id AND l.STORR_ID=$to_delete_id DETACH DELETE l";

    private static final int DEFAULT_CACHE_SIZE = 10000; // almost certainly too small for serious apps.

    private final IRepository repository;     // the repository in which the bucket is stored

    private final IStore store;               // the store
    private final String bucket_name;         // the name of this bucket - used as the directory name
    private final long neo_id;                // the neo4J id of this bucket
    private final NeoDbCypherBridge bridge;
    private Class<T> bucket_type = null;      // the type of records in this bucket if not null.
    private long type_label_id = -1;          // -1 == not set
    private Cache<Long, PersistentObject> object_cache;
    private int cache_size = DEFAULT_CACHE_SIZE;

    /**
     * Creates a DirectoryBackedBucket with no factory - a persistent collection of ILXPs
     *
     * @param repository  the repository in which to create the bucket
     * @param bucket_name the name of the bucket to be created
     * @param neo_id      the id
     * @throws RepositoryException if the bucket cannot be created in the repository
     */
    protected NeoBackedBucket(final IRepository repository, final String bucket_name, final long neo_id) throws RepositoryException {

        if (bucketNameIsIllegal(bucket_name)) throw new RepositoryException("Illegal name <" + bucket_name + ">");

        this.repository = repository;
        this.bucket_name = bucket_name;
        this.neo_id = neo_id;
        store = repository.getStore();
        bridge = store.getBridge();
        object_cache = newCache(DEFAULT_CACHE_SIZE);
    }

    /**
     * Creates a DirectoryBackedBucket with a factory - a persistent collection of ILXPs tied to some particular Java and store type.
     *
     * @param repository  the repository in which to create the bucket
     * @param bucket_name the name of the bucket to be created
     * @param neo_id      the id
     * @throws RepositoryException if the bucket cannot be created in the repository
     */
    NeoBackedBucket(final IRepository repository, final String bucket_name, final long neo_id, final Class<T> bucket_type) throws RepositoryException {

        this(repository, bucket_name, neo_id);
        this.bucket_type = bucket_type;

        try {
            final T instance = bucket_type.getDeclaredConstructor().newInstance(); // guarantees meta data creation.
            type_label_id = instance.getMetaData().getType().getId();

        } catch (final IllegalAccessException | InstantiationException | NoSuchMethodException | InvocationTargetException e) {
            throw new RepositoryException(e);
        }
    }

    public boolean persistentLabelIsCorrect() {
        return getPersistentTypeLabelID() == type_label_id;
    }

    public boolean bucketTypeIsCorrect(final Class<?> c) {
        return c.equals(bucket_type);
    }

    public long getPersistentTypeLabelID() {

        if (type_label_id != -1) return type_label_id;

        try (final Session session = bridge.getNewSession()) {

            final Result result = session.run(GET_TYPE_LABEL_QUERY, Values.parameters("bucket_id", neo_id));
            final List<Value> ids = result.list(r -> r.get("l.TYPE_LABEL_ID"));

            if (ids.isEmpty())
                throw new RuntimeException("Could not find type label for bucket with neo_id: " + neo_id);

            return ids.get(0).asLong();
        }
    }

    public void setPersistentTypeLabelID() throws BucketException {

        final boolean auto_commit = store.getTransactionManager().isAutoCommitEnabled();
        final Transaction tx = getTransaction(auto_commit);

        tx.run(SET_TYPE_LABEL_QUERY, Values.parameters("bucket_id", neo_id, "type_label", type_label_id));
        if (auto_commit) tx.commit();
    }

    public static boolean bucketNameIsIllegal(String name) {

        return !name.matches(LEGAL_CHARS_PATTERN);
    }

    public void setCacheSize(final int cache_size) throws BucketException {

        if (cache_size < object_cache.size())
            throw new BucketException("Object cache cannot be dynamically made smaller");

        final LoadingCache<Long, PersistentObject> new_cache = newCache(cache_size);
        new_cache.putAll(object_cache.asMap());
        this.cache_size = cache_size;
        object_cache = new_cache;
    }

    public int getCacheSize() {
        return cache_size;
    }

    private LoadingCache<Long, PersistentObject> newCache(final int cacheSize) {
        return CacheBuilder.newBuilder()
                .maximumSize(cacheSize)
                .weakValues()
                .build(
                        new CacheLoader<>() {
                            public PersistentObject load(final Long id) throws BucketException {
                                return NeoBackedBucket.this.load(id);
                            }
                        }
                );
    }

    public PersistentObject load(final long storr_id) throws BucketException {

        try (final Session session = bridge.getNewSession()) {

            final Result result = session.run(GET_LXP_BY_STORR_ID_QUERY, Values.parameters("bucket_id", neo_id, "storr_id", storr_id));

            final List<Node> nodes = result.list(r -> r.get("l").asNode());
            if (nodes.isEmpty())
                throw new BucketException("Did not find object with id: " + storr_id + " in " + bucket_name);

            final Map<String, Object> properties = nodes.get(0).asMap();

            try {
                //  No relevant constructor.
                if (bucket_type == null) return new DynamicLXP(storr_id, properties, this);

                final Constructor<?> constructor = bucket_type.getConstructor(long.class, Map.class, IBucket.class);
                return (PersistentObject) constructor.newInstance(storr_id, properties, this);

            } catch (final PersistentObjectException e) {
                throw new BucketException("Could not create new LXP for object with id: " + storr_id);
            } catch (final NoSuchMethodException e) {
                throw new BucketException("Error in reflective constructor call: class <" + bucket_type.getName() + "> must implement a constructor with the following signature: Constructor( long persistent_object_id, Map properties, IBucket bucket )");
            } catch (final IllegalAccessException | InstantiationException | InvocationTargetException e) {
                throw new BucketException("Error in reflective call of constructor in class " + bucket_type.getName() + ": " + e.getMessage());
            }
        }
    }

    public T getObjectById(final long id) throws BucketException {

        try {
            // this is safe since this.contains(id) and also the cache contains the object.

            //noinspection unchecked
            return (T) object_cache.get(id, () -> load(id));

        } catch (final ExecutionException e) {
            throw new BucketException("Cannot get object by id: " + id, e);
        }
    }

    @Override
    public IRepository getRepository() {
        return repository;
    }

    public String getName() {
        return bucket_name;
    }

    public Class<T> getBucketType() {
        return bucket_type;
    }

    public long getNeoId() {
        return neo_id;
    }

    public boolean contains(final long storr_id) {

        // If auto-commit is off, the id may be present only in the cache, if creation hasn't yet been committed.
        if (!store.getTransactionManager().isAutoCommitEnabled() && object_cache.getIfPresent(storr_id) != null)
            return true;

        try (Session session = bridge.getNewSession()) {

            final Result result = session.run(LXP_EXISTS_QUERY, Values.parameters("id", storr_id));
            return !result.list(r -> r.get("o").asNode()).isEmpty();
        }
    }

    public IInputStream<T> getInputStream() throws BucketException {

        try {
            return new NeoBackedInputStream<>(this);

        } catch (final IOException e) {
            throw new BucketException(e);
        }
    }

    public IOutputStream<T> getOutputStream() {
        return new NeoBackedOutputStream<>(this);
    }

    /**
     * @return the ids of records that are in this bucket
     */
    public synchronized List<Long> getObjectIds() {

        try (Session session = bridge.getNewSession()) {

            final List<Long> ids = new ArrayList<>();
            final Result result = session.run(GET_LXP_OIDS_QUERY, Values.parameters("bucket_id", neo_id));

            for (Value v : result.list(r -> r.get("l.STORR_ID"))) {
                ids.add(v.asLong());
            }
            return ids;
        }
    }

    public void makePersistent(final LXP record) throws BucketException {

        checkPersistencyConditions(record);

        object_cache.put(record.getId(), record);
        writeLXP(record);
    }

    private void checkPersistencyConditions(LXP record) throws BucketException {

        if (contains(record.getId())) throw new BucketException("records may not be overwritten - use update");

        if (type_label_id != -1) {

            // Bucket has a type label, check for consistency.
            if (record.getMetaData().containsLabel(Types.LABEL) && !(Types.checkLabelConsistency(record, type_label_id, store)))
                throw new BucketException("Label incompatibility");

            if (!Types.checkStructuralConsistency(record, type_label_id, store))
                throw new BucketException("Structural integrity incompatibility: " + record);

        } else {
            // Bucket has no type label.
            if (record.getMetaData().containsLabel(Types.LABEL) && !Types.checkStructuralConsistency(record, (long) record.get(Types.LABEL), store))
                throw new BucketException("Structural integrity incompatibility");
        }
    }

    @Override
    public synchronized void update(final T record_to_update) throws BucketException {

        if (!contains(record_to_update.getId())) throw new BucketException("bucket does not contain specified id");

        final String query = buildUpdateQuery(record_to_update);
        final boolean auto_commit = store.getTransactionManager().isAutoCommitEnabled();

        final ITransaction transaction = auto_commit ? store.getTransactionManager().beginTransaction() : getCurrentStorrTransaction();

        transaction.add(this, record_to_update);
        transaction.getNeoTransaction().run(query, Values.parameters("storr_id", record_to_update.getId()));

        if (auto_commit) transaction.commit();
    }

    private String buildUpdateQuery(final T record_to_update) {

        final StringBuilder query = new StringBuilder(UPDATE_LXP_PARTIAL_QUERY_BY_STORR_ID);

        query.append(record_to_update.getId());
        appendProperties(query, record_to_update.serializeFieldsToMap());
        query.append("}");

        return query.toString();
    }

    private void appendProperties(final StringBuilder query, final Map<String, Object> properties) {

        for (Map.Entry<String, Object> entry : properties.entrySet()) {
            query.append(", ");
            query.append(entry.getKey());
            query.append(" : ");
            query.append(toSerializationFormat(entry.getValue()));
        }
    }

    private String toSerializationFormat(final Object value) {

        return value instanceof String ? ("\"" + value + "\"") : value.toString();
    }

    private void writeLXP(final LXP record_to_write) throws BucketException {

        record_to_write.$$$bucket$$$bucket$$$ = this;

        final Class<?> c = record_to_write.getMetaData().metadata_class;

        final Map<String, Object> properties = record_to_write.serializeFieldsToMap();
        properties.put("STORR_ID", record_to_write.getId());

        final boolean auto_commit = store.getTransactionManager().isAutoCommitEnabled();
        final Transaction tx = getTransaction(auto_commit);
        runWriteLXPQuery(record_to_write, properties, c, tx);
        if (auto_commit) tx.commit();
    }

    private Transaction getTransaction(final boolean auto_commit) throws BucketException {

        return auto_commit ? bridge.getNewSession().beginTransaction() : getCurrentStorrTransaction().getNeoTransaction();
    }

    private ITransaction getCurrentStorrTransaction() throws BucketException {

        final ITransaction storr_transaction = store.getTransactionManager().getTransaction(Long.toString(Thread.currentThread().threadId()));

        if (storr_transaction == null || !storr_transaction.isActive()) {
            throw new BucketException("No transactional context specified");
        }
        return storr_transaction;
    }

    private void runWriteLXPQuery(final LXP record_to_write, final Map<String, Object> properties, final Class<?> c, final Transaction tx) throws BucketException {

        final String query = c != null ? buildParameterisedWriteLXPQuery(c) : CREATE_LXP_QUERY;
        final Result result = tx.run(query, Values.parameters("props", properties));

        final List<Node> nodes = result.list(r -> r.get("n").asNode());
        if (nodes.isEmpty())
            throw new BucketException("Cannot write LXP of type: " + record_to_write.getClass().getName() + " and id: " + record_to_write.getId());

        tx.run(ADD_LXP_TO_BUCKET_QUERY, Values.parameters("bucket_id", neo_id, "new_id", nodes.get(0).id()));
    }

    private String buildParameterisedWriteLXPQuery(Class<?> c) {

        return "CREATE (n:STORR_LXP:" + c.getSimpleName() + " $props) RETURN n";
    }

    public synchronized int size() {

        try (Session session = bridge.getNewSession()) {

            final Result result = session.run(GET_LXPS_QUERY, Values.parameters("bucket_id", neo_id));
            return result.list(r -> r.get("l")).size();
        }
    }

    public synchronized void invalidateCache() {

        // Called by watcher service.
        object_cache = newCache(cache_size); // There may be extant references to these objects in the heap which should be invalidated.
        // TODO is comment above a TODO?
    }

    @Override
    public void delete(final long object_id) throws BucketException {

        final boolean auto_commit = store.getTransactionManager().isAutoCommitEnabled();
        final Transaction tx = getTransaction(auto_commit);
        tx.run(DELETE_OBJECT_QUERY, Values.parameters("bucket_id", neo_id, "to_delete_id", object_id));
        if (auto_commit) tx.commit();

        object_cache.invalidate(object_id);
    }
}
