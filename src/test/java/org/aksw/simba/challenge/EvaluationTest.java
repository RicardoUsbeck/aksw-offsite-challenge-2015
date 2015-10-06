package org.aksw.simba.challenge;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.aksw.jena_sparql_api.core.QueryExecutionFactory;
import org.aksw.simba.challenge.approaches.Baseline;
import org.junit.Test;

import com.carrotsearch.hppc.ObjectIntOpenHashMap;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.sparql.engine.ref.Evaluator;

import junit.framework.Assert;

public class EvaluationTest {

    @Test
    public void test() {
        String approachResult[] = new String[] { "0", "1", "2", "3" };

        ObjectIntOpenHashMap<String> countedResources = new ObjectIntOpenHashMap<String>();
        countedResources.put("0", 9);
        countedResources.put("1", 8);
        countedResources.put("2", 8);
        countedResources.put("3", 6);

        Evaluation eval = new Evaluation("");
        Assert.assertEquals(0,
                RMSD.getRMSD(Arrays.asList(approachResult), eval.generateUriRankRangeMapping(countedResources)),
                0.000001);
    }

    @SuppressWarnings("deprecation")
    @Test
    public void testReadWrite() throws IOException {
        Baseline bs = new Baseline();
        List<String> queries = QueryLoader.loadQueries();
        Model model = Evaluation.readModel(Evaluation.MODEL_FILE);
        List<String> approachResult = bs.generateResourceRanking(queries, model);
        Evaluation.generateMapWithAllResources(model);
        QueryExecutionFactory factory = Evaluation.createQueryExecutionFactory(model);

        ObjectIntOpenHashMap<String> countedResources = Evaluation.generateMapWithAllResources(model);
        for (String query : queries) {
            bs.addResultCounts(factory, query, countedResources);
        }

        Evaluation eval = new Evaluation(QueryLoader.TRAINING_QUERIES_FILE);
        Assert.assertEquals(0, RMSD.getRMSD(approachResult, eval.generateUriRankRangeMapping(countedResources)),
                0.000001);
    }

    @SuppressWarnings("deprecation")
    @Test
    public void testValidate() throws IOException {
        Evaluation eval = new Evaluation(QueryLoader.CLEANED_TRAINING_QUERIES_FILE);
        Assert.assertEquals(0, eval.validate(new Baseline()), 0.000001);
    }
}
