package org.aksw.simba.challenge;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.aksw.simba.challenge.approaches.Approach;
import org.aksw.simba.challenge.approaches.Baseline;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.carrotsearch.hppc.ObjectIntOpenHashMap;
import com.hp.hpl.jena.rdf.model.Model;

public class BruteForceValidation extends Evaluation {

    private static final Logger LOGGER = LoggerFactory.getLogger(BruteForceValidation.class);

    private static final int NUMBER_OF_TRIES = 1000;
    private static final int NUMBER_OF_FOLDS = 10;
    private static final String OUTPUT_FOLDER = "data/";

    public static void main(String[] args) throws IOException {
        BruteForceValidation bfv = new BruteForceValidation(QueryLoader.CLEANED_TRAINING_QUERIES_FILE);
        Model model = readModel(MODEL_FILE);
        QueryExecutor executer = new QueryExecutor(model);
        bfv.run(new Baseline(executer), model, executer);
        executer.close();
    }

    public BruteForceValidation(String queryFile) {
        super(queryFile);
    }

    @SuppressWarnings("unchecked")
    public void run(Approach approach, Model model, QueryExecutor executer) throws IOException {
        // read queries
        List<String> queries = QueryLoader.loadQueries(QueryLoader.CLEANED_TRAINING_QUERIES_FILE);
        int verificationSize = (int) (0.2 * queries.size());
        List<String> crossValidationData, verificationData;
        FoldResult currentFold;
        double verificationError, bestError = Double.MAX_VALUE;
        long timestamp;
        // 0...1000
        for (int i = 0; i < NUMBER_OF_TRIES; ++i) {
            // shuffle queries
            Collections.shuffle(queries);
            // generate 80 20 split
            verificationData = queries.subList(0, verificationSize);
            crossValidationData = queries.subList(verificationSize, queries.size());
            // perform 10 fold cross validation on 80%
            // get the best trained result
            currentFold = findBestFold(NUMBER_OF_FOLDS, crossValidationData, model, executer, approach);
            // test on 20%
            verificationError = RMSD.getRMSD(currentFold.prediction,
                    generateUriRankRangeMapping(countResources(Arrays.asList(verificationData), model, executer)[0]));
            // generate average of error of cross validation and 20 % test
            currentFold.error = (currentFold.error + verificationError) / 2.0;
            if (currentFold.error < bestError) {
                timestamp = System.currentTimeMillis();
                LOGGER.info("Found a new best fold (error = " + currentFold.error
                        + "). Writing it to file with timestamp " + timestamp + ".");
                bestError = currentFold.error;
                writeFold(currentFold.prediction, timestamp);
            } else {
                LOGGER.info(
                        "This fold is not able to beat the best fold (" + currentFold.error + " > " + bestError + ").");
            }
        }
        LOGGER.info("Finished.");
    }

    @SuppressWarnings("unchecked")
    public FoldResult findBestFold(int n, List<String> queries, Model model, Approach approach) throws IOException {
        int partSize = queries.size() / n;
        List<List<String>> partitions = new ArrayList<>(n);
        for (int i = 0; i < (n - 1); i++) {
            partitions.add(queries.subList(i * partSize, (i + 1) * partSize));
        }
        partitions.add(queries.subList((n - 1) * partSize, queries.size()));

        LOGGER.info("Generating expected counts...");
        ObjectIntOpenHashMap<String> gsResults[] = countResources(partitions, model);

        List<String> training;
        FoldResult currentFold, bestFold = null;
        for (int i = 0; i < n; i++) {
            LOGGER.info("Starting fold " + i + "...");
            training = generateTrainingSet(i, partitions);
            currentFold = new FoldResult(approach.generateResourceRanking(training, model));
            currentFold.error = RMSD.getRMSD(currentFold.prediction, generateExpectedResult(i, gsResults));
            LOGGER.info("Error of fold " + i + " = " + currentFold.error);
            if ((bestFold == null) || (bestFold.error > currentFold.error)) {
                bestFold = currentFold;
            }
        }
        return bestFold;
    }

    @SuppressWarnings("unchecked")
    public FoldResult findBestFold(int n, List<String> queries, Model model, QueryExecutor executer, Approach approach)
            throws IOException {
        int partSize = queries.size() / n;
        List<List<String>> partitions = new ArrayList<>(n);
        for (int i = 0; i < (n - 1); i++) {
            partitions.add(queries.subList(i * partSize, (i + 1) * partSize));
        }
        partitions.add(queries.subList((n - 1) * partSize, queries.size()));

        LOGGER.info("Generating expected counts...");
        ObjectIntOpenHashMap<String> gsResults[] = countResources(partitions, model, executer);

        List<String> training;
        FoldResult currentFold, bestFold = null;
        for (int i = 0; i < n; i++) {
            LOGGER.info("Starting fold " + i + "...");
            training = generateTrainingSet(i, partitions);
            currentFold = new FoldResult(approach.generateResourceRanking(training, model));
            currentFold.error = RMSD.getRMSD(currentFold.prediction, generateExpectedResult(i, gsResults));
            LOGGER.info("Error of fold " + i + " = " + currentFold.error);
            if ((bestFold == null) || (bestFold.error > currentFold.error)) {
                bestFold = currentFold;
            }
        }
        return bestFold;
    }

    public static class FoldResult {
        public List<String> prediction;
        public double error;

        public FoldResult(List<String> prediction) {
            super();
            this.prediction = prediction;
        }

    }

    public static void writeFold(List<String> uris, long time) {
        try {
            FileUtils.writeLines(new File(OUTPUT_FOLDER + "results_" + time + ".txt"), uris);
        } catch (IOException e) {
            LOGGER.error("Couldn't write result to file.");
        }
    }
}
