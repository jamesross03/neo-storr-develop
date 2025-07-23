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

import uk.ac.standrews.cs.neoStorr.impl.JPO;
import uk.ac.standrews.cs.neoStorr.impl.JPOMetaData;
import uk.ac.standrews.cs.neoStorr.impl.exceptions.PersistentObjectException;
import uk.ac.standrews.cs.neoStorr.interfaces.IBucket;
import uk.ac.standrews.cs.neoStorr.types.JPO_FIELD;

import java.util.Map;
import java.util.Objects;

public class JPOPerson extends JPO {

    @JPO_FIELD
    private int age;

    @JPO_FIELD
    public String address;

    public JPOPerson() { // requirement for JPO
    }

    public JPOPerson(long id, Map<String, Object> map, IBucket<?> bucket ) throws PersistentObjectException { // a requirement for JPO
        super(id, map, bucket);
    }

    public JPOPerson(int age, String address) {
        this.age = age;
        this.address = address;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final JPOPerson person = (JPOPerson) o;
        return age == person.age &&
                Objects.equals(address, person.address);
    }

    @Override
    public int hashCode() {
        return Objects.hash(age, address);
    }

    /* Storr stuff */

    private static final JPOMetaData static_metadata;

    @Override
    public JPOMetaData getJPOMetaData() {
        return static_metadata;
    }

    static {
        try {
            static_metadata = new JPOMetaData(JPOPerson.class,"JPOPerson");
        } catch (Exception var1) {
            throw new RuntimeException(var1);
        }
    }
}
