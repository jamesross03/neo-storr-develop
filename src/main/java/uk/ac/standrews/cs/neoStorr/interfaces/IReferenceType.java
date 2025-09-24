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
package uk.ac.standrews.cs.neoStorr.interfaces;

import uk.ac.standrews.cs.neoStorr.impl.LXP;

import java.util.Set;


/**
 * Created by al on 20/06/2014.
 */
public interface IReferenceType extends IType {

    /**
     * @return the labels present in the type.
     * For example for a type [name: string, age: int] would return {name,age}
     */
    Set<String> getLabels();

    /**
     * @param label - the label whose type is being looked up
     * @return the field type associated with the specified label
     * e.g. for a type [name: string, age: int] and the label "name" this method would return the
     * rep for @class LXPBaseType(INT).
     */
    IType getFieldType(String label);

    /**
     * @return the id of this typerep - this is the id of the underlying rep implementation.
     */
    long getId();

    /**
     * @return the OID used to encode the reference type - e.g. [name: string, age: int]
     */
    LXP getRep();
}
