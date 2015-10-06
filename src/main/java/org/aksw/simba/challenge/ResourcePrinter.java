package org.aksw.simba.challenge;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import org.aksw.jena_sparql_api.model.QueryExecutionFactoryModel;
import org.aksw.simba.topicmodeling.commons.sort.AssociativeSort;
import org.apache.commons.io.IOUtils;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;

import com.carrotsearch.hppc.ObjectIntOpenHashMap;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;

public class ResourcePrinter {

    public static void main(String[] args) throws IOException {
        List<String> queries = QueryLoader.loadQueries("trained_queries.txt");
        Model model = readModel("swdf.nt");
        ObjectIntOpenHashMap<String> countedResources = sumUpResults(model, queries);
        String[] sortedResources = sortResources(countedResources);
        for (int i = 0; i < sortedResources.length; ++i) {
            System.out.println(sortedResources[i]);
        }
    }

    public static Model readModel(String modelFile) throws FileNotFoundException {
        FileInputStream fin = null;
        try {
            Model model = ModelFactory.createDefaultModel();
            fin = new FileInputStream(modelFile);
            RDFDataMgr.read(model, fin, Lang.NT);
            return model;
        } finally {
            IOUtils.closeQuietly(fin);
        }
    }

    public static ObjectIntOpenHashMap<String> sumUpResults(Model model, List<String> queries) {
        QueryExecutionFactoryModel factory = new QueryExecutionFactoryModel(model);
        ObjectIntOpenHashMap<String> countedResources = new ObjectIntOpenHashMap<String>();
        for (String query : queries) {
            addResultCounts(factory, query, countedResources);
        }
        return countedResources;
    }

    public static void addResultCounts(QueryExecutionFactoryModel factory, String query,
            ObjectIntOpenHashMap<String> countedResources) {
        QueryExecution qe = factory.createQueryExecution(query);
        ResultSet rs = qe.execSelect();
        QuerySolution qs;
        Iterator<String> varNames;
        while (rs.hasNext()) {
            qs = rs.next();
            varNames = qs.varNames();
            while (varNames.hasNext()) {
                countedResources.putOrAdd(varNames.next(), 1, 1);
            }
        }
    }

    public static String[] sortResources(ObjectIntOpenHashMap<String> countedResources) {
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
        return uris;
    }
}
