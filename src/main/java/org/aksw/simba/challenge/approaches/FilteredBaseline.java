package org.aksw.simba.challenge.approaches;

import java.util.List;

import org.aksw.simba.challenge.QueryExecutor;

import com.hp.hpl.jena.rdf.model.Model;

public class FilteredBaseline extends Baseline {

    public FilteredBaseline(QueryExecutor executer) {
        super(executer);
    }

    @SuppressWarnings("deprecation")
    public FilteredBaseline() {
        super();
    }

    @Override
    public List<String> generateResourceRanking(List<String> queries, Model knowledgeBase) {
        for (int i = 0; i < queries.size(); ++i) {
            String query = queries.get(i);
            if (query.contains("sample(")) {
                queries.remove(i);
            } else if (query.contains("LIMIT")) {
                if (!query.contains("ORDER BY ")) {
                    queries.remove(i);
                }
            }

        }

        return super.generateResourceRanking(queries, knowledgeBase);
    }
}
