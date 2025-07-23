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

import uk.ac.standrews.cs.neoStorr.impl.*;
import uk.ac.standrews.cs.neoStorr.interfaces.IReferenceType;
import uk.ac.standrews.cs.neoStorr.interfaces.IStore;
import uk.ac.standrews.cs.neoStorr.interfaces.IType;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
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
    public static <T extends LXP> boolean checkLabelConsistency(final T record, final long type_label_id, final IStore store) {

        final IReferenceType type = record.getMetaData().getType();
        if (type == null) return true;

        try {
            return checkLabelsConsistentWith(type.getId(), type_label_id, store);

        } catch (RuntimeException e) {
            return false; // label not there or inappropriate type
        }
    }

    /**
     * Checks that the content of a record is consistent with a supplied label (generally from a bucket)
     *
     * @param record        whose _structure is to be checked
     * @param type_label_id the label against which the checking is to be performed
     * @param <T>           the type of the record being checked
     * @return true if the structure is consistent
     */
    public static <T extends LXP> boolean checkStructuralConsistency(final T record, final long type_label_id, final IStore store) {

        final IReferenceType bucket_type = store.getTypeFactory().typeWithId(type_label_id);
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
    static <T extends LXP> boolean checkStructuralConsistency(final T record, final IReferenceType ref_type) {

        final Set<String> record_keys = record.getMetaData().getFields();
        final Set<String> required_labels = ref_type.getLabels();

        for (final String label : required_labels) {
            if (!record_keys.contains(label)) return false; // required label not present

            try {
                if (!ref_type.getFieldType(label).valueConsistentWithType(record.get(label)))
                    return false; // label does not match expected type

            } catch (RuntimeException e) {
                return false; // type mismatch
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
    private static boolean checkLabelsConsistentWith(final long supplied_label_id, final long type_label_id, final IStore store) {

        if (type_label_id == supplied_label_id) return true; // do id check first

        try {
            // do structural check over type reps
            final TypeFactory type_factory = store.getTypeFactory();

            // TODO should one of these use type_label_id?
            final IReferenceType stored_label = type_factory.typeWithId(supplied_label_id);
            final IReferenceType required_label = type_factory.typeWithId(supplied_label_id);

            for (String label : required_label.getLabels()) {
                if (required_label.getFieldType(label) != stored_label.getFieldType(label)) return false;
            }
            return true;

        } catch (RuntimeException e) {
            return false;
        }
    }

    static IType stringToType(final String value) {

        final TypeFactory type_factory = Store.getInstance().getTypeFactory();

        if (LXPBaseType.STRING.name().equalsIgnoreCase(value)) return LXPBaseType.STRING;
        if (LXPBaseType.LONG.name().equalsIgnoreCase(value)) return LXPBaseType.LONG;
        if (LXPBaseType.INT.name().equalsIgnoreCase(value)) return LXPBaseType.INT;
        if (LXPBaseType.DOUBLE.name().equalsIgnoreCase(value)) return LXPBaseType.DOUBLE;
        if (LXPBaseType.BOOLEAN.name().equalsIgnoreCase(value)) return LXPBaseType.BOOLEAN;

        if (value.startsWith("STOREREF[") && value.endsWith("]")) { // it is a ref type

            final String type_referenced_rep = value.substring("STOREREF[".length(), value.length() - 1); // the name of the type that the ref is to
            if (type_factory.containsKey(type_referenced_rep)) {
                IReferenceType type_referenced = type_factory.getTypeWithName(type_referenced_rep);
                return new LXPReferenceType((DynamicLXP) type_referenced.getRep());
            }

            throw new RuntimeException("Encountered reference to unknown type: " + type_referenced_rep);
        }

        if (value.startsWith("[") && value.endsWith("]")) { // it is a list type

            final String list_contents = value.substring(1, value.length() - 1);
            if (LXPBaseType.STRING.name().equalsIgnoreCase(list_contents) ||
                    LXPBaseType.LONG.name().equalsIgnoreCase(list_contents) ||
                    LXPBaseType.INT.name().equalsIgnoreCase(list_contents) ||
                    LXPBaseType.DOUBLE.name().equalsIgnoreCase(list_contents) ||
                    LXPBaseType.BOOLEAN.name().equalsIgnoreCase(list_contents)) {
                return LXPListBaseType.valueOf(list_contents);
            }

            // it may be a list of ref types
            if (type_factory.containsKey(list_contents)) {
                return new LXPListRefType(type_factory.getTypeWithName(list_contents));
            }

            throw new RuntimeException("Encountered unknown array contents: " + list_contents);
        }

        if (type_factory.containsKey(value)) {
            return type_factory.getTypeWithName(value);
        }

        throw new RuntimeException("Encountered reference to type not defined: " + value);
    }

    public static DynamicLXP getTypeRep(final Class c) {

        if (StaticLXP.class.isAssignableFrom(c) || DynamicLXP.class.isAssignableFrom(c)) {
            return getLXPTypeRep(c);
        }

        throw new RuntimeException("Do not recognise persistent class: " + c.getName());
    }

    public static DynamicLXP getLXPTypeRep(final Class c) {

        final DynamicLXP type_rep = new DynamicLXP();

        for (final Field f : c.getDeclaredFields()) {

            if (Modifier.isStatic(f.getModifiers())) {

                if (f.isAnnotationPresent(LXP_SCALAR.class)) {
                    if (f.isAnnotationPresent(LXP_REF.class) || f.isAnnotationPresent(LXP_LIST.class))
                        throw new RuntimeException("Conflicting labels: " + f.getName());

                    final LXP_SCALAR scalar_type = f.getAnnotation(LXP_SCALAR.class);

                    f.setAccessible(true);
                    type_rep.put(f.getName(), scalar_type.type().name());

                } else if (f.isAnnotationPresent(LXP_REF.class)) {

                    if (f.isAnnotationPresent(LXP_LIST.class))
                        throw new RuntimeException("Conflicting labels: " + f.getName());

                    final LXP_REF ref_type = f.getAnnotation(LXP_REF.class);

                    f.setAccessible(true);
                    type_rep.put(f.getName(), "STOREREF[" + ref_type.type() + "]");

                } else if (f.isAnnotationPresent(LXP_LIST.class)) {

                    final LXP_LIST list_type = f.getAnnotation(LXP_LIST.class);

                    f.setAccessible(true);
                    final String label_name = f.getName();
                    final LXPBaseType base_type = list_type.basetype();
                    final String ref_type = list_type.reftype();

                    if (base_type == LXPBaseType.UNKNOWN && ref_type.equals(LXP_LIST.UNSPECIFIED_REF_TYPE))
                        // no type specified by user - this is an error
                        throw new RuntimeException("Illegal access for label: no array types specified");

                    if (base_type != LXPBaseType.UNKNOWN && !ref_type.equals(LXP_LIST.UNSPECIFIED_REF_TYPE))
                        // both base type and ref type specified by user - this is an error
                        throw new RuntimeException("Illegal access for label: reftype and basetype for array contents specified");

                    if (base_type == LXPBaseType.UNKNOWN) {                  // Just got one specified by use - either are OK.
                        type_rep.put(label_name, "[" + ref_type + "]");              // use the ref type
                    } else {
                        type_rep.put(label_name, "[" + base_type.name() + "]");  // use the basetype
                    }
                }
            }
        }
        return type_rep;
    }
}
