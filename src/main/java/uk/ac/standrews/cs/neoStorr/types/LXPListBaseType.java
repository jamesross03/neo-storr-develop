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

import uk.ac.standrews.cs.neoStorr.interfaces.IType;

import java.util.List;

/**
 * Created by al on 22/1//2016
 * A class representing types that may be encoded above OID storage layer (optional)
 * Represents lists of base types
 */
public enum LXPListBaseType implements IType {

    UNKNOWN {
        @Override
        public boolean valueConsistentWithType(final Object value) {
            throw new RuntimeException("Encountered ARRAY OF UNKNOWN type whilst checking field contents");
        }
    },

    STRING {
        @Override
        public boolean valueConsistentWithType(final Object value) {
            return check(value, new String[1]);
        }
    },

    INT {
        @Override
        public boolean valueConsistentWithType(final Object value) {
            return check(value, new Integer[1]);
        }
    },

    LONG {
        @Override
        public boolean valueConsistentWithType(final Object value) {
            return check(value, new Long[1]);
        }
    },

    DOUBLE {
        @Override
        public boolean valueConsistentWithType(final Object value) {
            return check(value, new Double[1]);
        }
    },

    BOOLEAN {
        @Override
        public boolean valueConsistentWithType(final Object value) {
            return check(value, new Boolean[1]);
        }
    },

    REF {
        @Override
        public boolean valueConsistentWithType(final Object value) {
            return check(value, new Long[1]);
         }
    };

    private static boolean check(final Object value, final Object[] test_array) {

        if (value instanceof List) {
            final List<?> list = (List<?>) value;
            if (list.isEmpty()) return true;

            try {
                list.toArray(test_array);
                return true;
            } catch (ArrayStoreException e) {
                return false;
            }
        }
        return false;
    }
}


