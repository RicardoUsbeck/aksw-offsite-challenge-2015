package org.aksw.simba.challenge.approaches;

import java.util.List;

import com.hp.hpl.jena.rdf.model.Model;

public interface Approach {

    public List<String> generateResourceRanking(List<String> queries, Model knowledgeBase);
}
