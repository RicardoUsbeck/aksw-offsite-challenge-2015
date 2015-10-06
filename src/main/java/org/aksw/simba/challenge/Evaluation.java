package org.aksw.simba.challenge;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.aksw.jena_sparql_api.model.QueryExecutionFactoryModel;
import org.aksw.simba.challenge.approaches.Approach;
import org.aksw.simba.challenge.approaches.Baseline;
import org.aksw.simba.topicmodeling.commons.sort.AssociativeSort;
import org.apache.commons.io.IOUtils;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;

import com.carrotsearch.hppc.ObjectIntOpenHashMap;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;

public class Evaluation {

    private static final String MODEL_FILE = "data/swdf.nt";

    public static void main(String[] args) {
        Evaluation eval = new Evaluation();
        System.out.println(eval.crossValidationError(10, new Baseline()));
    }

    @SuppressWarnings("unchecked")
    public double crossValidationError(int n, Approach approach) throws IOException {
        List<String> queries = QueryLoader.loadQueries();
        int partSize = queries.size() / n;
        List<List<String>> partitions = new ArrayList<>(n);
        for (int i = 0; i < (n - 1); i++) {
            partitions.add(queries.subList(i * partSize, (i + 1) * partSize));
        }

        Model model = readModel(MODEL_FILE);
        ObjectIntOpenHashMap<String> gsResults[] = countResources(partitions, model);

        double rootMeanSquareSum = 0;
        List<String> training, predicted;
        for (int i = 0; i < n; i++) {
            training = generateTrainingSet(i, partitions);
            predicted = approach.generateResourceRanking(training, model);
            rootMeanSquareSum += RMSD.getRMSD(predicted, generateExpectedResult(i, gsResults));
        }
        return rootMeanSquareSum;
    }

    private List<String> generateTrainingSet(int fold, List<List<String>> partitions) {
        int size = 0;
        for (int j = 0; j < partitions.size(); ++j) {
            if (fold != j) {
                size += partitions.get(j).size();
            }
        }
        List<String> trainingQueries = new ArrayList<String>(size);
        for (int j = 0; j < trainingQueries.size(); ++j) {
            if (fold != j) {
                trainingQueries.addAll(partitions.get(j));
            }
        }
        return trainingQueries;
    }

    private Map<String, List<Double>> generateExpectedResult(int fold, ObjectIntOpenHashMap<String>[] gsResults) {
        ObjectIntOpenHashMap<String> expectedResults = new ObjectIntOpenHashMap<String>();
        for (int i = 0; i < gsResults.length; ++i) {
            for (int j = 0; j < gsResults[i].allocated.length; ++j) {
                if (gsResults[i].allocated[j]) {
                    expectedResults.putOrAdd((String) ((Object[]) gsResults[i].keys)[j], gsResults[i].values[j],
                            gsResults[i].values[j]);
                }
            }
        }
        return generateUriRankRangeMapping(expectedResults);
    }

    protected static Model readModel(String modelFile) throws FileNotFoundException {
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

    @SuppressWarnings("rawtypes")
    protected ObjectIntOpenHashMap[] countResources(List<List<String>> partitions, Model model) {
        Baseline goldApproach = new Baseline();
        QueryExecutionFactoryModel factory = new QueryExecutionFactoryModel(model);
        ObjectIntOpenHashMap countedResources[] = new ObjectIntOpenHashMap[partitions.size()];
        ObjectIntOpenHashMap<String> partitionCounts = new ObjectIntOpenHashMap<String>();
        List<String> queries;
        for (int i = 0; i < countedResources.length; ++i) {
            partitionCounts = new ObjectIntOpenHashMap<String>();
            countedResources[i] = partitionCounts;
            queries = partitions.get(i);
            for (String query : queries) {
                goldApproach.addResultCounts(factory, query, partitionCounts);
            }
        }
        return countedResources;
    }

    protected Map<String, List<Double>> generateUriRankRangeMapping(ObjectIntOpenHashMap<String> countedResources) {
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
        Map<String, List<Double>> uriPosMapping = new HashMap<String, List<Double>>((int) (1.75 * uris.length));
        int minRank, maxRank;
        for (int i = 0; i < uris.length; ++i) {
            minRank = i;
            while ((minRank > 0) && (counts[minRank - 1] == counts[minRank])) {
                --minRank;
            }
            maxRank = i;
            while ((maxRank < uris.length) && (counts[maxRank + 1] == counts[maxRank])) {
                ++maxRank;
            }
            uriPosMapping.put(uris[i], Arrays.asList(new Double(minRank), new Double(maxRank)));
        }
        return uriPosMapping;
    }
}