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

import org.junit.After;
import org.junit.Before;
import uk.ac.standrews.cs.neoStorr.impl.exceptions.BucketException;
import uk.ac.standrews.cs.neoStorr.impl.exceptions.RepositoryException;
import uk.ac.standrews.cs.neoStorr.impl.exceptions.StoreException;
import uk.ac.standrews.cs.neoStorr.interfaces.IRepository;
import uk.ac.standrews.cs.neoStorr.interfaces.IStore;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;

public abstract class CommonTest {

    static final String REPOSITORY_NAME = "TEST_REPO";

    protected IStore store;
    protected IRepository repository;

    @Before
    public void setUp() throws RepositoryException, IOException, StoreException, URISyntaxException, BucketException {

        store = Store.getInstance();

        try {
            repository = store.getRepository(REPOSITORY_NAME);
        } catch( Exception e ) {
            repository = store.makeRepository(REPOSITORY_NAME);
        }
    }
}
