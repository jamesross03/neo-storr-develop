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
package uk.ac.standrews.cs.neoStorr.types;

import uk.ac.standrews.cs.neoStorr.impl.DynamicLXP;
import uk.ac.standrews.cs.neoStorr.impl.LXP;
import uk.ac.standrews.cs.neoStorr.impl.StaticLXP;
import uk.ac.standrews.cs.neoStorr.impl.TypeFactory;
import uk.ac.standrews.cs.neoStorr.impl.exceptions.IllegalKeyException;
import uk.ac.standrews.cs.neoStorr.impl.exceptions.KeyNotFoundException;
import uk.ac.standrews.cs.neoStorr.impl.exceptions.TypeMismatchFoundException;
import uk.ac.standrews.cs.neoStorr.interfaces.IReferenceType;
import uk.ac.standrews.cs.neoStorr.interfaces.IStore;
import uk.ac.standrews.cs.neoStorr.interfaces.IType;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.Set;

/**
 * Created by al on 30/10/14.
 */
public class Types {

    public static final String LABEL = "$LABEL$";

    /**
     * Checks the type of a record (if there is one) is consistent with a supplied label (generally from a bucket)
     *
     * @param record        whose label is to be checked
     * @param type_label_id the label against which the checking is to be performed
     * @param <T>           the type of the record being checked
     * @return true if the labels are consistent
     */
    public static <T extends LXP> boolean checkLabelConsistency(final T record, long type_label_id, IStore store) {

        IReferenceType type = record.getMetaData().getType();

        if ( type != null ) { // if there is a type it must be correct
            try {
                return checkLabelsConsistentWith( (long) type.getId(), type_label_id, store);

            } catch (KeyNotFoundException | TypeMismatchFoundException e) {
                return false; // label not there or inappropriate type
            }
        } else {
            return true; // if there is no label that is OK
        }
    }

    /**
     * Checks that the content of a record is consistent with a supplied label (generally from a bucket)
     *
     * @param record        whose _structure is to be checked
     * @param type_label_id the label against which the checking is to be performed
     * @param <T>           the type of the record being checked
     * @return true if the structure is consistent
     * @throws IOException if one if thrown by the underlying subsystem(s)
     */
    public static <T extends LXP> boolean checkStructuralConsistency(final T record, long type_label_id, IStore store) throws IOException {

        IReferenceType bucket_type = store.getTypeFactory().typeWithId(type_label_id);
        return checkStructuralConsistency(record, bucket_type);
    }

    /**
     * Checks that the content of a record is consistent with a supplied label (generally from a bucket)
     *
     * @param record   whose _structure is to be checked
     * @param ref_type the type being checked against
     * @param <T>      the type of the record being checked
     * @return true if the structure is consistent
     */
    static <T extends LXP> boolean checkStructuralConsistency(final T record, IReferenceType ref_type) {

        Set<String> record_keys = record.getMetaData().getFields();

        Collection<String> required_labels = ref_type.getLabels();

        for (String label : required_labels) {
            if (!record_keys.contains(label)) {
                // required label not present
                return false;
            }
            // required label is present now check the types of the keys in the record
            try {
                Object value = record.get(label);

                if (!ref_type.getFieldType(label).valueConsistentWithType(value)) {
                    // label does not match expected type
                    return false;
                }
            } catch (TypeMismatchFoundException e) {
                // type mismatch
                return false;
            }
        }
        return true; // all matched to here we are finished!
    }

    /**
     * Checks the TYPE LABEL is consistent with a supplied label (generally from a bucket)
     *
     * @param supplied_label_id to be checked
     * @param type_label_id     the label against which the checking is to be performed
     * @return true if the labels are consistent
     */
    private static boolean checkLabelsConsistentWith(long supplied_label_id, long type_label_id, IStore store) {

        // do id check first
        if (type_label_id == supplied_label_id) {
            return true;
        }

        // if that doesn't work do structural check over type reps
        try {
            TypeFactory type_factory = store.getTypeFactory();

            IReferenceType stored_label = type_factory.typeWithId(supplied_label_id);
            IReferenceType required_label = type_factory.typeWithId(supplied_label_id);
            Collection<String> required = required_label.getLabels();
            for (String label : required) {
                if (required_label.getFieldType(label) != stored_label.getFieldType(label)) {
                    return false;
                }
            }
            return true;
        } catch (KeyNotFoundException | TypeMismatchFoundException e) {
            return false;
        }
    }

    static IType stringToType(String value, IStore store) {

        TypeFactory type_factory = store.getTypeFactory();

        if (LXPBaseType.STRING.name().equalsIgnoreCase(value)) {
            return LXPBaseType.STRING;
        }
        if (LXPBaseType.LONG.name().equalsIgnoreCase(value)) {
            return LXPBaseType.LONG;
        }
        if (LXPBaseType.INT.name().equalsIgnoreCase(value)) {
            return LXPBaseType.INT;
        }
        if (LXPBaseType.DOUBLE.name().equalsIgnoreCase(value)) {
            return LXPBaseType.DOUBLE;
        }
        if (LXPBaseType.BOOLEAN.name().equalsIgnoreCase(value)) {
            return LXPBaseType.BOOLEAN;
        }
        if( value.startsWith("STOREREF[") && value.endsWith("]")) { // it is a ref type
            String type_referenced_rep = value.substring(9, value.length() - 1); // the name of the type that the ref is to
            if (type_factory.containsKey(type_referenced_rep)) {
                IReferenceType type_referenced = type_factory.getTypeWithName(type_referenced_rep);
                return new LXPReferenceType((DynamicLXP) type_referenced.getRep());
            } else {
                throw new RuntimeException("Encountered reference to unknown type: " + type_referenced_rep );
            }
        }
        if (value.startsWith("[") && value.endsWith("]")) { // it is a list type
            String listcontents = value.substring(1, value.length() - 1);
            if (LXPBaseType.STRING.name().equalsIgnoreCase(listcontents) ||
                    LXPBaseType.LONG.name().equalsIgnoreCase(listcontents) ||
                    LXPBaseType.INT.name().equalsIgnoreCase(listcontents) ||
                    LXPBaseType.DOUBLE.name().equalsIgnoreCase(listcontents) ||
                    LXPBaseType.BOOLEAN.name().equalsIgnoreCase(listcontents)) {
                return LXPListBaseType.valueOf(listcontents);
            } else {
                // it may be a list of ref types
                if (type_factory.containsKey(listcontents)) {
                    IReferenceType list_contents_type = type_factory.getTypeWithName(listcontents);
                    return new LXPListRefType(list_contents_type, store);
                } else {
                    throw new RuntimeException("Encountered unknown array contents: " + listcontents);
                }
            }
        }
        if (type_factory.containsKey(value)) {
            return type_factory.getTypeWithName(value);
        }
        throw new RuntimeException("Encountered reference to type not defined: " + value);
    }

    public static DynamicLXP getTypeRep(Class c) {

        if ( StaticLXP.class.isAssignableFrom( c ) || DynamicLXP.class.isAssignableFrom( c ) ) {
            return getLXPTypeRep(c);
        } else {
            throw new RuntimeException( "Do not recognise persistent class: " + c.getName()  );
        }
    }

    public static DynamicLXP getLXPTypeRep(Class c) {

        DynamicLXP type_rep = new DynamicLXP();

        Field[] fields = c.getDeclaredFields();
        for (Field f : fields) {

            if (Modifier.isStatic(f.getModifiers())) {

                if (f.isAnnotationPresent(LXP_SCALAR.class)) {
                    if (f.isAnnotationPresent(LXP_REF.class) || f.isAnnotationPresent(LXP_LIST.class)) {
                        throw new RuntimeException("Conflicting labels: " + f.getName());
                    }
                    LXP_SCALAR scalar_type = f.getAnnotation(LXP_SCALAR.class);
                    try {
                        f.setAccessible(true);
                        String label_name = f.getName();
                        type_rep.put(label_name, scalar_type.type().name());
                    } catch (IllegalKeyException e) {
                        throw new RuntimeException("Illegal key in label: " + f.getName(), e);
                    }
                } else if (f.isAnnotationPresent(LXP_REF.class)) {
                    if (f.isAnnotationPresent(LXP_LIST.class)) {
                        throw new RuntimeException("Conflicting labels: " + f.getName());
                    }
                    LXP_REF ref_type = f.getAnnotation(LXP_REF.class);
                    String ref_type_name = ref_type.type(); // this is the name of the type that the reference refers to
                    try {
                        f.setAccessible(true);
                        String label_name = f.getName();
                        type_rep.put( label_name, "STOREREF[" + ref_type_name + "]" );
                    } catch (IllegalKeyException e) {
                        throw new RuntimeException("Illegal key in label: " + f.getName(), e);
                    }
                } else if (f.isAnnotationPresent(LXP_LIST.class)) {
                    LXP_LIST list_type = f.getAnnotation(LXP_LIST.class);
                    try {
                        f.setAccessible(true);
                        String label_name = f.getName();
                        LXPBaseType basetype = list_type.basetype();
                        String reftype = list_type.reftype();
                        if (basetype == LXPBaseType.UNKNOWN && reftype.equals(LXP_LIST.UNSPECIFIED_REF_TYPE)) {      // none specified
                            // no type specified by user - this is an error
                            throw new RuntimeException("Illegal access for label: no array types specified");
                        } else if (basetype != LXPBaseType.UNKNOWN && !reftype.equals(LXP_LIST.UNSPECIFIED_REF_TYPE)) { // both specified
                            // both base type and ref type specified by user - this is an error
                            throw new RuntimeException("Illegal access for label: reftype and basetype for array contents specified");
                        } else if (basetype == LXPBaseType.UNKNOWN) {                  // Just got one specified by use - either are OK.
                            type_rep.put(label_name, "[" + reftype + "]");              // use the ref type
                        } else {
                            type_rep.put(label_name, "[" + basetype.name() + "]");  // use the basetype
                        }
                    } catch (IllegalKeyException e) {
                        throw new RuntimeException("Illegal key in label: " + f.getName(), e);
                    }
                }
            }
        }
        return type_rep;
    }
}
