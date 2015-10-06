package org.aksw.simba.challenge.cleanloading;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.aksw.jena_sparql_api.core.QueryExecutionFactory;
import org.aksw.jena_sparql_api.model.QueryExecutionFactoryModel;
import org.aksw.jena_sparql_api.timeout.QueryExecutionFactoryTimeout;
import org.aksw.simba.challenge.Evaluation;
import org.aksw.simba.challenge.QueryLoader;

import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.RDFNode;

public class CleanLoader {
	public static final String TRAINING_QUERIES_FILE = "data/trained_queries.txt";
	private static final String MODEL_FILE = "data/swdf.nt";

	public static void main(String[] args) throws IOException {
		// load the queries
		List<String> queryList = QueryLoader.loadQueries(TRAINING_QUERIES_FILE);

		// run them against the database
		Model model = Evaluation.readModel(MODEL_FILE);
		QueryExecutionFactory factory = new QueryExecutionFactoryModel(model);
		factory = new QueryExecutionFactoryTimeout(factory, 1000);

		// select non empty resultsets
		List<String> okQueries = new ArrayList<String>();
		for (String query : queryList) {
			try {
				QueryExecution qe = factory.createQueryExecution(query);
				ResultSet rs = qe.execSelect();
				if (rs.hasNext()) {
					okQueries.add(query);
				}
			} catch (Exception e) {

			}
			System.out.println(query);
		}
		// write to file
		BufferedWriter bw = new BufferedWriter(new FileWriter("data/cleaned_queries.txt"));
		for (String query : okQueries) {
			bw.write(QueryLoader.encodeQuery(query));
		}
		bw.close();
		System.out.println("Done");
	}
}
