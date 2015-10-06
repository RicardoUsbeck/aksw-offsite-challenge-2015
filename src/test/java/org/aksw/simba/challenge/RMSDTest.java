package org.aksw.simba.challenge;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

public class RMSDTest {

    /**
     * Test for RMSD. Expected result is sqrt(2).
     */
    @Test
    public void test() {
        List<String> response = new ArrayList<>();
        response.add("B");
        response.add("A");
        response.add("C");
        response.add("D");
        response.add("E");

        Map<String, List<Double>> rankRange = new HashMap<>();
        List<Double> zero = new ArrayList<>();
        zero.add(0d);
        zero.add(0d);
        List<Double> one = new ArrayList<>();
        one.add(1d);
        one.add(1d);
        List<Double> two = new ArrayList<>();
        two.add(2d);
        two.add(2d);
        List<Double> threeFour = new ArrayList<>();
        threeFour.add(3d);
        threeFour.add(4d);

        rankRange.put("A", zero);
        rankRange.put("B", one);
        rankRange.put("C", two);
        rankRange.put("D", threeFour);
        rankRange.put("E", threeFour);

        Assert.assertEquals(Math.sqrt(2), RMSD.getRMSD(response, rankRange), 0.000001);
    }
}
