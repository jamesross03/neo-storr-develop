/*
 * Copyright 2017 Systems Research Group, University of St Andrews:
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
import org.neo4j.driver.types.Node;
import uk.ac.standrews.cs.neoStorr.impl.exceptions.RepositoryException;
import uk.ac.standrews.cs.neoStorr.impl.exceptions.StoreException;
import uk.ac.standrews.cs.neoStorr.impl.transaction.impl.TransactionManager;
import uk.ac.standrews.cs.neoStorr.impl.transaction.interfaces.ITransactionManager;
import uk.ac.standrews.cs.neoStorr.interfaces.IRepository;
import uk.ac.standrews.cs.neoStorr.interfaces.IStore;
import uk.ac.standrews.cs.neoStorr.util.NeoDbCypherBridge;

import java.util.*;

import static org.neo4j.driver.Values.parameters;
import static uk.ac.standrews.cs.neoStorr.impl.Repository.repositoryNameIsLegal;

/**
 * Created by al on 06/06/2014.
 */
public class Store implements IStore {

    private static Store instance;
    private ITransactionManager transaction_manager;
    private final TypeFactory type_factory;
    private final Map<String, IRepository> repository_cache;

    public NeoDbCypherBridge getBridge() {
        return bridge;
    }

    private final NeoDbCypherBridge bridge;

    private static final String CREATE_REPO_QUERY = "MERGE (a:STORR_REPOSITORY {name: $name})";
    private static final String REPO_EXISTS_QUERY = "MATCH (r:STORR_REPOSITORY {name: $name}) return r";
    private static final String DELETE_REPO_QUERY = "MATCH (r:STORR_REPOSITORY {name: $name})-[c:STORR_CONTAINS]-(b:STORR_BUCKET) DETACH DELETE r,b";

    // fully recursive: "MATCH (r:STORR_REPOSITORY {name: $name})-[c:STORR_CONTAINS]-(b:STORR_BUCKET)-[l:STORR_MEMBER]-(o) DETACH DELETE r,b,o";

    private static final String CREATE_ID_CONSTRAINT_QUERY = "CREATE CONSTRAINT ON (n:STORR_ID) ASSERT n.propertyName IS UNIQUE";
    private static final String STORR_INDEX_QUERY = "CALL db.createUniquePropertyConstraint(\"StorrIndex\", [\"STORR_LXP\"], [\"STORR_ID\"], \"native-btree-1.0\")";

    private List<String> init_indices_queries = Arrays.asList( CREATE_ID_CONSTRAINT_QUERY,STORR_INDEX_QUERY );
    private static final String SHOW_INDEXES_QUERY = "SHOW INDEXES";

    public Store() throws StoreException {

        instance = this;

        try {
            bridge = new NeoDbCypherBridge();
            repository_cache = new HashMap<>();

            transaction_manager = new TransactionManager(this);
            type_factory = new TypeFactory(this);
            initialiseIndices();

        } catch ( RepositoryException e) {
            throw new StoreException(e.getMessage());
        }
    }

    /**
     * Initialises the indices if they are not already set up.
     */
    private void initialiseIndices() {
        try (Session session = bridge.getNewSession(); ) {
            if( ! indicesInitialisedAlready(session) ) {
                for (String query : init_indices_queries) {
                    try {
                        session.run(query);
                    } catch (RuntimeException e) {
                        System.out.println("Exception in constraint: " + e.getMessage());
                    }
                }
            }
        }
    }

    public void close() throws Exception {
        bridge.close();
    }

    private boolean indicesInitialisedAlready(Session session) {
        Result r = session.run( SHOW_INDEXES_QUERY );
        while (r.hasNext()) {
            if( r.next().get("name").asString().equals( "StorrIndex" ) ) {
                return true;
            }
        }
        return false;
    }

    public synchronized static IStore getInstance() {
        return instance;
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
        if( repositoryExists(name)) {
            throw new RepositoryException("Repository with name <" + name + "> already exists");
        }
        createRepositoryInNeo(name);
        IRepository r = new Repository(this,name);
        repository_cache.put(name, r);
        return r;
    }

    @Override
    public boolean repositoryExists(String name) {
        if( repository_cache.containsKey(name) ) {
            return true;
        }
        return repositoryExistsInDB(name);
    }

    private boolean repositoryExistsInDB(String name) {
        try( Session s = bridge.getNewSession(); ) {
            Result result = s.run(REPO_EXISTS_QUERY, parameters("name", name));
            List<Node> nodes = result.list(r -> r.get("r").asNode());
            if (nodes.size() == 0) {
                return false;
            }
            return true;
        }
    }

    @Override
    public IRepository getRepository(String name) throws RepositoryException {

        if (repository_cache.containsKey(name)) {
            return repository_cache.get(name);
        } else {
            if (repositoryExistsInDB(name)) {
                IRepository r = new Repository(this, name);
                repository_cache.put(name, r);
                return r;
            }
        }
        throw new RepositoryException("repository does not exist: " + name);
    }

    @Override
    public void deleteRepository(String repository_name) throws RepositoryException {
        if (!repositoryExists(repository_name)) {
            throw new RepositoryException("Bucket with " + repository_name + "does not exist");
        }

        repository_cache.remove(repository_name);

        try( Session session = bridge.getNewSession(); ) {
            session.writeTransaction(tx -> tx.run(DELETE_REPO_QUERY,parameters("name", repository_name)));
        }

    }


    @Override
    public Iterator<IRepository> getIterator() {
        return null; }

//    private Path getRepoPath(final String name) {
//
//        return repository_path.resolve(name);
//    }

    ////////////////// private and protected methods //////////////////


    private void createRepositoryInNeo(String name) throws RepositoryException {

        if (repositoryExists(name)) {
            throw new RepositoryException("Repo: " + name + " already exists" );
        }

        try ( Session session = bridge.getNewSession() )
        {
            session.writeTransaction(tx -> tx.run(CREATE_REPO_QUERY, parameters("name", name)));
        }
    }
}
