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

import org.apache.commons.lang3.reflect.FieldUtils;
import uk.ac.standrews.cs.neoStorr.impl.exceptions.PersistentObjectException;
import uk.ac.standrews.cs.neoStorr.interfaces.IBucket;
import uk.ac.standrews.cs.neoStorr.interfaces.IStoreReference;

import java.util.*;

/**
 * Java Persistent Object Class
 *
 * @author al
 */
public abstract class JPO extends StaticLXP {

    public JPO() {
        super();
    }

    public JPO(final long object_id, final IBucket bucket) {
        this.$$$$id$$$$id$$$$ = object_id;
        this.$$$bucket$$$bucket$$$ = bucket;
    }

    public JPO(final long object_id, final Map properties, final IBucket bucket) throws PersistentObjectException {
        super(object_id, properties, bucket);
    }

    public abstract JPOMetaData getJPOMetaData();

    @Override
    public LXPMetaData getMetaData() {
        return getJPOMetaData();
    }

    public IStoreReference getThisRef() throws PersistentObjectException {
        return super.getThisRef();
    }

    public void put(final String key, Object value) {

        final JPOMetaData md = getJPOMetaData();
        final JPOField field = md.get(key);

        if (field == null) throw new RuntimeException("key not found: " + key);

        if (field.is_list) {
            if (value.equals("null")) value = null;

        } else if (field.is_lxp_ref) {
            // check it is a string
            if (!(value instanceof String))
                throw new RuntimeException("Encountered store reference type not String encoded");

            String str_val = (String) value;
            if (str_val.equals("null")) {
                value = null;
            } else {
                value = new LXPReference((String) value);
            }
        }

        try {
            FieldUtils.writeField(this, key, value, true);

        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }
}
