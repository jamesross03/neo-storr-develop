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
import uk.ac.standrews.cs.neoStorr.impl.exceptions.IllegalKeyException;
import uk.ac.standrews.cs.neoStorr.impl.exceptions.PersistentObjectException;
import uk.ac.standrews.cs.neoStorr.interfaces.IBucket;
import uk.ac.standrews.cs.neoStorr.interfaces.IReferenceType;
import uk.ac.standrews.cs.neoStorr.interfaces.IType;
import uk.ac.standrews.cs.neoStorr.types.LXPReferenceType;

import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
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

    public StaticLXP(final long persistent_object_id, Map properties, final IBucket bucket) throws PersistentObjectException {
        super( persistent_object_id, bucket );
        initialiseProperties( properties );
        fixReferences();
    }

    protected void fixReferences() throws PersistentObjectException {
        IReferenceType type = getMetaData().getType();
        Collection<String> labels = type.getLabels();
        for( String label : labels ) {
            IType t = type.getFieldType(label);
            if( t instanceof LXPReferenceType ) {
                String serialised = (String) this.get( label );
                String classname = extractRefType( (LXPReferenceType) type );

                try {
                    Class clazz = getClass(classname);

                    Method makeref = clazz.getDeclaredMethod("makeRef", String.class);
                    LXPReference newref = (LXPReference) makeref.invoke( null,serialised );
                    this.put( label, newref );


                } catch (final NoSuchMethodException e) {
                    throw new PersistentObjectException( "Error in reflective constructor call:" );
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                } catch (InvocationTargetException e) {
                    e.printStackTrace();
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                } catch (NoSuchFieldException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private Class getClass(String name) throws ClassNotFoundException, NoSuchFieldException, IllegalAccessException {

        Iterator<Class> iterator = getLoadedClasses(Thread.currentThread().getContextClassLoader());
        while( iterator.hasNext() ) {
            Class clazz = iterator.next();
            String clazz_name = clazz.getName();
            if( clazz_name.endsWith(name)) {
                return clazz;
            }
        }
        throw new ClassNotFoundException( "Could not find a loaded class with name <" + name + ">");
    }

    private static Iterator<Class> getLoadedClasses(ClassLoader CL)
            throws NoSuchFieldException, SecurityException,
            IllegalArgumentException, IllegalAccessException {
        Class CL_class = CL.getClass();
        while (CL_class != java.lang.ClassLoader.class) {
            CL_class = CL_class.getSuperclass();
        }
        java.lang.reflect.Field ClassLoader_classes_field = CL_class
                .getDeclaredField("classes");
        ClassLoader_classes_field.setAccessible(true);
        Vector<Class> classes = (Vector<Class>) ClassLoader_classes_field.get(CL);
        return classes.iterator();
    }

    /**
     *
     * @param t - should be a reference type containing a string of form STOREREF[Classname]
     * @return
     */
    private String extractRefType(LXPReferenceType t) {
        String rep = t.getRep().getString(0); // slot zero contains "STOREREF[Classname]"
        String classname = rep.substring( 9,rep.length() -1 );
        return classname;
    }


    @Override
    public void check(final String key) throws IllegalKeyException {

        if (key == null || key.equals("")) {
            throw new IllegalKeyException("null key");
        }
        final Map<String, Integer> field_name_to_slot = getMetaData().getFieldNamesToSlotNumbers();

        if( ! field_name_to_slot.containsKey(key) ) {
            throw new IllegalKeyException( key );
        }
    }

    // Java housekeeping

    @Override
    public boolean equals(final Object o) {

        return (o instanceof StaticLXP) && (compareTo((StaticLXP) o)) == 0;
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
        return (int) getId();
    }

    public abstract LXPMetadata getMetaData();
}
