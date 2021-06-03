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

import uk.ac.standrews.cs.neoStorr.impl.exceptions.BucketException;
import uk.ac.standrews.cs.neoStorr.impl.exceptions.PersistentObjectException;
import uk.ac.standrews.cs.neoStorr.impl.exceptions.RepositoryException;
import uk.ac.standrews.cs.neoStorr.interfaces.IBucket;
import uk.ac.standrews.cs.neoStorr.types.LXPBaseType;
import uk.ac.standrews.cs.neoStorr.types.LXP_SCALAR;

import java.util.Map;

public class BBB extends StaticLXP {

    private static LXPMetadata static_metadata;

    @LXP_SCALAR(type = LXPBaseType.STRING)
    public static int FORENAME;
    @LXP_SCALAR(type = LXPBaseType.STRING)
    public static int SURNAME;

    public BBB() {}

    public BBB(long persistent_object_id, Map properties, IBucket bucket ) throws PersistentObjectException {
        super( persistent_object_id, properties, bucket );
    }

    public BBB(String forename, String surname ) {
        this.put( BBB.FORENAME, forename );
        this.put( BBB.SURNAME, surname );
    }

    public static LXPReference<BBB> makeRef(String serialized ) {
        return new LXPReference<>(serialized);
    }

    public static BBB getRef(LXPReference<BBB> ref ) throws BucketException, RepositoryException {
        return (BBB) ref.getReferend(BBB.class);
    }

    @Override
    public LXPMetadata getMetaData() {
        return static_metadata;
    }

    static {
        try {
            static_metadata = new LXPMetadata(BBB.class, "BBB");
        } catch (Exception var1) {
            throw new RuntimeException(var1);
        }
    }
}