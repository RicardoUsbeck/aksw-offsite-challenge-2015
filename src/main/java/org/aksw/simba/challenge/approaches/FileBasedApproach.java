package org.aksw.simba.challenge.approaches;

import java.io.File;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hp.hpl.jena.rdf.model.Model;

public class FileBasedApproach implements Approach {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileBasedApproach.class);
    private File file;

    public FileBasedApproach(File file) {
        this.file = file;
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<String> generateResourceRanking(List<String> queries, Model knowledgeBase) {
        try {
            return FileUtils.readLines(file);
        } catch (Exception e) {
            LOGGER.error("Exception while trying to read results from file. Returning null.");
            return null;
        }
    }

}
