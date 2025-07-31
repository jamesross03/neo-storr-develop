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
package uk.ac.standrews.cs.neoStorr.util;

public abstract class NeoDbBridge implements AutoCloseable {

    public static final int DEFAULT_BOLT_PORT = 7687;

    public static final String DEFAULT_URL = "bolt://localhost:" + DEFAULT_BOLT_PORT;
    public static final String DEFAULT_USER = "neo4j";
    public static final String DEFAULT_PASSWORD = "password";

    protected final String url;
    protected final String user;
    protected final String password;

    public NeoDbBridge() {
        this(DEFAULT_URL, DEFAULT_USER, DEFAULT_PASSWORD);
    }

    public NeoDbBridge(final String url, final String user, final String password) {
        this.url = url;
        this.user = user;
        this.password = password;
    }

    public abstract void close() throws Exception;
}
