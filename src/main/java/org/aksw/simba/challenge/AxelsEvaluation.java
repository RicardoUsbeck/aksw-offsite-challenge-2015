package org.aksw.simba.challenge;

import java.io.File;
import java.io.IOException;

import org.aksw.simba.challenge.approaches.FileBasedApproach;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hp.hpl.jena.rdf.model.Model;

public class AxelsEvaluation {

    private static final Logger LOGGER = LoggerFactory.getLogger(AxelsEvaluation.class);

    // "AXEL HAS TO PUT THE CORRECT FILE NAME!!!"
    private static final String TEST_QUERIES[] = new String[] { QueryLoader.CLEANED_TRAINING_QUERIES_FILE };
    // "AXEL HAS TO PUT THE CORRECT FILE NAMES OF THE PARTICIPANTS!!!"
    private static final String PARTICIPANT_FILES[] = new String[] { "data/baselineOutput.txt" };

    public static void main(String[] args) throws IOException {
        Evaluation eval;
        Model model = Evaluation.readModel(Evaluation.MODEL_FILE);
        QueryExecutor qe = new QueryExecutor(model);
        double error;
        for (int j = 0; j < TEST_QUERIES.length; ++j) {
            eval = new Evaluation(TEST_QUERIES[j]);
            for (int i = 0; i < PARTICIPANT_FILES.length; ++i) {
                LOGGER.info("Evaluating " + PARTICIPANT_FILES[i] + " on " + TEST_QUERIES[j] + "...");
                error = eval.validate(new FileBasedApproach(new File(PARTICIPANT_FILES[i])), model, qe);
                LOGGER.info(PARTICIPANT_FILES[i] + " on " + TEST_QUERIES[j] + " has an error of " + error);
            }
        }
        LOGGER.info("Finished!");
        qe.close();
    }
}
