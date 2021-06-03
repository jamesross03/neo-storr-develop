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

import uk.ac.standrews.cs.neoStorr.impl.exceptions.PersistentObjectException;
import uk.ac.standrews.cs.neoStorr.interfaces.IBucket;
import uk.ac.standrews.cs.neoStorr.types.LXP_REF;

import java.util.Map;

public class AClassContainingBBBRef extends StaticLXP {

    private static LXPMetadata static_metadata;

    @LXP_REF(type = "BBB")
    public static int MY_FIELD;

    public AClassContainingBBBRef() {}

    public AClassContainingBBBRef(BBB b) throws PersistentObjectException {
        this.put(AClassContainingBBBRef.MY_FIELD, b.getThisRef() );
    }

    public AClassContainingBBBRef(long persistent_object_id, Map properties, IBucket bucket ) throws PersistentObjectException {
        super( persistent_object_id, properties, bucket );
    }

    @Override
    public LXPMetadata getMetaData() {
        return static_metadata;
    }

    static {
        try {
            static_metadata = new LXPMetadata(AClassContainingBBBRef.class, "AClassContainingBBBRef");
        } catch (Exception var1) {
            throw new RuntimeException(var1);
        }
    }

}