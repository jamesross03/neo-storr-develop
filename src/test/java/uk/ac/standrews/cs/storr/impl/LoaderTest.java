/*
 * Copyright 2017 Systems Research Group, University of St Andrews:
 * <https://github.com/stacs-srg>
 *
 * This file is part of the module storr.
 *
 * storr is free software: you can redistribute it and/or modify it under the terms of the GNU General Public
 * License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * storr is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with storr. If not, see
 * <http://www.gnu.org/licenses/>.
 */
package uk.ac.standrews.cs.storr.impl;

import org.junit.Before;
import org.junit.Test;
import uk.ac.standrews.cs.storr.impl.exceptions.BucketException;
import uk.ac.standrews.cs.storr.impl.exceptions.RepositoryException;
import uk.ac.standrews.cs.storr.impl.exceptions.StoreException;
import uk.ac.standrews.cs.storr.interfaces.IBucket;
import uk.ac.standrews.cs.storr.interfaces.IRepository;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;

/**
 * @author Simone I. Conte "sic2@st-andrews.ac.uk"
 */
public class LoaderTest extends CommonTest {

    @Before
    public void setUp() throws RepositoryException, IOException, StoreException, URISyntaxException, BucketException {

        super.setUp();
    }

    @Test
    public void checkLoader() throws RepositoryException, BucketException {

        IRepository types = store.getRepository("Types_repository");
        IBucket b = types.getBucket("Type_names");
        List<Long> an_oid = b.getOids();

        PersistentObject lxp = ((NeoBackedBucket) b).loader(an_oid.get(0));

        System.out.println( "Loaded lxp id: " + lxp.getId() );

        System.out.println( "finished");
    }
}
