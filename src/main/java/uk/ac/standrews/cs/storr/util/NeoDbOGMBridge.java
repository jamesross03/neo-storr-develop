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
package uk.ac.standrews.cs.storr.util;

import org.neo4j.ogm.config.Configuration;
import org.neo4j.ogm.session.Session;
import org.neo4j.ogm.session.SessionFactory;

public class NeoDbOGMBridge extends uk.ac.standrews.cs.storr.util.NeoDbBridge implements AutoCloseable {

    private SessionFactory sessionFactory;
    private Configuration conf;

    public NeoDbOGMBridge() {
        this(default_url, default_user, default_password);
    }

    public NeoDbOGMBridge(String uri, String user, String password) {
        super( uri,user,password );
        conf = new Configuration.Builder()
                .uri(uri)
                .credentials(user, password)
                .build();
        sessionFactory = new SessionFactory(conf, "uk.ac.standrews.cs.population_linkage.graph.model");
    }

    @Override
    public void close() throws Exception
    {
        sessionFactory.close();
    }

    public Session getNewSession() {
        return sessionFactory.openSession();
    }
}
