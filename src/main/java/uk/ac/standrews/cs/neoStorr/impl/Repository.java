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

import org.neo4j.driver.Result;
import org.neo4j.driver.Session;
import org.neo4j.driver.types.Node;
import uk.ac.standrews.cs.neoStorr.impl.exceptions.BucketException;
import uk.ac.standrews.cs.neoStorr.impl.exceptions.RepositoryException;
import uk.ac.standrews.cs.neoStorr.interfaces.IBucket;
import uk.ac.standrews.cs.neoStorr.interfaces.IRepository;
import uk.ac.standrews.cs.neoStorr.interfaces.IStore;
import uk.ac.standrews.cs.neoStorr.util.NeoDbCypherBridge;

import java.util.*;

import static org.neo4j.driver.Values.parameters;

/**
 * A Collection of buckets.
 * Created by al on 11/05/2014.
 */
public class Repository implements IRepository {

    private static final String ILLEGAL_CHARS_MAC = ":";
    private static final String ILLEGAL_CHARS_LINUX = "/\0";
    private static final String ILLEGAL_CHARS_WINDOWS = "<>:\"/\\|?*";

    private static final String ILLEGAL_CHARS = ILLEGAL_CHARS_MAC + ILLEGAL_CHARS_LINUX + ILLEGAL_CHARS_WINDOWS;
    public static final String LEGAL_CHARS_PATTERN = "[^" + ILLEGAL_CHARS + "]*";

    private static final String MAKE_BUCKET_QUERY = "MATCH (r:STORR_REPOSITORY {name: $repo_name}) MERGE (r)-[c:STORR_CONTAINS]-(b:STORR_BUCKET {name:$bucket_name}) return b";
    private static final String BUCKET_EXISTS_QUERY = "MATCH (r:STORR_REPOSITORY {name: $repo_name})-[c:STORR_CONTAINS]-(b:STORR_BUCKET {name:$bucket_name}) return b";
    private static final String DELETE_BUCKET_QUERY = "MATCH (r:STORR_REPOSITORY {name: $repo_name})-[c:STORR_CONTAINS]-(b:STORR_BUCKET {name:$bucket_name}) DETACH DELETE b";
    private static final String ALL_BUCKET_NAMES_QUERY = "MATCH (r:STORR_REPOSITORY {name: $repo_name})-[c:STORR_CONTAINS]-(b:STORR_BUCKET) return b.name";

    private final IStore store;
    private final String repository_name;

    private final Map<String, NeoBackedBucket> bucket_cache;
    private final NeoDbCypherBridge bridge;

    Repository(final IStore store, final String repository_name) throws RepositoryException {

        if (!repositoryNameIsLegal(repository_name)) {
            throw new RepositoryException("Illegal repository name <" + repository_name + ">");
        }

        this.store = store;
        this.bridge = store.getBridge();
        this.repository_name = repository_name;
        bucket_cache = new HashMap<>();
    }

    @Override
    public IBucket makeBucket(final String bucket_name) throws RepositoryException {

        makeBucketInNeo(bucket_name); // Throws exception if it already exists in Db
        NeoBackedBucket bucket = new NeoBackedBucket(this, bucket_name, getNeoBucketIDFromDb(bucket_name));
        bucket_cache.put(bucket_name, bucket);
        return bucket;
    }

    @Override
    public <T extends LXP> IBucket<T> makeBucket(final String bucket_name, final Class<T> bucketType) throws RepositoryException, BucketException {

        makeBucketInNeo(bucket_name); // Throws exception if it already exists in Db
        NeoBackedBucket<T> bucket = new NeoBackedBucket<>(this, bucket_name, getNeoBucketIDFromDb(bucket_name), bucketType);
        bucket.setPersistentTypeLabelID();
        bucket_cache.put(bucket_name, bucket);
        return bucket;
    }

    /**
     * @param bucket_name the name of the bucket to create
     * @throws RepositoryException
     */
    private void makeBucketInNeo(final String bucket_name) throws RepositoryException {

        if (bucketExists(bucket_name)) throw new RepositoryException("Repo: " + bucket_name + " already exists");

        try (final Session session = bridge.getNewSession()) {
            session.executeWrite(tx -> tx.run(MAKE_BUCKET_QUERY, parameters("repo_name", repository_name, "bucket_name", bucket_name)));
        }
    }

    @Override
    public boolean bucketExists(final String bucket_name) {

        try (final Session s = bridge.getNewSession()) {

            Result result = s.run(BUCKET_EXISTS_QUERY, parameters("repo_name", repository_name, "bucket_name", bucket_name));

            List<Node> nodes = result.list(r -> r.get("b").asNode());
            return nodes.size() == 1;
        }
    }

    public String getNeoBucketIDFromDb(final String bucket_name) throws RepositoryException {

        try (final Session s = bridge.getNewSession()) {

            Result result = s.run(BUCKET_EXISTS_QUERY, parameters("repo_name", this.repository_name, "bucket_name", bucket_name));

            List<Node> nodes = result.list(r -> r.get("b").asNode());
            if (nodes.isEmpty()) throw new RepositoryException("Bucket id not found for: " + bucket_name);

            return nodes.get(0).elementId();
        }
    }

    @Override
    public void deleteBucket(final String bucket_name) {

        try (final Session session = bridge.getNewSession();) {
            session.executeWrite(tx -> tx.run(DELETE_BUCKET_QUERY, parameters("repo_name", this.repository_name, "bucket_name", bucket_name)));
        }
        bucket_cache.remove(bucket_name);
    }

    @Override
    public IBucket getBucket(final String bucket_name) throws RepositoryException {

        final IBucket bucket = bucket_cache.get(bucket_name);
        if (bucket != null) return bucket;

        if (bucketExists(bucket_name)) return new NeoBackedBucket(this, bucket_name, getNeoBucketIDFromDb(bucket_name));

        throw new RepositoryException("bucket does not exist with name: <" + bucket_name + ">");
    }

    @Override
    public <T extends LXP> IBucket<T> getBucket(final String bucket_name, final Class<T> bucketType) throws RepositoryException {

        final IBucket bucket = bucket_cache.get(bucket_name);

        if (bucket != null) {
            if (((NeoBackedBucket) bucket).bucketTypeIsCorrect(bucketType)) return bucket;

            throw new RepositoryException("bucket: " + bucket_name + " is not of type: <" + bucketType.getName() + ">");
        }

        if (bucketExists(bucket_name)) {

            final NeoBackedBucket<T> neo_bucket = new NeoBackedBucket<>(this, bucket_name, getNeoBucketIDFromDb(bucket_name), bucketType);
            if (!neo_bucket.persistentLabelIsCorrect())
                throw new RepositoryException("bucket: " + bucket_name + " is not of type: <" + bucketType.getName() + ">");

            return neo_bucket;
        }

        throw new RepositoryException("bucket does not exist with name: <" + bucket_name + ">");
    }

    @Override
    public String getName() {
        return repository_name;
    }

    @Override
    public IStore getStore() {
        return store;
    }

    /**
     * Check that the repository name is legal.
     *
     * @param name to be checked
     * @return true if the name is legal
     */
    public static boolean bucketNameIsLegal(final String name) {

        return name.matches(LEGAL_CHARS_PATTERN);
    }

    public static boolean repositoryNameIsLegal(final String name) {

        return name.matches(LEGAL_CHARS_PATTERN);
    }

    @Override
    public Iterator<String> getBucketNameIterator() {

        try (final Session s = bridge.getNewSession()) {
            Result result = s.run(ALL_BUCKET_NAMES_QUERY, parameters("repo_name", repository_name));
            return result.list(r -> r.get("b.name").asString()).iterator();
        }
    }

    @Override
    public <T extends LXP> Iterator<IBucket<T>> getIterator(Class<T> bucketType) {

        final List<IBucket<T>> typed_buckets = new ArrayList<>();
        final Iterator<String> name_iterator = getBucketNameIterator();

        while (name_iterator.hasNext()) {

            try {
                IBucket<T> bucket = getBucket(name_iterator.next(), bucketType);
                typed_buckets.add(bucket);

            } catch (RepositoryException e) {
                // Ignore the bucket if it's the wrong type.
            }
        }

        return typed_buckets.iterator();
    }
}
