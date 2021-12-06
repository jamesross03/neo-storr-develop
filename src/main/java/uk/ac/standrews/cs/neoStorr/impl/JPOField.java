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

import java.lang.reflect.Type;

public class JPOField {

    public final String name;
    public final Type type;
    public final boolean is_list;
    public final boolean is_lxp_ref;
    public final boolean is_jpo_ref;

    public JPOField(final String name, final Type type, final boolean is_list, final boolean is_lxp_ref, final boolean is_jpo_ref) {

        this.name = name;
        this.type = type;
        this.is_list = is_list;
        this.is_lxp_ref = is_lxp_ref;
        this.is_jpo_ref = is_jpo_ref;
    }
}

