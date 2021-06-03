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
import org.neo4j.driver.Result;
import org.neo4j.driver.Session;
import org.neo4j.driver.Transaction;
import org.neo4j.driver.Value;
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

import static org.neo4j.driver.Values.parameters;
import static uk.ac.standrews.cs.neoStorr.impl.Repository.LEGAL_CHARS_PATTERN;
import static uk.ac.standrews.cs.neoStorr.types.Types.checkLabelConsistency;

public class NeoBackedBucket<T extends LXP> implements IBucket<T> {

    public static final String META_BUCKET_NAME = "META";
    private static final String TYPE_LABEL_FILE_NAME = "TYPELABEL";

    public static final String LXP_EXISTS = "MATCH (o:STORR_LXP { STORR_ID:$id } ) RETURN o";
    private static final String CREATE_LXP_QUERY ="CREATE (n:STORR_LXP $props) RETURN n";
    private static final String ADD_LXP_TO_BUCKET_QUERY = "MATCH(b:STORR_BUCKET),(l:STORR_LXP) WHERE id(b)=$bucket_id AND id(l)=$new_id CREATE (b)-[r:STORR_MEMBER]->(l)";
    private static final String GET_LXPS_QUERY = "MATCH(b:STORR_BUCKET)-[r:STORR_MEMBER]-(l:STORR_LXP) WHERE id(b)=$bucket_id RETURN l";
    private static final String GET_LXP_BY_STORR_ID_QUERY = "MATCH(b:STORR_BUCKET)-[r:STORR_MEMBER]-(l:STORR_LXP) WHERE id(b)=$bucket_id AND l.STORR_ID=$storr_id RETURN l";
    private static final String UPDATE_LXP_PARTIAL_QUERY_BY_STORR_ID = "MATCH (l:STORR_LXP { STORR_ID:$storr_id } ) SET l={ ";
    private static final String GET_LXP_OIDS_QUERY = "MATCH(b:STORR_BUCKET)-[r:STORR_MEMBER]-(l:STORR_LXP) WHERE id(b)=$bucket_id RETURN l.STORR_ID";
    private static final String GET_TYPE_LABEL_QUERY = "MATCH(b:STORR_BUCKET) WHERE id(b)=$bucket_id RETURN b.TYPE_LABEL_ID";
    private static final String SET_TYPE_LABEL_QUERY = "MATCH(b:STORR_BUCKET) WHERE id(b)=$bucket_id SET b.TYPE_LABEL_ID =$type_label";
    private static final String DELETE_OID_QUERY = "MATCH(b:STORR_BUCKET)-[r:STORR_MEMBER]-(l:STORR_LXP) WHERE id(b)=$bucket_id AND l.STORR_ID=$to_delete_id DETACH DELETE l";

    private final IRepository repository;     // the repository in which the bucket is stored

    private final IStore store;               // the store
    private final String bucket_name;         // the name of this bucket - used as the directory name
    private final long neo_id;                 // the neo4J id of this bucket
    private NeoDbCypherBridge bridge;
    private Class<T> bucketType = null;       // the type of records in this bucket if not null.
    private long type_label_id = -1;          // -1 == not set
    private Cache<Long, PersistentObject> object_cache;
    private static final int DEFAULT_CACHE_SIZE = 10000; // almost certainly too small for serious apps.
    private int cache_size = DEFAULT_CACHE_SIZE;

    /**
     * Creates a DirectoryBackedBucket with no factory - a persistent collection of ILXPs
     *
     * @param repository  the repository in which to create the bucket
     * @param bucket_name the name of the bucket to be created
     * @param neo_id
     * @throws RepositoryException if the bucket cannot be created in the repository
     */
    protected NeoBackedBucket(final IRepository repository, final String bucket_name, long neo_id) throws RepositoryException {

        if (!bucketNameIsLegal(bucket_name)) {
            throw new RepositoryException("Illegal name <" + bucket_name + ">");
        }

        this.bucket_name = bucket_name;
        this.repository = repository;
        this.store = repository.getStore();
        this.bridge = store.getBridge();
        this.neo_id = neo_id;
        object_cache = newCache(repository, DEFAULT_CACHE_SIZE, this);
    }

    /**
     * Creates a DirectoryBackedBucket with a factory - a persistent collection of ILXPs tied to some particular Java and store type.
     *
     * @param repository  the repository in which to create the bucket
     * @param bucket_name the name of the bucket to be created
     * @param neo_id
     * @throws RepositoryException if the bucket cannot be created in the repository
     */
    NeoBackedBucket(final IRepository repository, final String bucket_name, long neo_id, final Class<T> bucketType ) throws RepositoryException {

        this.bucketType = bucketType;
        this.bucket_name = bucket_name;
        this.repository = repository;
        this.store = repository.getStore();
        this.bridge = store.getBridge();
        this.neo_id = neo_id;

        if (!bucketNameIsLegal(bucket_name)) {
            throw new RepositoryException("Illegal name <" + bucket_name + ">");
        }

        try {
            final T instance = bucketType.newInstance(); // guarantees meta data creation.
            final PersistentMetaData md = instance.getMetaData();
            this.type_label_id = md.getType().getId();
        } catch (final IllegalAccessException | InstantiationException e) {
            throw new RepositoryException(e);
        }

        object_cache = newCache(repository, DEFAULT_CACHE_SIZE, this);
    }

    public boolean persistentLabelIsCorrect() {
        return getPersistentTypeLabelID() == type_label_id;
    }

    public boolean bucketTypeIsCorrect( Class clazz ) {
        return bucketType.equals(clazz);
    }

    public long getPersistentTypeLabelID() {

        if (type_label_id != -1) {
            return type_label_id;
        } // only look it up if not cached.

        try (Session session = bridge.getNewSession();) {

            System.out.println("Bucket neo id = " + this.neo_id);

            Result result = session.run(GET_TYPE_LABEL_QUERY, parameters("bucket_id", this.neo_id));

            List<Value> values = result.list(r -> r.get("l.TYPE_LABEL_ID"));
            if (values.size() != 1) {
                throw new RuntimeException("Could not find type label for bucket with neo_id: " + this.neo_id);
            }
            return convertToLongs(values).get(0);
        }
    }

    public void setPersistentTypeLabelID() throws IOException {

        try (Session session = bridge.getNewSession(); Transaction tx = session.beginTransaction();) {

            tx.run(SET_TYPE_LABEL_QUERY, parameters("bucket_id", this.neo_id, "type_label", type_label_id));

        } catch (Exception e) {
            throw new IOException(e);
        }
    }

    public static boolean bucketNameIsLegal(String name) {

        return name.matches(LEGAL_CHARS_PATTERN);
    }

    public void setCacheSize(final int cache_size) throws Exception {
        if (cache_size < object_cache.size()) {
            throw new Exception("Object cache cannot be dynamically made smaller");
        }
        final LoadingCache<Long, PersistentObject> new_cache = newCache(repository, cache_size, this);
        new_cache.putAll(object_cache.asMap());
        this.cache_size = cache_size;
        object_cache = new_cache;
    }

    public int getCacheSize() {
        return cache_size;
    }

    private LoadingCache<Long, PersistentObject> newCache(final IRepository repository, final int cacheSize, final NeoBackedBucket<T> my_bucket) {
        return CacheBuilder.newBuilder()
                .maximumSize(cacheSize)
                .weakValues()
                .build(
                        new CacheLoader<Long, PersistentObject>() {

                            public PersistentObject load(final Long id) throws BucketException { // no checked exception
                                return loader(id);
                            }
                        }
                );
    }

    public PersistentObject loader(final Long storr_id) throws BucketException { // no checked exception

        try (Session session = bridge.getNewSession();) {

            Result q_result = session.run(GET_LXP_BY_STORR_ID_QUERY, parameters("bucket_id", this.neo_id, "storr_id", storr_id));

            List<Node> nodes = q_result.list(r -> r.get("l").asNode());
            if (nodes.size() != 1) {
                throw new BucketException("Did not find object with id: " + storr_id + " in " + bucket_name);
            }
            Map<String, Object> properties = nodes.get(0).asMap();

            if (bucketType == null) { //  No java constructor specified
                try {
                    return new DynamicLXP(storr_id, properties, this);
                } catch (final PersistentObjectException e) {
                    throw new BucketException("Could not create new LXP for object with id: " + storr_id);
                }
            } else {
                final Constructor<?> constructor;
                try {
                    final Class[] param_classes = new Class[]{long.class, Map.class, IBucket.class};
                    constructor = bucketType.getConstructor(param_classes);
                } catch (final NoSuchMethodException e) {
                    throw new BucketException("Error in reflective constructor call: class <" + bucketType.getName() + "> must implement a constructor with the following signature: Constructor( long persistent_object_id, Map properties, IBucket bucket )");
                }
                try {
                    return (PersistentObject) constructor.newInstance(storr_id, properties, this);
                } catch (final IllegalAccessException | InstantiationException | InvocationTargetException e) {
                    throw new BucketException("Error in reflective call of constructor in class " + bucketType.getName() + ": " + e.getMessage());
                }
            }
        }
    }

    public T getObjectById(final long id) throws BucketException {

        try {
            return (T) object_cache.get(id, () -> loader(id));
            // this is safe since this.contains(id) and also the cache contains the object.

        } catch (final ExecutionException e) {
            throw new BucketException("Cannot get object by id: " + id + " Exception " + e.getMessage());
        }
    }

    @Override
    public IRepository getRepository() {
        return this.repository;
    }

    public String getName() {
        return bucket_name;
    }

    public Class<T> getBucketType() {
        return bucketType;
    }

    public long getNeoId() { return neo_id; }

    public boolean contains(final long storr_id) {
        Result result = bridge.getNewSession().run(LXP_EXISTS, parameters("id", storr_id));
        if (result == null) {
            return false;
        }
        List<Node> nodes = result.list(r -> r.get("o").asNode());
        if (nodes.size() == 0) {
            return false;
        }
        return true;
    }

    public IInputStream<T> getInputStream() throws BucketException {

        try {
            return new NeoBackedInputStream<>(this);

        } catch (final IOException e) {
            throw new BucketException(e.getMessage());
        }
    }

    //***********************************************************//

    public IOutputStream<T> getOutputStream() {
        return new NeoBackedOutputStream<>(this);
    }

    /**
     * @return the oids of records that are in this bucket
     */
    public synchronized List<Long> getOids() {

        try (Session session = bridge.getNewSession(); Transaction tx = session.beginTransaction();) {

            Result result = tx.run(GET_LXP_OIDS_QUERY, parameters("bucket_id", this.neo_id));

            List<Value> xx = result.list(r -> r.get("l.STORR_ID"));
            return convertToLongs(xx);
        }
    }

    private List<Long> convertToLongs(List<Value> values) {
        List<Long> result = new ArrayList<>();
        for (Value v : values) {
            result.add(v.asLong());
        }
        return result;
    }

    public void makePersistent(final LXP record) throws BucketException {

        final long storr_id = record.getId();
        if (contains(storr_id)) {
            throw new BucketException("records may not be overwritten - use update");
        } else {
            object_cache.put(storr_id, record);
            writePersistentObject((LXP) record); // normal object write
        }
    }

    @Override
    public synchronized void update(final T record_to_update) throws BucketException {

        final long storr_id = record_to_update.getId();
        if (!contains(storr_id)) {
            throw new BucketException("bucket does not contain specified id");
        }

        StringBuilder query = new StringBuilder();
        query.append(UPDATE_LXP_PARTIAL_QUERY_BY_STORR_ID);

        Map<String, Object> props = record_to_update.serializeFieldsToMap();
        query.append("STORR_ID"); query.append(" : "); query.append(record_to_update.getId()); query.append(", ");
        for (Map.Entry<String, Object> entry : props.entrySet()) {
            query.append(entry.getKey());
            query.append(" : ");
            query.append( toSerializationFormat( entry.getValue() ) );
            query.append(", ");
        }
        query.delete( query.length() - 2,query.length() - 1 ); // get rid of last comma
        query.append("}");

        final ITransaction tx;
        try {
            tx = store.getTransactionManager().getTransaction(Long.toString(Thread.currentThread().getId()));
        } catch (final StoreException e) {
            throw new BucketException(e);
        }
        if (tx == null) {
            throw new BucketException("No transactional context specified");
        }

        tx.add(this, record_to_update);

        tx.getNeoTransaction().run(query.toString(), parameters("storr_id", storr_id));

        // MATCH (p {name: 'Peter'})
        // SET p = {name: 'Peter Smith', position: 'Entrepreneur'}
    }

    private String toSerializationFormat(Object value) {
        if (value instanceof String) {
            return "\"" + value + "\"";
        } else {
            return value.toString();
        }

    }

    private void writePersistentObject(final PersistentObject record_to_write) throws BucketException {
        if( record_to_write instanceof LXP ) {
            writeLXP( (LXP) record_to_write );
        } else {
            throw new BucketException( "This impl only capable of writing LXP instances");
        }
    }

    private void writeLXP(final LXP record_to_write) throws BucketException {

       if (type_label_id != -1) { // we have set a type label in this bucket there must check for consistency
            if (record_to_write.getMetaData().containsLabel(Types.LABEL)) { // if there is a label it must be correct
                if (!(checkLabelConsistency(record_to_write, type_label_id, store))) { // check that the record label matches the bucket label - throw exception if it doesn't
                    throw new BucketException("Label incompatibility");
                }
            }
            // get to here -> there is no record label on record
            try {
                if (!Types.checkStructuralConsistency(record_to_write, type_label_id, store)) {
                    // Temporarily output more information, for diagnostics
                    throw new BucketException("Structural integrity incompatibility"
                            + "\nrecord_to_write: " + record_to_write + "\n"
                            + "\ntype_label_id: " + type_label_id + "\n");
                }
            } catch (final IOException e) {
                throw new BucketException("I/O exception checking Structural integrity");
            }
        } else // get to here and bucket has no type label on it.
            if (record_to_write.getMetaData().containsLabel(Types.LABEL)) { // no type label on bucket but record has a type label so check structure
                try {
                    if (!Types.checkStructuralConsistency(record_to_write, (long) record_to_write.get(Types.LABEL), store)) {
                        throw new BucketException("Structural integrity incompatibility");
                    }
                } catch (final KeyNotFoundException e) {
                    // this cannot happen - label checked in if .. so .. just let it go
                } catch (final IOException e) {
                    throw new BucketException("I/O exception checking consistency");
                } catch (final TypeMismatchFoundException e) {
                    throw new BucketException("Type mismatch checking consistency");
                }
            }

        if( record_to_write instanceof LXP) {
            writeData((LXP) record_to_write);
        } else {
            throw new RuntimeException( "This version can only persist LXPs not PersistentObjects - FIXME??");
        }
    }

    private void writeData(LXP record_to_write) {

        record_to_write.$$$bucket$$$bucket$$$ = this;
        Map<String, Object> props = record_to_write.serializeFieldsToMap();
        props.put( "STORR_ID", record_to_write.getId());
        Class java_class = record_to_write.getMetaData().metadata_class;

        try (Session session = bridge.getNewSession(); Transaction tx = session.beginTransaction();) {

            Result result;
            if( java_class != null ) {
                result = tx.run(build_parameterised_query( java_class ), parameters("props", props));
            } else{
                result = tx.run(CREATE_LXP_QUERY, parameters("props", props));
            }

            List<Node> nodes = result.list(r -> r.get("n").asNode());
            if( nodes.size() != 1 ) {
                throw new RepositoryException( "Cannot write LXP of type: " + record_to_write.getClass().getName() + " and id: " + record_to_write.getId() );
            }
            long new_id = nodes.get(0).id();

            tx.run(ADD_LXP_TO_BUCKET_QUERY,parameters("bucket_id", this.neo_id, "new_id", new_id));
            tx.commit();
        } catch (RepositoryException e) {
            e.printStackTrace();
        }
    }

    private String build_parameterised_query(Class clazz) {
        String name = clazz.getSimpleName();
        return "CREATE (n:STORR_LXP:" + name + " $props) RETURN n";
    }


    public synchronized int size() throws BucketException {

        try (Session session = bridge.getNewSession(); Transaction tx = session.beginTransaction();) {

            Result result = tx.run(GET_LXPS_QUERY, parameters("bucket_id", this.neo_id));

            return result.list(r -> r.get("l")).size();
        } catch ( Exception e ) {
            throw new BucketException(e);
        }
    }

    /**
     * `
     * called by Watcher service
     */
    public synchronized void invalidateCache() {

        object_cache = newCache(repository, cache_size, this); // There may be extent references to these objects in the heap which should be invalidated.
    }

    @Override
    public void delete(final long oid) throws BucketException {

        try (Session session = bridge.getNewSession();) {

            session.writeTransaction(tx -> tx.run(DELETE_OID_QUERY, parameters("bucket_id", this.neo_id, "to_delete_id", oid)));

        } catch ( Exception e ) {
            throw new BucketException(e);
        }
        object_cache.invalidate(oid);
    }
}
