package org.aksw.simba.challenge.approaches;

import java.util.ArrayList;
import java.util.List;

import org.aksw.simba.challenge.QueryExecutor;

import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.rdf.model.Model;

public class LimitDeleter extends Baseline {

    public LimitDeleter(QueryExecutor executer) {
        super(executer);
    }

    @Override
    public List<String> generateResourceRanking(List<String> queries, Model knowledgeBase) {
        List<String> fixedQueries = new ArrayList<String>();
        Query q;
        for (String queryString : queries) {
            q = this.executer.getFactory().createQueryExecution(queryString).getQuery();
            if (q.hasLimit()) {
                q.setLimit(Query.NOLIMIT);
            }
            fixedQueries.add(q.toString());
        }
        return super.generateResourceRanking(fixedQueries, knowledgeBase);
    }
}
