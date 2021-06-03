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


import org.neo4j.driver.Session;
import org.neo4j.driver.Transaction;
import uk.ac.standrews.cs.neoStorr.util.NeoDbCypherBridge;

public class EraseDb  {

    private static String q1 = "MATCH (r:STORR_REPOSITORY) DETACH DELETE r";
    private static String q2 = "MATCH (b:STORR_BUCKET) DETACH DELETE b";
    private static String q3 = "MATCH (l:STORR_LXP) DETACH DELETE l";

    public static void main(String[] args) {

        try( NeoDbCypherBridge bridge = new NeoDbCypherBridge(); ) {

            try (Session session = bridge.getNewSession(); Transaction tx = session.beginTransaction();) {

                tx.run(q1);
                tx.run(q2);
                tx.run(q3);
                tx.commit();
            } finally {
                System.out.println("Storr constructs in Neo4J deleted");
            }
        } catch (Exception e) {
            System.out.println( "Exception in bridge: " + e.getMessage() );
        } finally {
            System.out.println("Execution complete");
        }

    }




}
