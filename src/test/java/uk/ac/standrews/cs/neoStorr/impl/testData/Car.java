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
package uk.ac.standrews.cs.neoStorr.impl.testData;

import uk.ac.standrews.cs.neoStorr.impl.LXPMetadata;
import uk.ac.standrews.cs.neoStorr.impl.StaticLXP;
import uk.ac.standrews.cs.neoStorr.types.LXPBaseType;
import uk.ac.standrews.cs.neoStorr.types.LXP_SCALAR;

public class Car extends StaticLXP {

    private static final LXPMetadata static_metadata;

    @LXP_SCALAR(type = LXPBaseType.STRING)
    public static int MODEL;

    @Override
    public LXPMetadata getMetaData() {
            return static_metadata;
    }

    static {
        try {
            static_metadata = new LXPMetadata(Car.class, Car.class.getSimpleName());
        } catch (Exception var1) {
            throw new RuntimeException(var1);
        }
    }
}