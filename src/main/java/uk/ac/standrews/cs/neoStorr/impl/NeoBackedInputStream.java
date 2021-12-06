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
import uk.ac.standrews.cs.neoStorr.interfaces.IBucket;
import uk.ac.standrews.cs.neoStorr.interfaces.IInputStream;

import java.io.IOException;
import java.util.Iterator;
import java.util.NoSuchElementException;

public class NeoBackedInputStream<T extends PersistentObject> implements IInputStream<T> {

    private final IBucket<T> bucket;

    NeoBackedInputStream(final IBucket<T> bucket) throws IOException {
        this.bucket = bucket;
    }

    public Iterator<T> iterator() {

        return new Iterator<T>() {

            private final Iterator<Long> oid_iterator = bucket.getObjectIds().iterator();

            @Override
            public boolean hasNext() {
                return oid_iterator.hasNext();
            }

            @Override
            public T next() {

                try {
                    return bucket.getObjectById(oid_iterator.next());

                } catch (BucketException e) {
                    throw new NoSuchElementException(e.getMessage());
                }
            }
        };
    }
}
