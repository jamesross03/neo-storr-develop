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
package uk.ac.standrews.cs.storr.impl;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import org.neo4j.driver.Result;
import org.neo4j.driver.Session;
import org.neo4j.driver.Transaction;
import org.neo4j.driver.types.Node;
import uk.ac.standrews.cs.storr.impl.exceptions.BucketException;
import uk.ac.standrews.cs.storr.impl.exceptions.RepositoryException;
import uk.ac.standrews.cs.storr.interfaces.*;
import uk.ac.standrews.cs.storr.util.NeoDbCypherBridge;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import static org.neo4j.driver.Values.parameters;
import static uk.ac.standrews.cs.storr.impl.Repository.bucketNameIsLegal;

public class NeoBackedBucket<T extends PersistentObject> implements IBucket<T> {

    public static final String META_BUCKET_NAME = "META";
    private static final String TYPE_LABEL_FILE_NAME = "TYPELABEL";

    private static final String LXP_EXISTS = "MATCH (o: STORR_LXP { STORR_ID:$id }) return o";

    private final IRepository repository;     // the repository in which the bucket is stored

    private final IStore store;               // the store
    private final String bucket_name;         // the name of this bucket - used as the directory name
    private final long neo_id;                 // the neo4J id of this bucket
    private NeoDbCypherBridge bridge;
    private Class<T> bucketType = null;       // the type of records in this bucket if not null.
    private long type_label_id = -1;          // -1 == not set
    private Cache<Long, PersistentObject> object_cache;
    private int size = -1; // number of items in Bucket.
    private List<Long> cached_oids = null;
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
    NeoBackedBucket(final IRepository repository, final String bucket_name, final Class<T> bucketType, long neo_id) throws RepositoryException {

        this.bucketType = bucketType;
        this.bucket_name = bucket_name;
        this.repository = repository;
        this.store = repository.getStore();
        this.bridge = store.getBridge();
        this.neo_id = neo_id;
        final long class_type_label_id; // TODO

        if (!bucketNameIsLegal(bucket_name)) {
            throw new RepositoryException("Illegal name <" + bucket_name + ">");
        }

        try {
            final T instance = bucketType.newInstance(); // guarantees meta data creation.
            final PersistentMetaData md = instance.getMetaData();
            class_type_label_id = md.getType().getId();
        } catch (final IllegalAccessException | InstantiationException e) {
            throw new RepositoryException(e);
        }

        object_cache = newCache(repository, DEFAULT_CACHE_SIZE, this);
    }

    private void checkKind(String bucket_name, IRepository repository) {
        // TODO
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

    public PersistentObject loader(final Long id) throws BucketException { // no checked exception


//        final PersistentObject result;
//
//        try (final BufferedReader reader = Files.newBufferedReader(filePath(id), FileManipulation.FILE_CHARSET)) {
//
//            if (bucketType == null) { //  No java constructor specified
//                try {
//                    result = new DynamicLXP(id, new JSONReader(reader), this);
//                } catch (final PersistentObjectException e) {
//                    throw new BucketException("Could not create new LXP for object with id: " + id + " in directory: " + directory );
//                }
//            } else {
//                final Constructor<?> constructor;
//                try {
//                    final Class[] param_classes = new Class[] { long.class, JSONReader.class, IBucket.class };
//                    constructor = bucketType.getConstructor( param_classes );
//                }
//                catch ( final NoSuchMethodException e ) {
//                    throw new BucketException("Error in reflective constructor call - class " + bucketType.getName() + " must implement constructors with the following signature: Constructor(long persistent_object_id, JSONReader reader, IBucket bucket )" );
//                }
//                try {
//                    result = (PersistentObject) constructor.newInstance( id, new JSONReader(reader), this);
//                } catch (final IllegalAccessException | InstantiationException | InvocationTargetException e) {
//                    throw new BucketException("Error in reflective call of constructor in class " + bucketType.getName() + ": " + e.getMessage() );
//                }
//
//            }
//        } catch (final IOException e) {
//            throw new BucketException( "Error creating JSONReader for id: " + id + " in bucket " + bucket_name );
//        }
//        return result;
        return null; //TODO
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

    public boolean contains(final long id) {
        Result result = bridge.getNewSession().run(LXP_EXISTS, parameters("id", id));
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
            return new BucketBackedInputStream<>(this);

        } catch (final IOException e) {
            throw new BucketException(e.getMessage());
        }
    }

    //***********************************************************//

    public IOutputStream<T> getOutputStream() {
        return new BucketBackedOutputStream<>(this);
    }

    /**
     * @return the oids of records that are in this bucket
     */
    public synchronized List<Long> getOids() {

        if (cached_oids == null) {

            cached_oids = new ArrayList<>();

//            final Iterator<File> iterator = new FileIterator(directory, true, false);
//            while (iterator.hasNext()) {
//                cached_oids.add(Long.parseLong(iterator.next().getName()));
//            }
        }
        return cached_oids;
    }

    private long getTypeLabelID() {

//        if (type_label_id != -1) {
//            return type_label_id;
//        } // only look it up if not cached.
//
//        final Path path = directory.toPath();
//        final Path typepath = path.resolve(META_BUCKET_NAME).resolve(TYPE_LABEL_FILE_NAME);
//
//        try (final BufferedReader reader = Files.newBufferedReader(typepath, FileManipulation.FILE_CHARSET)) {
//
//            final String id_as_string = reader.readLine();
//            type_label_id = Long.parseLong(id_as_string);
//            return type_label_id;
//
//        } catch (final IOException e) {
//            throw new RuntimeException(e);
//        }
        throw new RuntimeException();
    }

    public void setTypeLabelID(final long type_label_id) throws IOException {

//        if (this.type_label_id != -1) {
//            throw new IOException("Type label already set");
//        }
//        this.type_label_id = type_label_id; // cache it and keep a persistent copy of the label.
//
//        final Path path = directory.toPath();
//        final Path meta_path = path.resolve(META_BUCKET_NAME);
//        FileManipulation.createDirectoryIfDoesNotExist(meta_path);
//
//        final Path typepath = meta_path.resolve(TYPE_LABEL_FILE_NAME);
//        if (Files.exists(typepath)) {
//            throw new IOException("Type label already set");
//        }
//        FileManipulation.createFileIfDoesNotExist((typepath));
//
//        try (final BufferedWriter writer = Files.newBufferedWriter(typepath, FileManipulation.FILE_CHARSET)) {
//
//            writer.write(Long.toString(type_label_id)); // Write the id of the typelabel OID into this field.
//            writer.newLine();
//        }
    }

    public void makePersistent(final PersistentObject record) throws BucketException {

        final long id = record.getId();
        if (contains(id)) {
            throw new BucketException("records may not be overwritten - use update");
        } else {
            writePersistentObject(record); // normal object write
        }
    }

    @Override
    public synchronized void update(final T record) throws BucketException {

//        final long id = record.getId();
//        if (!contains(id)) {
//            throw new BucketException("bucket does not contain specified id");
//        }
//        final Transaction t;
//        try {
//            t = store.getTransactionManager().getTransaction(Long.toString(Thread.currentThread().getId()));
//        } catch (final StoreException e) {
//            throw new BucketException(e);
//        }
//        if (t == null) {
//            throw new BucketException("No transactional context specified");
//        }
//
//        final Path new_record_write_location = transactionsPath(record.getId());
//        if (new_record_write_location.toFile().exists()) { // we have a transaction conflict.
//            t.rollback();
//            return;
//        }
//        t.add(this, record.getId());
//
//        writePersistentObject(record, new_record_write_location); //  write to transaction log
    }

    private void writePersistentObject(final PersistentObject record_to_write) throws BucketException {

        System.out.println("Not dealt with type ids yet"); // TODO


//       if (type_label_id != -1) { // we have set a type label in this bucket there must check for consistency
//            if (record_to_write.getMetaData().containsLabel(Types.LABEL)) { // if there is a label it must be correct
//                if (!(checkLabelConsistency(record_to_write, type_label_id, store))) { // check that the record label matches the bucket label - throw exception if it doesn't
//                    throw new BucketException("Label incompatibility");
//                }
//            }
//            // get to here -> there is no record label on record
//            try {
//                if (!Types.checkStructuralConsistency(record_to_write, type_label_id, store)) {
//                    // Temporarily output more information, for diagnostics
//                    throw new BucketException("Structural integrity incompatibility"
//                            + "\nrecord_to_write: " + record_to_write + "\n"
//                            + "\ntype_label_id: " + type_label_id + "\n");
//                }
//            } catch (final IOException e) {
//                throw new BucketException("I/O exception checking Structural integrity");
//            }
//        } else // get to here and bucket has no type label on it.
//            if (record_to_write.getMetaData().containsLabel(Types.LABEL)) { // no type label on bucket but record has a type label so check structure
//                try {
//                    if (!Types.checkStructuralConsistency(record_to_write, (long) record_to_write.get(Types.LABEL), store)) {
//                        throw new BucketException("Structural integrity incompatibility");
//                    }
//                } catch (final KeyNotFoundException e) {
//                    // this cannot happen - label checked in if .. so .. just let it go
//                } catch (final IOException e) {
//                    throw new BucketException("I/O exception checking consistency");
//                } catch (final TypeMismatchFoundException e) {
//                    throw new BucketException("Type mismatch checking consistency");
//                }
//            }

        if( record_to_write instanceof LXP) {
            writeData((LXP) record_to_write);
        } else {
            throw new RuntimeException( "This version can only persist LXPs not PersistentObjects - FIXME??");
        }
    }

    private void writeData(LXP record_to_write) {

        Map<String, Object> props = record_to_write.serializeFieldsToMap();

        try (Session session = bridge.getNewSession(); Transaction tx = session.beginTransaction();) {
            String CREATE_LXP_QUERY = "CREATE (n:STORR_LXP $props) RETURN n";
            String ADD_LXP_TO_BUCKET_QUERY = "MATCH(b:STORR_BUCKET),(l:STORR_LXP) WHERE id(b)=$bucket_id AND id(l)=$new_id CREATE (b)-[r:STORR_MEMBER]->(l)";

            Result result = tx.run(CREATE_LXP_QUERY, parameters("props", props));

            List<Node> nodes = result.list(r -> r.get("n").asNode());
            if( nodes.size() != 1 ) {
                throw new RepositoryException( "Bucket id not found for:" + bucket_name );
            }
            long new_id = nodes.get(0).id();

            System.out.println( "Newly created LXP neo id = " + new_id );
            System.out.println( "Bucket neo id = " + this.neo_id );

            tx.run(ADD_LXP_TO_BUCKET_QUERY,parameters("bucket_id", this.neo_id, "new_id", new_id));
            tx.commit();
        } catch (RepositoryException e) {
            e.printStackTrace();
        }
    }


    public synchronized int size() throws BucketException {

//        if (size == -1) {
//            try {
//                size = (int) Files.list(directory.toPath()).count() - 2; // do not count . and ..
//            } catch (final IOException e) {
//                throw new BucketException("Cannot determine size - I/O error");
//            }
//        }
//        return size;
        return 0;
    }

    /**
     * `
     * called by Watcher service
     */
    public synchronized void invalidateCache() {

        size = -1;
        cached_oids = null;
        object_cache = newCache(repository, cache_size, this); // There may be extent references to these objects in the heap which should be invalidated.
    }

    /**
     * ******** Transaction support **********
     */

    @Override
    public void delete(final long oid) throws BucketException {

        System.out.println("Unimplemented");
        throw new RuntimeException("Unimplemented");
    }

}
