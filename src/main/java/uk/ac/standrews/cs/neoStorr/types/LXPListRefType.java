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

import uk.ac.standrews.cs.neoStorr.impl.LXP;
import uk.ac.standrews.cs.neoStorr.interfaces.IReferenceType;
import uk.ac.standrews.cs.neoStorr.interfaces.IType;

import java.util.List;

/**
 * Created by al on 22/1//2016
 * A class representing types that may be encoded above OID storage layer (optional)
 * Represents lists of reference types
 */
public class LXPListRefType implements IType {

    private final IReferenceType contents_type;

    LXPListRefType(final IReferenceType contents_type) {
        this.contents_type = contents_type;
    }

    public boolean valueConsistentWithType(final Object value) {

        if (value == null) return true; // permit all null values

        if (value instanceof List) {

            final List<?> list = (List<?>) value;
            if (list.isEmpty()) return true; // cannot check contents due to type erasure - and is empty so OK.

            // Need to check the contents of the list are type compatible with expected type.
            for (final Object o : list) {
                final LXP record = (LXP) o;
                if (!Types.checkStructuralConsistency(record, contents_type)) return false;
            }
            // everything checked out
            return true;
        }

        return false;
    }
}
