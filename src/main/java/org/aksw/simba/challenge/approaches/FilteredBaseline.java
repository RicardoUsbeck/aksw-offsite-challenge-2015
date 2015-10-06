package org.aksw.simba.challenge.approaches;

import java.util.List;

import com.hp.hpl.jena.rdf.model.Model;

public class FilteredBaseline extends Baseline {
	@Override
	public List<String> generateResourceRanking(List<String> queries, Model knowledgeBase) {
		for (int i = 0; i < queries.size(); ++i) {
			String query = queries.get(i);
			if (query.contains("sample(")) {
				queries.remove(i);
			}
		}

		return super.generateResourceRanking(queries, knowledgeBase);
	}
}
