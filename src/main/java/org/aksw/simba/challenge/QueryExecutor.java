package org.aksw.simba.challenge;

import java.io.Closeable;
import java.io.IOException;
import java.util.Iterator;
import java.util.concurrent.ExecutionException;

import org.aksw.jena_sparql_api.core.QueryExecutionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.carrotsearch.hppc.ObjectIntOpenHashMap;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.RDFNode;

public class QueryExecutor extends CacheLoader<String, CountedResources>implements Closeable {

    private static final Logger LOGGER = LoggerFactory.getLogger(QueryExecutor.class);

    private LoadingCache<String, CountedResources> cache;
    private QueryExecutionFactory factory;

    public QueryExecutor(Model model) {
        cache = CacheBuilder.newBuilder().build(this);
        factory = Evaluation.createQueryExecutionFactory(model);
    }

    public CountedResources query(String query) {
        try {
            return cache.get(query);
        } catch (ExecutionException e) {
            LOGGER.error("Got an exception from the cache. Returning empty result.");
            return new CountedResources(new String[0], new int[0]);
        }
    }

    @Override
    public CountedResources load(String query) {
        ObjectIntOpenHashMap<String> countedResources = new ObjectIntOpenHashMap<String>();
        QueryExecution qe = factory.createQueryExecution(query);
        ResultSet rs = qe.execSelect();
        QuerySolution qs;
        Iterator<String> varNames;
        RDFNode node;
        try {
            while (rs.hasNext()) {
                qs = rs.next();
                varNames = qs.varNames();
                while (varNames.hasNext()) {
                    node = qs.get(varNames.next());
                    if (node.isResource() && !node.isAnon()) {
                        countedResources.putOrAdd(node.asResource().getURI(), 1, 1);
                    }
                }
            }
        } catch (Exception e) {
            // LOGGER.warn("Got exception while executing query: \"" + query +
            // "\".", e);
            return new CountedResources(new String[0], new int[0]);
        }
        CountedResources result = new CountedResources(new String[countedResources.assigned],
                new int[countedResources.assigned]);
        int pos = 0;
        for (int i = 0; i < countedResources.allocated.length; ++i) {
            if (countedResources.allocated[i]) {
                result.uri[pos] = (String) ((Object[]) countedResources.keys)[i];
                result.count[pos] = countedResources.values[i];
            }
        }

        return result;
    }

    @Override
    public void close() throws IOException {
        factory.close();
    }

}
