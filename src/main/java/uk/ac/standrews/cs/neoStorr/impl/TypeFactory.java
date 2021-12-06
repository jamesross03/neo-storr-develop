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
import uk.ac.standrews.cs.neoStorr.interfaces.IReferenceType;
import uk.ac.standrews.cs.neoStorr.interfaces.IRepository;
import uk.ac.standrews.cs.neoStorr.interfaces.IStore;
import uk.ac.standrews.cs.neoStorr.types.LXPReferenceType;
import uk.ac.standrews.cs.neoStorr.types.Types;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by al on 12/09/2014.
 */
public class TypeFactory {

    static final String TYPES_REPOSITORY_NAME = "Types_repository";
    static final String TYPE_REPS_BUCKET_NAME = "Type_reps";
    static final String TYPE_NAMES_BUCKET_NAME = "Type_names";
    static final String NAME_FIELD_NAME = "name";
    static final String KEY_FIELD_NAME = "key";

    private final IBucket type_reps_bucket;
    private final IBucket type_name_bucket;
    private final IStore store;

    private IRepository type_repository;

    private final Map<String, IReferenceType> names_to_type_cache = new HashMap<>();
    private final Map<Long, IReferenceType> ids_to_type_cache = new HashMap<>();

    protected TypeFactory(final IStore store) throws RepositoryException {

        this.store = store;

        setupTypeRepository(TYPES_REPOSITORY_NAME);

        type_reps_bucket = getBucket(TYPE_REPS_BUCKET_NAME);
        type_name_bucket = getBucket(TYPE_NAMES_BUCKET_NAME);
        loadCaches();

        // initialise predefined types - only 1 for now
        if (!type_repository.bucketExists(TYPE_REPS_BUCKET_NAME)) createAnyType();
    }

    private void createAnyType() {

        final DynamicLXP type_rep = Types.getTypeRep(StaticLXP.class);
        final LXPReferenceType lxp_type = new LXPReferenceType(type_rep);
        doHousekeeping("lxp", lxp_type);
    }

    public IReferenceType createType(final Class c, final String type_name) {

        final DynamicLXP type_rep = Types.getTypeRep(c);
        final LXPReferenceType ref_type = new LXPReferenceType(type_rep);
        doHousekeeping(type_name, ref_type);
        return ref_type;
    }

    public IReferenceType getTypeWithName(final String name) {
        return names_to_type_cache.get(name);
    }

    public boolean containsKey(final String name) {
        return names_to_type_cache.containsKey(name);
    }

    public IReferenceType typeWithId(final long id) {
        return ids_to_type_cache.get(id);
    }

    private void loadCaches() {

        try {
            for (final DynamicLXP lxp : (Iterable<DynamicLXP>) type_name_bucket.getInputStream()) {

                // as set up in @code nameValuePair below.
                final String name = (String) lxp.get(NAME_FIELD_NAME);
                final long type_key = (long) lxp.get(KEY_FIELD_NAME);

                final LXP type_rep = (LXP) type_reps_bucket.getObjectById(type_key);
                final LXPReferenceType reference = new LXPReferenceType((DynamicLXP) (type_rep));

                names_to_type_cache.put(name, reference);
                ids_to_type_cache.put(type_key, reference);
            }
        } catch (final BucketException e) {
            throw new RuntimeException("IO exception getting iterator over type name field_storage", e);
        }
    }

    private void doHousekeeping(final String type_name, final LXPReferenceType ref_type) {

        try {
            final LXP type_rep = ref_type.getRep();
            final LXP name_value = nameValuePair(type_name, type_rep.getId());

            type_reps_bucket.makePersistent(type_rep);
            type_name_bucket.makePersistent(name_value);

        } catch (final BucketException e) {
            throw new RuntimeException("Bucket exception adding type " + type_name + " to types bucket", e);
        }

        names_to_type_cache.put(type_name, ref_type);
        ids_to_type_cache.put(ref_type.getId(), ref_type);
    }

    private LXP nameValuePair(final String type_name, final long type_key) {

        final DynamicLXP lxp = new DynamicLXP();

        lxp.put(NAME_FIELD_NAME, type_name);
        lxp.put(KEY_FIELD_NAME, type_key);

        return lxp;
    }

    private void setupTypeRepository(final String type_repo_name) throws RepositoryException {

        if (store.repositoryExists(type_repo_name)) {
            type_repository = store.getRepository(type_repo_name);
        } else {
            type_repository = store.makeRepository(type_repo_name);
        }
    }

    private IBucket getBucket(final String bucket_name) throws RepositoryException {

        if (type_repository.bucketExists(bucket_name)) {
            return type_repository.getBucket(bucket_name);
        } else {
            return type_repository.makeBucket(bucket_name);
        }
    }
}
