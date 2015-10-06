package org.aksw.simba.challenge;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Query Loader Class
 * 
 * @author Saleem
 *
 */
public class QueryLoader {

    public static final String TRAINING_QUERIES_FILE = "data/trained_queries.txt";
    public static final String CLEANED_TRAINING_QUERIES_FILE = "data/cleaned_queries.txt";

    public static void main(String[] args) throws IOException {
        List<String> queries = loadQueries(TRAINING_QUERIES_FILE);
        System.out.println(queries.size());
        for (String query : queries)
            System.out.println(query);

    }

    /**
     * Load queries into a List from queries file
     * 
     * @return List of queries
     * @throws IOException
     */
    public static List<String> loadQueries() throws IOException {
        return loadQueries(TRAINING_QUERIES_FILE);
    }

    /**
     * Load queries into a List from queries file
     * 
     * @param queriesFile
     *            Queries file
     * @return List of queries
     * @throws IOException
     */
    public static List<String> loadQueries(String queriesFile) throws IOException {
        List<String> queries = new ArrayList<String>();
        BufferedReader br = new BufferedReader(new FileReader(queriesFile));
        String line;
        while ((line = br.readLine()) != null) {
            queries.add(decodeQuery(line));
        }
        br.close();
        return queries;
    }

    /**
     * Decode query
     * 
     * @param query
     *            query string
     * @return decoded query
     */
    public static String decodeQuery(String query) {
        try {
            query = java.net.URLDecoder.decode(query, "UTF-8");
        } catch (Exception e) {// System.err.println(query+ " "+
                               // e.getMessage());
        }

        return query + "\n";
    }

    /**
     * Encode query
     * 
     * @param query
     *            query string
     * @return decoded query
     */
    public static String encodeQuery(String query) {
        try {
            query = java.net.URLEncoder.encode(query, "UTF-8");
        } catch (Exception e) {// System.err.println(query+ " "+
                               // e.getMessage());
        }

        return query + "\n";
    }
}
