package com.jperch;

import com.telmomenezes.jfastemd.*;

import java.util.*;
import java.io.File;
import java.io.FileNotFoundException;
import smile.clustering.linkage.*;
import smile.clustering.HierarchicalClustering;
import java.util.concurrent.ThreadLocalRandom;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

class TestHAC {
    static double[][] dist;
    static Signature getSignature(String fileName, int dimension) throws FileNotFoundException {
        Scanner scanner = new Scanner(new File(fileName)); 
        List<Double> list = new ArrayList<>();
        while (scanner.hasNextDouble()) {
            list.add(scanner.nextDouble());
        }
        scanner.close();

        int n = list.size() / dimension;
        FeatureND[] features = new FeatureND[n];
        double[] weights = new double[n];

        for (int i = 0; i < n; i++) {
            List<Double> tmp = new LinkedList<>();
            for (int j = 0; j < dimension; j++) {
                tmp.add(list.get(i + j * n));
            }
            FeatureND f = new FeatureND(tmp);
            features[i] = f;
            weights[i] = 1.0 / n;
        }
        
        Signature signature = new Signature();
        signature.setNumberOfFeatures(n);
        signature.setFeatures(features);
        signature.setWeights(weights);

        return signature;
    } 

    static void updateDist(Signature x, Signature[] sigs, int index) {
        dist[index] = new double[index + 1];
        dist[index][index] = 0;
        for (int i = 0; i < index; i++) {
            Signature s = sigs[i];
            double d = JFastEMD.distance(s, x, 0, 0.7);
            dist[index][i] = d;
        }
    }

    public static void main (String[] args) {
        String prefix = "/xcluster/data/data1024/feature";
        dist = new double[1000][];
        StringBuilder sb = new StringBuilder();
        try {
            Signature[] sigs = new Signature[1000];
            System.out.println("Start reading features from files.");
            System.out.println("++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
            for (int i = 0; i < 1000; i++) {
                Signature x = getSignature(prefix + i + ".csv", 2); 
                x.setLabel("signature" + i);
                sigs[i] = x;
            }
            shuffleArray(sigs);
            System.out.println("++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
            System.out.println("Finish reading all features!");
            System.out.println("");
            for (int i = 0; i < 1000; i++) {
                updateDist(sigs[i], sigs, i);
                double[][] tmp = new double[i + 1][];
                for (int j = 0; j <= i; j++) {
                    tmp[j] = dist[j];
                }
                long[] costs = hcClustering(tmp);
                for (long cost : costs) {
                    sb.append(cost + ",");
                }
                sb.append("\n");
                System.out.println("finish " + i + "the insertion");
            }
        } catch (FileNotFoundException e) {
            System.out.println("Cannot open the file!");
        }
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter("microHAC.csv"));
            writer.write(sb.toString());//save the string representation of the board
            writer.close();
        } catch (IOException e) {
            System.out.println("cannot write");
        }
        System.out.println("Test is done!");
    }

    // Implementing Fisher–Yates shuffle
    static void shuffleArray(Signature[] ar)
    {
        // If running on Java 6 or older, use `new Random()` on RHS here
        Random rnd = ThreadLocalRandom.current();
        for (int i = ar.length - 1; i > 0; i--)
        {
            int index = rnd.nextInt(i + 1);
            // Simple swap
            Signature a = ar[index];
            ar[index] = ar[i];
            ar[i] = a;
        }
    }

    private static long[] hcClustering(double[][] dist){
        Linkage linkage = null;
        long[] cost = new long[7];
        System.out.println("++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
        for (int i = 0; i < 7; i++) {
            switch (i) {
                case 0:
                    linkage = new SingleLinkage(dist);
                    break;
                case 1:
                    linkage = new CompleteLinkage(dist);
                    break;
                case 2:
                    linkage = new UPGMALinkage(dist);
                    break;
                case 3:
                    linkage = new WPGMALinkage(dist);
                    break;
                case 4:
                    linkage = new UPGMCLinkage(dist);
                    break;
                case 5:
                    linkage = new WPGMCLinkage(dist);
                    break;
                case 6:
                    linkage = new WardLinkage(dist);
                    break;
            }
            long startTime = System.nanoTime();
            HierarchicalClustering hc = HierarchicalClustering.fit(linkage);
            long endTime = System.nanoTime();
            cost[i] = endTime - startTime;
            System.out.println("when there are " + dist.length + " elements, the clustering time is " + (endTime - startTime));
        }
        return cost;
    }
}
