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
        BruteForceValidation bfv = new BruteForceValidation();
        bfv.run(new Baseline());
    }

    @SuppressWarnings("unchecked")
    public void run(Approach approach) throws IOException {
        Model model = readModel(MODEL_FILE);
        // read queries
        List<String> queries = QueryLoader.loadQueries();
        int verificationSize = (int) (0.2 * queries.size());
        List<String> crossValidationData, verificationData;
        FoldResult currentFold;
        double verificationError, bestError = Double.MAX_VALUE;
        int resultCounter = 0;
        // 0...1000
        for (int i = 0; i < NUMBER_OF_TRIES; ++i) {
            // shuffle queries
            Collections.shuffle(queries);
            // generate 80 20 split
            verificationData = queries.subList(0, verificationSize);
            crossValidationData = queries.subList(verificationSize, queries.size());
            // perform 10 fold cross validation on 80%
            // get the best trained result
            currentFold = findBestFold(NUMBER_OF_FOLDS, crossValidationData, model, approach);
            // test on 20%
            verificationError = RMSD.getRMSD(currentFold.prediction,
                    generateUriRankRangeMapping(countResources(Arrays.asList(verificationData), model)[0]));
            // generate average of error of cross validation and 20 % test
            currentFold.error = (currentFold.error + verificationError) / 2.0;
            if (currentFold.error < bestError) {
                LOGGER.info("Found a new best fold (error = " + currentFold.error + "). Writing it to file #"
                        + resultCounter + ".");
                bestError = currentFold.error;
                ++resultCounter;
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

    public static class FoldResult {
        public List<String> prediction;
        public double error;

        public FoldResult(List<String> prediction) {
            super();
            this.prediction = prediction;
        }

    }

    protected void writeFold(List<String> uris, int id) {
        try {
            FileUtils.writeLines(new File(OUTPUT_FOLDER + "results_" + id + ".txt"), uris);
        } catch (IOException e) {
            LOGGER.error("Couldn't write result to file.");
        }
    }
}
