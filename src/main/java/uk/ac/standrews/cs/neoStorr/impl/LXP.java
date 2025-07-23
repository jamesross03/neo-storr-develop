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

import org.json.JSONException;
import org.json.JSONWriter;
import uk.ac.standrews.cs.neoStorr.impl.exceptions.PersistentObjectException;
import uk.ac.standrews.cs.neoStorr.interfaces.IBucket;
import uk.ac.standrews.cs.neoStorr.interfaces.IRepository;
import uk.ac.standrews.cs.neoStorr.interfaces.IStoreReference;

import java.util.*;

/**
 * Abstract class for an LXP (labeled cross product class).
 *
 * @author al
 */
public abstract class LXP extends PersistentObject {

    public static final String STORR_ID_KEY = "STORR_ID";

    private static final int INITIAL_SIZE = 5;
    private static final int SIZE_INCREMENT = 5;

    private Object[] field_storage = new Object[INITIAL_SIZE];   // where the data lives in the LXP.
    private int next_free_slot = 0;

    public LXP() {
        super();
    }

    public LXP(final long object_id, final IBucket<?> bucket) {
        super(object_id, bucket);
    }

    /**
     * Checks to see if the given key is present in the lxp.
     * Dynamic classes are at liberty to add fields if required.
     *
     * @param key - a key to be checked
     * @throws RuntimeException if the key is not present or illegal
     */
    public abstract void check(String key);

    /**
     * @return the metadata associated with the class extending LXP base.
     * This may be static or dynamically created.
     * Two classes are provided corresponding to the above.
     */
    public abstract LXPMetaData getMetaData();

    /**
     * A getter method over labelled values in the LXPID
     *
     * @param slot - the slot number of the required field
     * @return the value associated with @param label
     */
    public Object get(final int slot) {

        try {
            return field_storage[slot];
        } catch (final IndexOutOfBoundsException e) {
            throw new RuntimeException("Illegal slot number: " + slot);
        }
    }

    /**
     * A getter method over labelled values in the LXP
     *
     * @param slot - the slot number of the required field
     * @return the value associated with @param label
     */
    public String getString(final int slot) {

        try {
            return (String) field_storage[slot];

        } catch (final IndexOutOfBoundsException e) {
            throw new RuntimeException("Illegal slot number: " + slot);
        } catch (final ClassCastException e) {
            throw new RuntimeException("expected String found: " + field_storage[slot].getClass().getName());
        }
    }

    /**
     * A getter method over labelled values in the LXP
     *
     * @param slot - the slot number of the required field
     * @return the value associated with @param label
     */
    public double getDouble(final int slot) {

        try {
            return (Double) field_storage[slot];
        } catch (final IndexOutOfBoundsException e) {
            throw new RuntimeException("Illegal slot number: " + slot);
        } catch (final ClassCastException e) {
            throw new RuntimeException("expected double found: " + field_storage[slot].getClass().getName());
        }
    }

    /**
     * A getter method over labelled values in the LXP
     *
     * @param slot - the slot number of the required field
     */
    public int getInt(final int slot) {

        try {
            return (Integer) field_storage[slot];
        } catch (final IndexOutOfBoundsException e) {
            throw new RuntimeException("Illegal slot number: " + slot);
        } catch (final ClassCastException e) {
            throw new RuntimeException("expected int found: " + field_storage[slot].getClass().getName());
        }
    }

    /**
     * A getter method over labelled values in the LXP
     *
     * @param slot - the slot number of the required field
     * @return the value associated with @param label
     */
    public boolean getBoolean(final int slot) {

        try {
            return (Boolean) field_storage[slot];
        } catch (final IndexOutOfBoundsException e) {
            throw new RuntimeException("Illegal slot number: " + slot);
        } catch (final ClassCastException e) {
            throw new RuntimeException("expected boolean found: " + field_storage[slot].getClass().getName());
        }
    }

    /**
     * A getter method over labelled values in the LXP
     *
     * @param slot - the slot number of the required field
     * @return the value associated with @param label
     */
    public long getLong(final int slot) {

        try {
            return (Long) field_storage[slot];
        } catch (final IndexOutOfBoundsException e) {
            throw new RuntimeException("Illegal slot number: " + slot);
        } catch (final ClassCastException e) {
            throw new RuntimeException("expected Long found: " + field_storage[slot].getClass().getName());
        }
    }

    /**
     * A getter method over labelled values in the LXP
     *
     * @param slot - the slot number of the required field
     * @return the list associated with @param label
     */
    public List<?> getList(final int slot) {

        try {
            return (List<?>) field_storage[slot];
        } catch (final IndexOutOfBoundsException e) {
            throw new RuntimeException("Illegal slot number: " + slot);
        } catch (final ClassCastException e) {
            throw new RuntimeException("expected List found: " + field_storage[slot].getClass().getName());
        }
    }

    /**
     * A getter method over labelled values in the LXP
     *
     * @param slot - the slot number of the required field
     * @return the value associated with @param label
     */
    public IStoreReference<?> getRef(final int slot) {

        try {
            return (IStoreReference<?>) field_storage[slot];
        } catch (final IndexOutOfBoundsException e) {
            throw new RuntimeException("Illegal slot number: " + slot);
        } catch (final ClassCastException e) {
            throw new RuntimeException("expected IStoreReference found: " + field_storage[slot].getClass().getName());
        }
    }

    /**
     * @return a reference to the object on which it was called.
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public IStoreReference<?> getThisRef() throws PersistentObjectException {

        if ($$$bucket$$$bucket$$$ == null) {
            throw new PersistentObjectException("Null bucket encountered in LXP (uncommited LXP reference) : " + this);
        }
        return new LXPReference($$$bucket$$$bucket$$$.getRepository(), $$$bucket$$$bucket$$$, this);
    }

    public IRepository getRepository() {

        if ($$$bucket$$$bucket$$$ == null) {
            return null;
        } else {
            return $$$bucket$$$bucket$$$.getRepository();
        }
    }

    /**
     * @return the value associated with the label supplied.
     */
    public Object get(final String label) {

        final Integer slot = getMetaData().getSlot(label);
        if (slot == null) {
            throw new RuntimeException("No field with name " + label + " in " + getId());
        }
        return field_storage[slot];
    }

    /**
     * A setter method over labelled values in the LXP
     *
     * @param slot  - the slot number of the required field
     * @param value - the value to associated with the @param label
     */
    public void put(final int slot, final Object value) {

        putValue(slot, value);
    }

    /**
     * A setter method over labelled values in the LXP
     *
     * @param slot  - the slot number of the required field
     * @param value - the value to associated with the @param label
     */
    public void put(final int slot, final String value) {

        putValue(slot, value);
    }

    /**
     * A setter method over labelled values in the LXP
     *
     * @param slot  - the slot number of the required field
     * @param value - the value to associated with the @param label
     */
    public void put(final int slot, final double value) {

        putValue(slot, value);
    }

    /**
     * A setter method over labelled values in the LXP
     *
     * @param slot  - the slot number of the required field
     * @param value - the value to associated with the @param label
     */
    public void put(final int slot, final int value) {

        putValue(slot, value);
    }

    /**
     * A setter method over labelled values in the LXP
     *
     * @param slot  - the slot number of the required field
     * @param value - the value to associated with the @param label
     */
    public void put(final int slot, final boolean value) {

        putValue(slot, value);
    }

    /**
     * A setter method over labelled values in the LXP
     *
     * @param slot  - the slot number of the required field
     * @param value - the value to associated with the @param label
     */
    public void put(final int slot, final long value) {

        putValue(slot, value);
    }

    /**
     * A setter method over labelled values in the LXP
     *
     * @param slot  - the slot number of the required field
     * @param value - the list to associated with the @param label
     */
    public void put(final int slot, final List<?> value) {

        putValue(slot, value);
    }

    /**
     * A setter method over labelled values in the LXP
     *
     * @param slot  - the slot number of the required field
     * @param value - the value to associated with the @param label
     */
    public void put(final int slot, final IStoreReference<?> value) {

        putValue(slot, value);
    }

    /**
     * Associated the value supplied with the supplied key in this object.
     *
     * @param key   - the key with which to associate a value.
     * @param value - the value to be added to the tuple.
     */
    public void put(final String key, final Object value) {

        check(key);
        field_storage[getMetaData().getSlot(key)] = value;
    }

    // Slot management

    private void putValue(final int slot, final Object value) {

        if (slot >= field_storage.length) {
            growStorage(slot);
        }
        field_storage[slot] = value;
    }

    private void copyArray(final int new_size) {
        field_storage = Arrays.copyOf(field_storage, new_size);
    }

    private void growStorage(final int requested_slot) {

        final int new_size = Math.max(requested_slot + SIZE_INCREMENT, field_storage.length + SIZE_INCREMENT); // Leave some space for expansion.
        copyArray(new_size);
    }

    /**
     * @return the first free slot in the storage array, grow the array if necessary
     */
    public Integer findFirstFree() {

        if (next_free_slot >= field_storage.length) {
            copyArray(field_storage.length + SIZE_INCREMENT);
        }
        return next_free_slot++;
    }

    // JSON Manipulation - write methods

    @Override
    public void serializeToJSON(final JSONWriter writer) throws JSONException {

        writer.object();
        serializeFieldsToJSON(writer);
        writer.endObject();
    }

    private void serializeFieldsToJSON(final JSONWriter writer) throws JSONException {

        for (int i = 0; i < getMetaData().getFieldCount(); i++) {

            final String key = getMetaData().getFieldName(i);
            writer.key(key);
            Object value = null;

            try {
                value = field_storage[i];

            } catch (IndexOutOfBoundsException e) {
                // if the static LXP has been dynamically created - e.g.
                // during type conversion, not all storage fields may exist
            }

            if (value instanceof ArrayList) {
                writer.array();
                for (final Object o : (List<?>) value) {
                    if (o instanceof LXP) {
                        writeReference(writer, (LXP) o);
                    } else {
                        writeSimpleValue(writer, o);
                    }
                }
                writer.endArray();
            } else {
                writeSimpleValue(writer, value);
            }
        }
    }

    @Override
    public Map<String, Object> serializeFieldsToMap() {

        Map<String, Object> map = new HashMap<>();
        for (int i = 0; i < getMetaData().getFieldCount(); i++) {
            final String key = getMetaData().getFieldName(i);
            try {
                final Object value = field_storage[i];
                addValueToMap(map, key, value);

            } catch (IndexOutOfBoundsException ignored) {
                // can occur if fields are missing - dynamic instantiation.
                // don't do anything.
            }
        }
        return map;
    }

    private void addValueToMap(final Map<String, Object> map, final String key, final Object value) {

        if (value instanceof LXPReference) {
            map.put(key, value.toString());
        } else if (value instanceof LXP) {
            addReferenceToMap(map, key, (LXP) value);
        } else {
            map.put(key, value);
        }
    }

    private void addReferenceToMap(final Map<String, Object> map, final String key, final LXP value) {

        try {
            map.put(key, value.getThisRef().toString());

        } catch (final PersistentObjectException e) {
            throw new JSONException("Cannot serialise reference");
        }
    }

    private static void writeReference(final JSONWriter writer, final LXP value) {

        try {
            writer.value(value.getThisRef().toString());

        } catch (final PersistentObjectException e) {
            throw new JSONException("Cannot serialise reference");
        }
    }

    private static void writeSimpleValue(final JSONWriter writer, final Object value) throws JSONException {

        if (value instanceof Double) {
            writer.value(((Double) (value)).doubleValue());
        } else if (value instanceof Integer) {
            writer.value(((Integer) (value)).intValue());
        } else if (value instanceof Boolean) {
            writer.value(((Boolean) (value)).booleanValue());
        } else if (value instanceof Long) {
            writer.value(((Long) (value)).longValue());
        } else {
            writer.value(value); // default is to write a string
        }
    }

    void initialiseProperties(final Map<String, Object> properties) throws JSONException {

        final Map<String, Integer> field_name_to_slot = getMetaData().getFieldNamesToSlotNumbers();

        for (final Map.Entry<String, Object> entry : properties.entrySet()) {

            final String key = entry.getKey(); // keep the keys identical whenever possible.

            if (!key.equals(STORR_ID_KEY)) { // these are not for public consumption - used in Neo to store storr id
                final Object value = entry.getValue();

                if (value != null) {
                    check(key);
                    put(field_name_to_slot.get(key), value);

                } else {
                    // can occur if dynamically constructed and data has missing field values.
                    // also for null refs
                    put(field_name_to_slot.get(key), (Object) null);
                }
            }
        }
    }
}
