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


import org.junit.Before;
import org.junit.Test;
import uk.ac.standrews.cs.neoStorr.impl.exceptions.BucketException;
import uk.ac.standrews.cs.neoStorr.impl.exceptions.IllegalKeyException;
import uk.ac.standrews.cs.neoStorr.impl.exceptions.RepositoryException;
import uk.ac.standrews.cs.neoStorr.interfaces.IBucket;
import uk.ac.standrews.cs.neoStorr.interfaces.IInputStream;
import uk.ac.standrews.cs.neoStorr.interfaces.IRepository;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Random;

import static org.junit.Assert.*;

public class TypesTest extends CommonTest {

    private static String generic_bucket_name1 = Double.toString( new Random().nextDouble() );
    private IBucket<BBB> b;

    @Before
    public void setUp() throws RepositoryException, IOException, URISyntaxException, BucketException {

        super.setUp();

        try {
            repository.deleteBucket(generic_bucket_name1);
        } catch ( Exception e ) {
            System.out.println( "Bucket: " + generic_bucket_name1 + " did not exist before test - that is ok.");
        }

        b = repository.makeBucket(generic_bucket_name1, BBB.class);
    }


    @Test
    public synchronized void testGetType() throws RepositoryException, IllegalKeyException, BucketException {

        Class c = b.getBucketType();

        assertEquals( c, BBB.class );
    }

    @Test
    public synchronized void testTypenames() throws RepositoryException, IllegalKeyException, BucketException {

        IRepository types_repo = store.getRepository("Types_repository");
        IBucket  type_names = types_repo.getBucket( "Type_names" );

        IInputStream<LXP> name_stream = type_names.getInputStream();

        boolean found = false;

        for( LXP type_name : name_stream ) {
            // everything in type names should be LXPs containing an id and name field
            String name_field = (String) type_name.get( "name" );
            if( name_field.equals( "BBB" ) ) {
                if( found ) {
                    fail( "Found the type name BBB twice (or more...)");
                }
                found = true;
            }
        }
        assertTrue(found);
    }

    @Test
    public synchronized void testTypenamesANDReps() throws RepositoryException, IllegalKeyException, BucketException {

        IRepository types_repo = store.getRepository("Types_repository");
        IBucket  type_names = types_repo.getBucket( "Type_names" );
        IBucket  type_reps = types_repo.getBucket( "Type_reps" );

        IInputStream<LXP> name_stream = type_names.getInputStream();

        for( LXP type_name : name_stream ) {
            // everything in type names shpould be LXPs containing an id and name field
            String name_field = (String) type_name.get( "name" );
            if( name_field.equals( "BBB" ) ) {
                long rep_id = (Long) type_name.get( "key" );
                assertTrue( type_reps.contains(rep_id) );
                LXP type_rep = (LXP) type_reps.getObjectById(rep_id);
                assertTrue( type_rep.get("FORENAME").equals("STRING") );
                assertTrue( type_rep.get("SURNAME").equals("STRING") );
            }
        }
    }
}
