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

import org.neo4j.driver.Result;
import org.neo4j.driver.Session;
import org.neo4j.driver.types.Node;
import uk.ac.standrews.cs.storr.impl.exceptions.RepositoryException;
import uk.ac.standrews.cs.storr.interfaces.*;
import uk.ac.standrews.cs.storr.util.NeoDbCypherBridge;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static org.neo4j.driver.Values.parameters;

/**
 * A Collection of buckets identified by a file path representing its root.
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

    private final IStore store;
    private final String repository_name;

    private final Map<String, IBucket> bucket_cache;
    private final NeoDbCypherBridge bridge;

    Repository(IStore store, String repository_name) throws RepositoryException {

        if (!repositoryNameIsLegal(repository_name)) {
            throw new RepositoryException("Illegal repository name <" + repository_name + ">");
        }

        this.store = store;
        this.bridge = store.getBridge();
        this.repository_name = repository_name;
        bucket_cache = new HashMap<String, IBucket>();
    }

    @Override
    public IBucket makeBucket(final String bucket_name) throws RepositoryException {

        makeBucketInNeo(bucket_name);
        IBucket bucket = new NeoBackedBucket(this, bucket_name, getNeoBucketID(bucket_name));
        bucket_cache.put(bucket_name, bucket);
        return bucket;
    }

    @Override
    public <T extends PersistentObject> IBucket<T> makeBucket(final String bucket_name, Class<T> bucketType) throws RepositoryException {

        makeBucketInNeo(bucket_name);
        IBucket<T> bucket = new NeoBackedBucket(this, bucket_name, bucketType, getNeoBucketID(bucket_name));
        bucket_cache.put(bucket_name, bucket);
        return bucket;
    }

    /**
     * @param bucket_name - the name of the bucket to create
     * @return the Neo4J bucket id
     * @throws RepositoryException
     */
    private void makeBucketInNeo(String bucket_name) throws RepositoryException {
        if (bucketExists(bucket_name)) {
            throw new RepositoryException("Repo: " + bucket_name + " already exists" );
        }

        try ( Session session = bridge.getNewSession() )
        {
            session.writeTransaction(tx -> tx.run(MAKE_BUCKET_QUERY, parameters("repo_name", this.repository_name, "bucket_name", bucket_name)));

        }
    }

    @Override
    public <T extends LXP> IIdtoLXPMap<T> makeIdtoLXPMap(String name, Class<T> bucketType) throws RepositoryException {
        return null;
    }

    @Override
    public <T extends LXP> IStringtoILXPMap<T> makeStringtoLXPMap(String name, Class<T> bucketType) throws RepositoryException {
        return null;
    }

    @Override
    public boolean bucketExists(final String bucket_name) {

        Result result = bridge.getNewSession().run(BUCKET_EXISTS_QUERY,parameters("repo_name", this.repository_name, "bucket_name", bucket_name));
        if( result == null ) {
            return false;
        }
        List<Node> nodes = result.list(r -> r.get("b").asNode());
        if( nodes.size() == 0 ) {
            return false;
        }
        return true;
    }

    public long getNeoBucketID(final String bucket_name) throws RepositoryException {

        Result result = bridge.getNewSession().run(BUCKET_EXISTS_QUERY,parameters("repo_name", this.repository_name, "bucket_name", bucket_name));
        if( result == null ) {
            throw new RepositoryException( "getNeoBucketID: (1) Bucket id not found for: " + bucket_name );
        }
        List<Node> nodes = result.list(r -> r.get("b").asNode());
        if( nodes.size() != 1 ) {
            throw new RepositoryException( "getNeoBucketID: (2) Bucket id not found for: " + bucket_name );
        }
        return nodes.get(0).id();
    }

    @Override
    public void deleteBucket(final String bucket_name) throws RepositoryException {

        try( Session session = bridge.getNewSession(); ) {
            session.writeTransaction(tx -> tx.run(DELETE_BUCKET_QUERY,parameters("repo_name", this.repository_name, "bucket_name", bucket_name)));
        }
        bucket_cache.remove(bucket_name);
    }

    @Override
    public IBucket getBucket(final String bucket_name) throws RepositoryException {

        if (bucketExists(bucket_name)) {
            final IBucket bucket = bucket_cache.get(bucket_name);
            return bucket != null ? bucket : new NeoBackedBucket(this,bucket_name, getNeoBucketID(bucket_name));
        }
        throw new RepositoryException("bucket does not exist: " + bucket_name);
    }

    @Override
    public <T extends PersistentObject> IBucket<T> getBucket(final String bucket_name, final Class<T> bucketType) throws RepositoryException {

        if (bucketExists(bucket_name)) {

            final IBucket bucket = bucket_cache.get(bucket_name);
            return bucket != null ? bucket : new NeoBackedBucket<T>(this,bucket_name, getNeoBucketID(bucket_name));
        }
        throw new RepositoryException("bucket does not exist: " + bucket_name);
    }

    @Override
    public <T extends LXP> IIdtoLXPMap<T> getIdtoLXPMap(String name, Class<T> bucketType) throws RepositoryException {

        return  null; // TODO new 8888 IdtoILXPMap( name, this, bucketType, false );
    }

    @Override
    public <T extends LXP> IStringtoILXPMap<T> getStringtoLXPMap(String name, Class<T> bucketType) throws RepositoryException {

        return null; // TODO 8888 new StringtoILXPMap( name, this, bucketType, false );
    }

    @Override
    public Iterator<String> getBucketNameIterator() {

        throw new RuntimeException("Code commented"); //return new BucketNamesIterator(repository_directory);  // TODO 8888
    }

    @Override
    public <T extends PersistentObject> Iterator<IBucket<T>> getIterator(Class<T> bucketType) {
        throw new RuntimeException("Code commented"); //return new BucketIterator(this, repository_directory, bucketType); // TODO 8888
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
    public static boolean bucketNameIsLegal(String name) {

        return name.matches(LEGAL_CHARS_PATTERN);
    }

    public static boolean repositoryNameIsLegal(String name) {

        return name.matches(LEGAL_CHARS_PATTERN);
    }
}
