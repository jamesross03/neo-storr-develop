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
import uk.ac.standrews.cs.neoStorr.interfaces.IReferenceType;
import uk.ac.standrews.cs.neoStorr.interfaces.IType;
import uk.ac.standrews.cs.neoStorr.types.LXPReferenceType;

import java.io.StringWriter;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Vector;

/**
 * This is a Labelled Cross Product (a tuple).
 * This is the basic unit that is stored in Buckets.
 * Higher order language level types may be constructed above this basic building block.
 * LXP provides a thin wrapper over a Map (providing name value lookup) along with identity and the ability to save and recover persistent versions (encoded in JSON).
 */
public abstract class StaticLXP extends LXP {

    public StaticLXP() {
        super();
    }

    public StaticLXP(final String persistent_object_id, final Map properties, final IBucket bucket) throws PersistentObjectException {

        super(persistent_object_id, bucket);
        initialiseProperties(properties);
        fixReferences();
    }

    protected void fixReferences() throws PersistentObjectException {

        final IReferenceType type = getMetaData().getType();
        final Collection<String> labels = type.getLabels();

        for (final String label : labels) {

            final IType t = type.getFieldType(label);
            if (t instanceof LXPReferenceType) {

                final String serialised = (String) get(label);
                final String class_name = extractRefType((LXPReferenceType) type);

                try {
                    final Class c = getClass(class_name);

                    final Method makeref = c.getDeclaredMethod("makeRef", String.class);
                    final LXPReference newref = (LXPReference) makeref.invoke(null, serialised);
                    put(label, newref);

                } catch (final Exception e) {
                    throw new PersistentObjectException("Error in reflective constructor call", e);
                }
            }
        }
    }

    private Class getClass(final String name) throws ClassNotFoundException, NoSuchFieldException, IllegalAccessException {

        final Iterator<Class> iterator = getLoadedClasses(Thread.currentThread().getContextClassLoader());

        while (iterator.hasNext()) {

            final Class c = iterator.next();
            if (c.getSimpleName().equals(name)) return c;
        }
        throw new ClassNotFoundException("Could not find a loaded class with name <" + name + ">");
    }

    private static Iterator<Class> getLoadedClasses(final ClassLoader class_loader) throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {

        Class c = class_loader.getClass();
        while (c != ClassLoader.class) c = c.getSuperclass();

        final Field f = c.getDeclaredField("classes");
        f.setAccessible(true);
        return ((Vector<Class>) f.get(class_loader)).iterator();
    }

    /**
     * @param t - should be a reference type containing a string of form STOREREF[Classname]
     */
    private String extractRefType(LXPReferenceType t) {

        final String rep = t.getRep().getString(0); // slot zero contains "STOREREF[Classname]"
        return rep.substring("STOREREF[".length(), rep.length() - 1);
    }

    @Override
    public void check(final String key) {

        if (key == null || key.equals("")) throw new RuntimeException("null key");

        final Map<String, Integer> field_name_to_slot = getMetaData().getFieldNamesToSlotNumbers();

        if (!field_name_to_slot.containsKey(key)) throw new RuntimeException(key);
    }

    // Java housekeeping

    @Override
    public boolean equals(final Object o) {

        return (o instanceof StaticLXP) && (compareTo((StaticLXP) o)) == 0;
    }

    public String toString() {

        try {
            final StringWriter writer = new StringWriter();
            serializeToJSON(new JSONWriter(writer));
            return writer.toString();

        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }

    public int hashCode() {
        return getId().hashCode();
    }

    public abstract LXPMetaData getMetaData();
}
