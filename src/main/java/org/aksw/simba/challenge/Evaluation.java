package org.aksw.simba.challenge;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.aksw.jena_sparql_api.core.QueryExecutionFactory;
import org.aksw.jena_sparql_api.model.QueryExecutionFactoryModel;
import org.aksw.jena_sparql_api.timeout.QueryExecutionFactoryTimeout;
import org.aksw.simba.challenge.approaches.Approach;
import org.aksw.simba.challenge.approaches.Baseline;
import org.aksw.simba.topicmodeling.commons.sort.AssociativeSort;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.carrotsearch.hppc.ObjectIntOpenHashMap;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.NodeIterator;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;

public class Evaluation {

    private static final Logger LOGGER = LoggerFactory.getLogger(Evaluation.class);

    protected static final String MODEL_FILE = "data/swdf.nt";
    private static final String CACHE_FOLDER = "./data/cache";

    private static final long CACHE_TIME_TO_LIVE = 7 * 24 * 60 * 60 * 1000;

    private static ObjectIntOpenHashMap<String> resourcesWithoutCounts = null;

    public static void main(String[] args) throws IOException {
        Evaluation eval = new Evaluation();
        System.out.println("average error = " + eval.crossValidationError(10, new Baseline()));
    }

    @SuppressWarnings("unchecked")
    public double crossValidationError(int n, Approach approach) throws IOException {
        List<String> queries = QueryLoader.loadQueries(QueryLoader.CLEANED_TRAINING_QUERIES_FILE);
        int partSize = queries.size() / n;
        List<List<String>> partitions = new ArrayList<>(n);
        for (int i = 0; i < (n - 1); i++) {
            partitions.add(queries.subList(i * partSize, (i + 1) * partSize));
        }
        partitions.add(queries.subList((n - 1) * partSize, queries.size()));

        Model model = readModel(MODEL_FILE);
        LOGGER.info("Generating expected counts...");
        ObjectIntOpenHashMap<String> gsResults[] = countResources(partitions, model);

        double rootMeanSquareSum = 0;
        double foldErrors[] = new double[n];
        List<String> training, predicted;
        for (int i = 0; i < n; i++) {
            LOGGER.info("Starting fold " + i + "...");
            training = generateTrainingSet(i, partitions);
            predicted = approach.generateResourceRanking(training, model);
            foldErrors[i] = RMSD.getRMSD(predicted, generateExpectedResult(i, gsResults));
            LOGGER.info("Error of fold " + i + " = " + foldErrors[i]);
            rootMeanSquareSum += foldErrors[i];
        }
        LOGGER.info("Error of folds " + Arrays.toString(foldErrors));
        return rootMeanSquareSum / n;
    }

    protected List<String> generateTrainingSet(int fold, List<List<String>> partitions) {
        int size = 0;
        for (int j = 0; j < partitions.size(); ++j) {
            if (fold != j) {
                size += partitions.get(j).size();
            }
        }
        List<String> trainingQueries = new ArrayList<String>(size);
        for (int j = 0; j < partitions.size(); ++j) {
            if (fold != j) {
                trainingQueries.addAll(partitions.get(j));
            }
        }
        return trainingQueries;
    }

    protected Map<String, List<Double>> generateExpectedResult(int fold, ObjectIntOpenHashMap<String>[] gsResults) {
        return generateUriRankRangeMapping(gsResults[fold]);
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

    @SuppressWarnings("rawtypes")
    protected ObjectIntOpenHashMap[] countResources(List<List<String>> partitions, Model model) {
        Baseline goldApproach = new Baseline();
        QueryExecutionFactory factory = createQueryExecutionFactory(model);
        ObjectIntOpenHashMap countedResources[] = new ObjectIntOpenHashMap[partitions.size()];
        ObjectIntOpenHashMap<String> partitionCounts = new ObjectIntOpenHashMap<String>();
        List<String> queries;
        for (int i = 0; i < countedResources.length; ++i) {
            partitionCounts = generateMapWithAllResources(model);
            countedResources[i] = partitionCounts;
            queries = partitions.get(i);
            for (String query : queries) {
                goldApproach.addResultCounts(factory, query, partitionCounts);
            }
        }
        factory.close();
        return countedResources;
    }

    @SuppressWarnings("rawtypes")
    protected ObjectIntOpenHashMap[] countResources(List<List<String>> partitions, Model model,
            QueryExecutor executer) {
        Baseline goldApproach = new Baseline(executer);
        // QueryExecutionFactory factory = createQueryExecutionFactory(model);
        ObjectIntOpenHashMap countedResources[] = new ObjectIntOpenHashMap[partitions.size()];
        ObjectIntOpenHashMap<String> partitionCounts = new ObjectIntOpenHashMap<String>();
        List<String> queries;
        for (int i = 0; i < countedResources.length; ++i) {
            partitionCounts = generateMapWithAllResources(model);
            countedResources[i] = partitionCounts;
            queries = partitions.get(i);
            for (String query : queries) {
                goldApproach.addResultCounts(query, partitionCounts);
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
        ArrayUtils.reverse(uris);
        ArrayUtils.reverse(counts);
        Map<String, List<Double>> uriPosMapping = new HashMap<String, List<Double>>((int) (1.75 * uris.length));
        int minRank, maxRank;
        for (int i = 0; i < uris.length; ++i) {
            minRank = i;
            while ((minRank > 0) && (counts[minRank - 1] == counts[minRank])) {
                --minRank;
            }
            maxRank = i;
            while ((maxRank < (counts.length - 1)) && (counts[maxRank + 1] == counts[maxRank])) {
                ++maxRank;
            }
            uriPosMapping.put(uris[i], Arrays.asList(new Double(minRank), new Double(maxRank)));
        }
        return uriPosMapping;
    }

    public static ObjectIntOpenHashMap<String> generateMapWithAllResources(Model model) {
        if (resourcesWithoutCounts == null) {
            resourcesWithoutCounts = new ObjectIntOpenHashMap<String>();
            StmtIterator stmtIter = model.listStatements();
            Statement s;
            while (stmtIter.hasNext()) {
                s = stmtIter.next();
                if (s.getSubject().isResource()) {
                    resourcesWithoutCounts.putIfAbsent(s.getSubject().getURI(), 0);
                }
                if (s.getPredicate().isResource()) {
                    resourcesWithoutCounts.putIfAbsent(s.getPredicate().getURI(), 0);
                }
                if (s.getObject().isResource()) {
                    resourcesWithoutCounts.putIfAbsent(s.getObject().asResource().getURI(), 0);
                }
            }
            NodeIterator nodeIter = model.listObjects();
            RDFNode n;
            while (nodeIter.hasNext()) {
                n = nodeIter.next();
                if (n.isResource()) {
                    resourcesWithoutCounts.putIfAbsent(n.asResource().getURI(), 0);
                }
            }
        }
        return resourcesWithoutCounts.clone();
    }

    public static QueryExecutionFactory createQueryExecutionFactory(Model model) {
        QueryExecutionFactory factory = new QueryExecutionFactoryModel(model);
        factory = new QueryExecutionFactoryTimeout(factory, 1000);
        // CacheBackend cacheBackend;
        // try {
        // cacheBackend = CacheCoreH2.create(true, (new
        // File(CACHE_FOLDER)).getAbsolutePath(), "challenge",
        // CACHE_TIME_TO_LIVE, true);
        // } catch (Exception e) {
        // throw new RuntimeException(e);
        // }
        // CacheFrontend cacheFrontend = new CacheFrontendImpl(cacheBackend);
        // factory = new QueryExecutionFactoryCacheEx(factory, cacheFrontend);
        return factory;
    }
}