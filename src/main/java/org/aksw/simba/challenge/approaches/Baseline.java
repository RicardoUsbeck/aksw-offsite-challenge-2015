package org.aksw.simba.challenge.approaches;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.aksw.jena_sparql_api.core.QueryExecutionFactory;
import org.aksw.jena_sparql_api.model.QueryExecutionFactoryModel;
import org.aksw.simba.topicmodeling.commons.sort.AssociativeSort;
import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.carrotsearch.hppc.ObjectIntOpenHashMap;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Model;

public class Baseline implements Approach {

    private static final Logger LOGGER = LoggerFactory.getLogger(Baseline.class);

    @Override
    public List<String> generateResourceRanking(List<String> queries, Model knowledgeBase) {
        ObjectIntOpenHashMap<String> countedResources = sumUpResults(knowledgeBase, queries);
        String[] sortedResources = sortResources(countedResources);
        return Arrays.asList(sortedResources);
    }

    public ObjectIntOpenHashMap<String> sumUpResults(Model model, List<String> queries) {
        QueryExecutionFactoryModel factory = new QueryExecutionFactoryModel(model);
        ObjectIntOpenHashMap<String> countedResources = new ObjectIntOpenHashMap<String>();
        int count = 0;
        for (String query : queries) {
            addResultCounts(factory, query, countedResources);
            ++count;
            if ((count % 100) == 0) {
                LOGGER.info("saw " + count + " queries.");
            }
        }
        return countedResources;
    }

    public void addResultCounts(QueryExecutionFactory factory, String query,
            ObjectIntOpenHashMap<String> countedResources) {
        QueryExecution qe = factory.createQueryExecution(query);
        ResultSet rs = qe.execSelect();
        QuerySolution qs;
        Iterator<String> varNames;
        try {
            while (rs.hasNext()) {
                qs = rs.next();
                varNames = qs.varNames();
                while (varNames.hasNext()) {
                    countedResources.putOrAdd(varNames.next(), 1, 1);
                }
            }
        } catch (Exception e) {
            LOGGER.warn("Got exception while executing query: \"" + query + "\".", e);
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
