package org.aksw.simba.challenge.approaches;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.aksw.jena_sparql_api.core.QueryExecutionFactory;
import org.aksw.simba.challenge.CountedResources;
import org.aksw.simba.challenge.Evaluation;
import org.aksw.simba.challenge.QueryExecutor;
import org.aksw.simba.topicmodeling.commons.sort.AssociativeSort;
import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.carrotsearch.hppc.ObjectIntOpenHashMap;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.RDFNode;

public class Baseline implements Approach {

    @SuppressWarnings("unused")
    private static final Logger LOGGER = LoggerFactory.getLogger(Baseline.class);

    protected QueryExecutor executer = null;

    @Deprecated
    public Baseline() {
    }

    public Baseline(QueryExecutor executer) {
        this.executer = executer;
    }

    @Override
    public List<String> generateResourceRanking(List<String> queries, Model knowledgeBase) {
        ObjectIntOpenHashMap<String> countedResources = sumUpResults(knowledgeBase, queries);
        String[] sortedResources = sortResources(countedResources);
        return Arrays.asList(sortedResources);
    }

    public ObjectIntOpenHashMap<String> sumUpResults(Model model, List<String> queries) {
        ObjectIntOpenHashMap<String> countedResources = Evaluation.generateMapWithAllResources(model);
        if (executer == null) {
            QueryExecutionFactory factory = Evaluation.createQueryExecutionFactory(model);
            for (String query : queries) {
                addResultCounts(factory, query, countedResources);
            }
            factory.close();
        } else {
            for (String query : queries) {
                addResultCounts(query, countedResources);
            }
        }
        return countedResources;
    }

    public void addResultCounts(String query, ObjectIntOpenHashMap<String> countedResources) {
        CountedResources result = executer.query(query);
        for (int i = 0; i < result.uri.length; ++i) {
            countedResources.putOrAdd(result.uri[i], result.count[i], result.count[i]);
        }
    }

    @Deprecated
    public void addResultCounts(QueryExecutionFactory factory, String query,
            ObjectIntOpenHashMap<String> countedResources) {
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
        }
    }

    public String[] sortResources(ObjectIntOpenHashMap<String> countedResources) {
        String uris[] = new String[countedResources.assigned];
        int counts[] = new int[countedResources.assigned];
        int pos = 0;
        for (int i = 0; i < countedResources.allocated.length; ++i) {
            if (countedResources.allocated[i]) {
                uris[pos] = (String) ((Object[]) countedResources.keys)[i];
                counts[pos] = countedResources.values[i];
                ++pos;
            }
        }
        AssociativeSort.quickSort(counts, uris);
        ArrayUtils.reverse(uris);
        return uris;
    }
}
