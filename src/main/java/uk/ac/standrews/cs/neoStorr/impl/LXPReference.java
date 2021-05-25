/*
 * Copyright 2017 Systems Research Group, University of St Andrews:
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

import uk.ac.standrews.cs.neoStorr.impl.exceptions.BucketException;
import uk.ac.standrews.cs.neoStorr.impl.exceptions.ReferenceException;
import uk.ac.standrews.cs.neoStorr.impl.exceptions.RepositoryException;
import uk.ac.standrews.cs.neoStorr.impl.exceptions.StoreException;
import uk.ac.standrews.cs.neoStorr.interfaces.IBucket;
import uk.ac.standrews.cs.neoStorr.interfaces.IRepository;
import uk.ac.standrews.cs.neoStorr.interfaces.IStore;
import uk.ac.standrews.cs.neoStorr.interfaces.IStoreReference;
import uk.ac.standrews.cs.neoStorr.types.LXPBaseType;
import uk.ac.standrews.cs.neoStorr.types.LXP_SCALAR;

import java.lang.ref.WeakReference;

/**
 * Created by al on 23/03/15.
 */
public class LXPReference<T extends LXP> extends StaticLXP implements IStoreReference<T> {

    private static LXPMetadata static_md;

    static {
        try {
            static_md = new LXPMetadata(LXPReference.class, "LXPReference");

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @LXP_SCALAR(type = LXPBaseType.STRING)
    public static int REPOSITORY;

    @LXP_SCALAR(type = LXPBaseType.STRING)
    public static int BUCKET;

    @LXP_SCALAR(type = LXPBaseType.LONG)
    public static int OID;

    private static final String SEPARATOR = "/";

    private WeakReference<T> ref = null;

    /**
     * @param serialized - a String of form repo_name SEPARATOR bucket_name SEPARATOR oid
     */
    public LXPReference(String serialized) throws ReferenceException {

        try {
            String[] tokens = serialized.split(SEPARATOR);
            put(REPOSITORY, tokens[0]);
            put(BUCKET, tokens[1]);
            put(OID, Long.parseLong(tokens[2]));
        } catch (ArrayIndexOutOfBoundsException | NumberFormatException e) {
            throw new ReferenceException(e);
        }
    }

    public LXPReference(String repo_name, String bucket_name, long oid) {

        super();
        this.put(REPOSITORY, repo_name);
        this.put(BUCKET, bucket_name);
        this.put(OID, oid);
        // don't bother looking up cache reference on demand or by caller
    }

    public LXPReference(IRepository repo, IBucket bucket, T reference) {
        this(repo.getStore(), repo.getName(), bucket.getName(), reference);
    }

    private LXPReference(IStore store, String repo_name, String bucket_name, T reference) {
        this(repo_name, bucket_name, reference.getId());
        ref = new WeakReference<T>(reference);   // TODO was weakRef - make softRef??
    }

    public LXPReference(LXP record) {

        this((String) record.get(REPOSITORY), (String) record.get(BUCKET), (long) record.get(OID));
        // don't bother looking up cache reference on demand
    }

    @Override
    public String getRepositoryName() {
        return (String) get(REPOSITORY);
    }

    @Override
    public String getBucketName() {
        return (String) get(BUCKET);
    }

    @Override
    public Long getOid() {
        return (long) get(OID);
    }

    public LXP getReferend() throws RepositoryException, BucketException {
        IBucket b = getBucket();
        return getReferend( b );
    }


    public T getReferend(Class clazz) throws BucketException, RepositoryException {

        IBucket<T> b = getBucket(clazz);
        return getReferend(b);
    }

    private T getReferend(IBucket<T> b) throws BucketException {

        // First see if we have a cached reference.
        if (ref != null) {
            T result = ref.get();
            if (result != null) {
                return result;
            }
        }
        try {
            T result = b.getObjectById(getOid());
            ref = new WeakReference<T>(result);  // cache the object we have just loaded.
            return result;
        } catch (StoreException e) {
            throw new BucketException(e);
        }
    }

    public IBucket<T> getBucket(Class clazz) throws BucketException, RepositoryException {
        if( ref != null ) {
            T obj = ref.get();
            if (obj != null) {
                return getBucket(clazz);
            }
        }
        return Store.getInstance().getRepository(getRepositoryName()).getBucket(getBucketName(), clazz);
    }

    public IBucket getBucket() throws RepositoryException {
        if( ref != null ) {
            LXP obj = ref.get();
            if (obj != null) {
                return (IBucket) obj.getBucket();
            }
        }
        return Store.getInstance().getRepository(getRepositoryName()).getBucket(getBucketName());
    }

    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (obj instanceof LXPReference) {
            LXPReference sr = (LXPReference) obj;
            return sr == this || sr.getOid().equals(this.getOid());
        } else {
            return false;
        }
    }

    public String toString() {
        return getRepositoryName() + SEPARATOR + getBucketName() + SEPARATOR + getOid();
    }

    @Override
    public LXPMetadata getMetaData() {
        return static_md;
    }
}
