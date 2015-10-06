/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.aksw.simba.challenge;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Evaluation method for the AKSW Challenge 2015
 * @author ngonga
 */
public class RMSD {
    
    /**
     * Implementation of RMSD. 
     * @param response Output of an algorithm, read from a file. 
     * @param rankRange Rank ranges computed by executing real queries. Map a 
     * resource to a list with allowed begin and end rank
     * @return The RMSD score.
     */
    public static double getRMSD(List<String> response, Map<String, List<Double>> rankRange)
    {
        int size = rankRange.keySet().size();
        //Set of foundResources returned by tool
        Set<String> foundResources = new HashSet<>();
        
        double error = 0d;
        String resource;
        
        for(int i=0; i<response.size(); i++)
        {
            resource = response.get(i);
            //remember seen foundResources
            foundResources.add(resource);
            
            //compute error for this resource
            //unknown foundResources will not be ranked
            if(rankRange.containsKey(resource))
            {
                //range contains min and max rank
                List<Double> range = rankRange.get(resource);
                //if resource not within acceptable range then increase total error
                if(!(i >= range.get(0) && i <= range.get(1)))
                {
                    error = error + Math.min(Math.pow(i-range.get(0), 2),
                            Math.pow(i-range.get(1), 2));
                }
            }                            
        }
        
        //now check for foundResources that were not ranked and assign them the max error
        for(String r: rankRange.keySet())
        {
            if(!foundResources.contains(r))
                error = error + Math.pow(size, 2);
        }
        return Math.sqrt(error);
    }
}
