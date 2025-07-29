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

import uk.ac.standrews.cs.neoStorr.impl.exceptions.BucketException;
import uk.ac.standrews.cs.neoStorr.impl.exceptions.RepositoryException;
import uk.ac.standrews.cs.neoStorr.interfaces.IBucket;
import uk.ac.standrews.cs.neoStorr.interfaces.IRepository;
import uk.ac.standrews.cs.neoStorr.interfaces.IStoreReference;
import uk.ac.standrews.cs.neoStorr.types.LXPBaseType;
import uk.ac.standrews.cs.neoStorr.types.LXP_SCALAR;

import java.lang.ref.WeakReference;

/**
 * Created by al on 23/03/15.
 */
public class LXPReference<T extends LXP> extends StaticLXP implements IStoreReference<T> {

    private static LXPMetaData static_md;

    static {
        static_md = new LXPMetaData(LXPReference.class, "LXPReference");
    }

    @LXP_SCALAR(type = LXPBaseType.STRING)
    public static int REPOSITORY;

    @LXP_SCALAR(type = LXPBaseType.STRING)
    public static int BUCKET;

    @LXP_SCALAR(type = LXPBaseType.STRING)
    public static int OID;

    private static final String SEPARATOR = "/";

    private WeakReference<T> ref = null;

    /**
     * @param serialized - a String of form repo_name SEPARATOR bucket_name SEPARATOR oid
     */
    public LXPReference(final String serialized) {

        try {
            final String[] tokens = serialized.split(SEPARATOR);

            put(REPOSITORY, tokens[0]);
            put(BUCKET, tokens[1]);
            put(OID, tokens[2]);

        } catch (ArrayIndexOutOfBoundsException | NumberFormatException e) {
            throw new RuntimeException(e);
        }
    }

    public LXPReference(final String repo_name, final String bucket_name, final String oid) {

        super();

        this.put(REPOSITORY, repo_name);
        this.put(BUCKET, bucket_name);
        this.put(OID, oid);
        // don't bother looking up cache reference on demand or by caller
    }

    public LXPReference(final IRepository repo, final IBucket bucket, final T reference) {
        this(repo.getName(), bucket.getName(), reference);
    }

    private LXPReference(final String repo_name, final String bucket_name, final T reference) {

        this(repo_name, bucket_name, reference.getId());
        ref = new WeakReference<>(reference);   // TODO was weakRef - make softRef??
    }

    public LXPReference(LXP record) {

        this((String) record.get(REPOSITORY), (String) record.get(BUCKET), (String) record.get(OID));
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
    public String getObjectId() {
        return (String) get(OID);
    }

    public LXP getReferend() throws BucketException, RepositoryException {

        return getReferend(getBucket());
    }

    public T getReferend(final Class c) throws BucketException, RepositoryException {

        // TODO class is ignored if this reference was created using an explicit reference.
        return getReferend(getBucket(c));
    }

    private T getReferend(final IBucket<T> bucket) throws BucketException {

        // First see if we have a cached reference.
        if (ref != null) {
            T result = ref.get();
            if (result != null) {
                return result;
            }
        }

        try {
            T result = bucket.getObjectById(getObjectId());
            ref = new WeakReference<>(result);  // cache the object we have just loaded.
            return result;

        } catch (RuntimeException e) {
            throw new BucketException(e);
        }
    }

    private IBucket<T> getBucket(final Class c) throws RepositoryException {

        if (ref != null) {
            T obj = ref.get();
            if (obj != null) {
                return (IBucket<T>) obj.getBucket();
            }
        }

        return Store.getInstance().getRepository(getRepositoryName()).getBucket(getBucketName(), c);
    }

    public IBucket getBucket() throws RepositoryException {

        if (ref != null) {
            LXP obj = ref.get();
            if (obj != null) {
                return (IBucket) obj.getBucket();
            }
        }

        return Store.getInstance().getRepository(getRepositoryName()).getBucket(getBucketName());
    }

    public boolean equals(final Object obj) {

        if (obj == null) return false;

        if (obj instanceof LXPReference) {
            LXPReference other_ref = (LXPReference) obj;
            return other_ref == this || other_ref.getObjectId() == getObjectId();
        }

        return false;
    }

    public String toString() {
        return getRepositoryName() + SEPARATOR + getBucketName() + SEPARATOR + getObjectId();
    }

    @Override
    public LXPMetaData getMetaData() {
        return static_md;
    }
}
