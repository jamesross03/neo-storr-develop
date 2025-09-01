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

import org.json.JSONWriter;
import uk.ac.standrews.cs.neoStorr.impl.exceptions.PersistentObjectException;
import uk.ac.standrews.cs.neoStorr.interfaces.IBucket;

import java.io.StringWriter;
import java.util.Map;

/**
 * This is a Dynamic Labelled Cross Product (a tuple).
 *  In this version the storage is dynamically assigned on-demand
 * LXP provides a thin wrapper over a Map (providing name value lookup) along with identity and the ability to save and recover persistent versions.
 */
public class DynamicLXP extends LXP {

    final LXPMetaData metadata = new LXPMetaData();

    public DynamicLXP() {
        super();
    }

    public DynamicLXP(final String object_id, final IBucket bucket) {
        super(object_id, bucket);
    }

    public DynamicLXP(final String persistent_object_id, Map<String,Object> properties, final IBucket bucket) throws PersistentObjectException {

        this(persistent_object_id, bucket);
        initialiseProperties( properties );
    }

    @Override
    public LXPMetaData getMetaData() {
        return metadata;
    }


    @Override
    public void check(final String key) {

        if (key == null || key.equals("")) {
            throw new RuntimeException("null key");
        }

        final Map<String, Integer> field_name_to_slot = metadata.getFieldNamesToSlotNumbers();
        final Map<Integer, String> slot_to_fieldname = metadata.getSlotNumbersToFieldNames();

        if (!field_name_to_slot.containsKey(key)) {

            final int next_slot = findFirstFree();
            field_name_to_slot.put(key, next_slot);
            slot_to_fieldname.put(next_slot, key);
        }
    }

    // Java housekeeping

    @Override
    public boolean equals(final Object o) {

        return (o instanceof DynamicLXP) && (compareTo((DynamicLXP) o)) == 0;
    }

    public String toString() {

        final StringWriter writer = new StringWriter();
        try {
            serializeToJSON(new JSONWriter(writer));
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
        return writer.toString();
    }

    public int hashCode() {
        return getId().hashCode();
    }
}
