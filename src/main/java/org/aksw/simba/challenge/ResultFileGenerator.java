package org.aksw.simba.challenge;

import java.io.File;
import java.io.IOException;

import org.aksw.simba.challenge.approaches.Approach;
import org.aksw.simba.challenge.approaches.Baseline;
import org.apache.commons.io.FileUtils;

import com.hp.hpl.jena.rdf.model.Model;

public class ResultFileGenerator {

    private static final File resultFile = new File("data/baselineOutput.txt");

    public static void main(String[] args) throws IOException {
        Model model = Evaluation.readModel(Evaluation.MODEL_FILE);
        QueryExecutor executor = new QueryExecutor(model);
        Approach a = new Baseline(executor);
        // FileUtils.writeLines(resultFile,
        // a.generateResourceRanking(QueryLoader.loadQueries(QueryLoader.TRAINING_QUERIES_FILE),
        // model));
        FileUtils.writeLines(resultFile,
                a.generateResourceRanking(QueryLoader.loadQueries(QueryLoader.CLEANED_TRAINING_QUERIES_FILE), model));
        System.out.println("Finished!");
        executor.close();
    }
}
