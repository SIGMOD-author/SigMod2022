package com.telmomenezes.jfastemd;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.concurrent.TimeUnit;
import java.util.Scanner;
import java.util.List;
import java.util.LinkedList;
import java.util.ArrayList;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

/**
 * @author author 
 *
 */

public class TestMicro {

    static Signature getNDSignature(String fileName) throws FileNotFoundException {
        Scanner scanner = new Scanner(new File(fileName)); 
        List<Double> list = new LinkedList<>();
        while (scanner.hasNextDouble()) {
            list.add(scanner.nextDouble());
        }
        scanner.close();

        int n = list.size() / 1024;
        System.out.println(n);
        FeatureND[] features = new FeatureND[n];
        double[] weights = new double[n];

        for (int i = 0; i < n; i++) {
            List<Double> tmp = new ArrayList<>();
            tmp.add(list.get(i));
            tmp.add(list.get(i + n));
            FeatureND f = new FeatureND(tmp);
            features[i] = f;
            weights[i] = 1.0 / n;
        }
        
        Signature signature = new Signature();
        signature.setNumberOfFeatures(n);
        signature.setFeatures(features);
        signature.setWeights(weights);
        signature.setLabel("test");

        return signature;
    } 

    static double emdDist(Signature sig1, Signature sig2, double thresholdRatio) {
        double dist = 0;
        dist = JFastEMD.distance(sig1, sig2, -1, thresholdRatio);
        return dist;
    }

    public static void main(String[] args) {
        String root = "JFastEMD/src/com/telmomenezes/jfastemd/f";
        List<Signature> sigs = new LinkedList<>();
        // read in the signature;
        for (int i = 1; i <= 10; i++) {
            try {
                sigs.add(getNDSignature(root + i + ".csv"));
            } catch (FileNotFoundException e) {
                System.out.println("Cannot open the file!");
            }
        }
        double[] ratios = {1, 0.9, 0.8, 0.7, 0.6, 0.5};
        long[][] elapsedTimes = new long[ratios.length][sigs.size() - 1];
        double[][] dists = new double[ratios.length][sigs.size() - 1];
        String ds = "";
        String et = "";
        for (int k = 0; k < ratios.length; k++) {
            double thresholdRatio = ratios[k];
            for (int i = 0; i < sigs.size() - 1; i++) {
                long startTime = System.currentTimeMillis();
                double dist = emdDist(sigs.get(i), sigs.get(i + 1), thresholdRatio);
                long endTime = System.currentTimeMillis(); 
                dists[k][i] = dist;
                elapsedTimes[k][i] = endTime - startTime;
                ds += dist;
                et += endTime - startTime;
                System.out.println(dist);
                System.out.println("Elapse time under threshold " + thresholdRatio + " is " + (endTime - startTime));
                if (i != sigs.size() - 1) {
                    ds += ",";
                    et += ",";
                }
            }
            ds += "\n";
            et += "\n";
        }
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter("testMicroTime.csv"));
            writer.write(et);//save the string representation of the board
            writer.close();
            writer = new BufferedWriter(new FileWriter("testMicroDist.csv"));
            writer.write(ds);//save the string representation of the board
            writer.close();
        } catch (IOException e) {
            System.out.println("cannot write");
        }
    }
}
