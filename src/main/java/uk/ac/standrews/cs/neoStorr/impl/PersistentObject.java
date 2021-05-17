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
package uk.ac.standrews.cs.neoStorr.impl;

import org.json.JSONException;
import org.json.JSONWriter;
import uk.ac.standrews.cs.neoStorr.impl.exceptions.PersistentObjectException;
import uk.ac.standrews.cs.neoStorr.impl.exceptions.RepositoryException;
import uk.ac.standrews.cs.neoStorr.interfaces.IBucket;
import uk.ac.standrews.cs.neoStorr.interfaces.IStoreReference;

import java.security.SecureRandom;
import java.util.Map;
import java.util.Objects;


public abstract class PersistentObject implements Comparable<PersistentObject> {

    protected long $$$$id$$$$id$$$$;
    protected IBucket $$$bucket$$$bucket$$$ = null;

    private static final SecureRandom RANDOM = new SecureRandom();

    public PersistentObject() {      // don't know the repo or the bucket
        this.$$$$id$$$$id$$$$ = getNextFreePID();
    }

    public PersistentObject(final long object_id, final IBucket bucket) {

        this.$$$$id$$$$id$$$$ = object_id;
        this.$$$bucket$$$bucket$$$ = bucket;

        // This constructor used when about to be filled in with values.
    }

    public long getId() { return $$$$id$$$$id$$$$; }

    public Object getBucket() throws RepositoryException {
        return $$$bucket$$$bucket$$$;
    }

    public abstract IStoreReference getThisRef() throws PersistentObjectException;

    /**
     * Writes the state of the LXP to a Bucket.
     *
     * @param writer the stream to which the state is written.
     * @param bucket @throws JSONException if the record being written cannot be written to legal JSON.
     */
    public void serializeToJSON(final JSONWriter writer, final IBucket bucket) throws JSONException {

        this.$$$bucket$$$bucket$$$ = bucket;
        serializeToJSON(writer);
    }

    public abstract void serializeToJSON(JSONWriter writer) throws JSONException;

    public abstract Map<String,Object> serializeFieldsToMap();

    /**
     * @return the metadata associated with the class extending LXP base.
     * This may be static or dynamically created.
     * Two classes are provided corresponding to the above.
     */
    public abstract PersistentMetaData getMetaData();

    /**
     * @return a pseudo random positive long
     */
    private static long getNextFreePID() {

        long next_prn;
        do {
            next_prn = RANDOM.nextLong();
        } while (next_prn <= 0);

        return next_prn;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PersistentObject)) return false;
        PersistentObject that = (PersistentObject) o;
        return $$$$id$$$$id$$$$ == that.$$$$id$$$$id$$$$ && Objects.equals($$$bucket$$$bucket$$$, that.$$$bucket$$$bucket$$$);
    }

    @Override
    public int hashCode() {
        return Objects.hash($$$$id$$$$id$$$$, $$$bucket$$$bucket$$$);
    }

    public int compareTo(final PersistentObject o) {
        return Long.compare(this.getId(), o.getId());
    }
}
