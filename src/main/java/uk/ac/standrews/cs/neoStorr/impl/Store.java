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

import org.neo4j.driver.Result;
import org.neo4j.driver.Session;
import org.neo4j.driver.Transaction;
import org.neo4j.driver.types.Node;
import uk.ac.standrews.cs.neoStorr.impl.exceptions.RepositoryException;
import uk.ac.standrews.cs.neoStorr.impl.transaction.impl.TransactionManager;
import uk.ac.standrews.cs.neoStorr.impl.transaction.interfaces.ITransactionManager;
import uk.ac.standrews.cs.neoStorr.interfaces.IRepository;
import uk.ac.standrews.cs.neoStorr.interfaces.IStore;
import uk.ac.standrews.cs.neoStorr.util.NeoDbCypherBridge;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.neo4j.driver.Values.parameters;
import static uk.ac.standrews.cs.neoStorr.impl.Repository.repositoryNameIsLegal;

/**
 * Created by al on 06/06/2014.
 */
public class Store implements IStore {

    private static Store instance = null;

    private final ITransactionManager transaction_manager;
    private final TypeFactory type_factory;
    private final Map<String, IRepository> repository_cache;

    private NeoDbCypherBridge bridge = null;

    private static final String CREATE_REPO_QUERY = "MERGE (a:STORR_REPOSITORY {name: $name})";
    private static final String REPO_EXISTS_QUERY = "MATCH (r:STORR_REPOSITORY {name: $name}) return r";
    private static final String DELETE_EMPTY_REPO_QUERY = "MATCH (r:STORR_REPOSITORY {name: $name}) DETACH DELETE r";
    private static final String DELETE_REPO_CONTENTS_QUERY = "MATCH (r:STORR_REPOSITORY {name: $name})-[c:STORR_CONTAINS]-(b:STORR_BUCKET) DETACH DELETE r,b";

    // 2 level: "MATCH (r:STORR_REPOSITORY {name: $name})-[c:STORR_CONTAINS]-(b:STORR_BUCKET) DETACH DELETE r,b"
    // 3 level: "MATCH (r:STORR_REPOSITORY {name: $name})-[c:STORR_CONTAINS]-(b:STORR_BUCKET)-[l:STORR_MEMBER]-(o) DETACH DELETE r,b,o";
    // fully recursive??

    private static final String STORR_INDEX_NAME = "StorrIndex";
    private static final String CREATE_ID_CONSTRAINT_QUERY =
        "CREATE CONSTRAINT storr_id_unique FOR (n:STORR_ID) REQUIRE n.propertyName IS UNIQUE";
    private static final String STORR_INDEX_QUERY = 
        String.format("CREATE CONSTRAINT %s FOR (n:%s) REQUIRE n.%s IS UNIQUE", STORR_INDEX_NAME, "STORR_LXP", "STORR_ID");

    private static final List<String> INIT_INDICES_QUERIES = Arrays.asList(CREATE_ID_CONSTRAINT_QUERY, STORR_INDEX_QUERY);
    private static final String SHOW_INDICES_QUERY = "SHOW INDEXES";

    private Store() {

        try {
            bridge = new NeoDbCypherBridge();
            repository_cache = new HashMap<>();

            transaction_manager = new TransactionManager(this);
            type_factory = new TypeFactory(this);
            initialiseIndices();

        } catch (final Exception e) {
            bridge.close();
            throw new RuntimeException(e);
        }
    }

    public synchronized static IStore getInstance() {

        if (instance == null) instance = new Store();
        return instance;
    }

    public NeoDbCypherBridge getBridge() {
        return bridge;
    }

    /**
     * Initialises the indices if they are not already set up.
     */
    private void initialiseIndices() {

        try (final Session session = bridge.getNewSession()) {

            if (!indicesInitialisedAlready(session)) {
                for (String query : INIT_INDICES_QUERIES) {
                    session.run(query);
                }
            }
        }
    }

    public void close() {
        bridge.close();
    }

    private boolean indicesInitialisedAlready(final Session session) {

        Result r = session.run(SHOW_INDICES_QUERY);
        while (r.hasNext()) {
            if (r.next().get("name").asString().equals(STORR_INDEX_NAME)) return true;
        }
        return false;
    }

    @Override
    public ITransactionManager getTransactionManager() {
        return transaction_manager;
    }

    @Override
    public TypeFactory getTypeFactory() {
        return type_factory;
    }

    @Override
    public IRepository makeRepository(final String name) throws RepositoryException {

        if (!repositoryNameIsLegal(name)) {
            throw new RepositoryException("Illegal Repository name <" + name + ">");
        }

        if (repositoryExists(name)) {
            throw new RepositoryException("Repository with name <" + name + "> already exists");
        }

        createRepositoryInNeo(name);
        IRepository r = new Repository(this, name);
        repository_cache.put(name, r);

        return r;
    }

    @Override
    public boolean repositoryExists(final String name) {
        return repository_cache.containsKey(name) || repositoryExistsInDB(name);
    }

    private boolean repositoryExistsInDB(final String name) {

        try (final Session s = bridge.getNewSession()) {

            Result result = s.run(REPO_EXISTS_QUERY, parameters("name", name));
            List<Node> nodes = result.list(r -> r.get("r").asNode());
            return !nodes.isEmpty();
        }
    }

    @Override
    public IRepository getRepository(final String name) throws RepositoryException {

        if (repository_cache.containsKey(name)) {
            return repository_cache.get(name);
        }

        if (repositoryExistsInDB(name)) {
            final IRepository r = new Repository(this, name);
            repository_cache.put(name, r);
            return r;
        }

        throw new RepositoryException("repository does not exist: " + name);
    }

    @Override
    public void deleteRepository(final String repository_name) throws RepositoryException {

        if (!repositoryExists(repository_name))
            throw new RepositoryException("Bucket " + repository_name + " does not exist");

        repository_cache.remove(repository_name);

        try (final Session session = bridge.getNewSession(); final Transaction tx = session.beginTransaction()) {

            tx.run(DELETE_REPO_CONTENTS_QUERY, parameters("name", repository_name));
            tx.run(DELETE_EMPTY_REPO_QUERY, parameters("name", repository_name));
            tx.commit();
        }
    }

    private void createRepositoryInNeo(final String name) throws RepositoryException {

        if (repositoryExists(name)) throw new RepositoryException("Repo: " + name + " already exists");

        try (final Session session = bridge.getNewSession()) {
            session.run(CREATE_REPO_QUERY, parameters("name", name));
        }
    }
}
