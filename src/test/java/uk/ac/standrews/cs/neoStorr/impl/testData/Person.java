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

import uk.ac.standrews.cs.neoStorr.impl.LXPMetaData;
import uk.ac.standrews.cs.neoStorr.impl.StaticLXP;
import uk.ac.standrews.cs.neoStorr.impl.exceptions.PersistentObjectException;
import uk.ac.standrews.cs.neoStorr.interfaces.IBucket;
import uk.ac.standrews.cs.neoStorr.types.LXPBaseType;
import uk.ac.standrews.cs.neoStorr.types.LXP_SCALAR;

import java.util.Map;

public class Person extends StaticLXP {

    private static final LXPMetaData static_metadata;

    @LXP_SCALAR(type = LXPBaseType.STRING)
    public static int FORENAME;
    @LXP_SCALAR(type = LXPBaseType.STRING)
    public static int SURNAME;

    public Person() {
    }

    public Person(long persistent_object_id, Map properties, IBucket bucket) throws PersistentObjectException {
        super(persistent_object_id, properties, bucket);
    }

    public Person(String forename, String surname) {
        this.put(Person.FORENAME, forename);
        this.put(Person.SURNAME, surname);
    }

    @Override
    public LXPMetaData getMetaData() {
        return static_metadata;
    }

    static {
        static_metadata = new LXPMetaData(Person.class, Person.class.getSimpleName());
    }
}