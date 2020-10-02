package com.jperch;

import com.telmomenezes.jfastemd.*;

import java.util.*;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

class TestPerch {
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

    static void levelTransversal(Pnode root) {
        Queue<Pnode> queue = new LinkedList<>();
        queue.add(root);
        while(!queue.isEmpty()) {
            int size = queue.size();
            String level = " ";
            for(int i = 0; i < size; i++) {
                Pnode cur = queue.poll();
                level += "[";
                for (Signature s : cur.sigs) {
                    level += " " + s.getLabel(); 
                }
                level += " ] ";
                for (Pnode child : cur.children) {
                    queue.add(child);
                }
            }
            System.out.println(level);
        }
    }

    public static void main (String[] args) {
        Ptree t = new Ptree();
        String prefix = "./data/data1024/feature";
        Map<Signature, Map<Signature, Double>> dist = new HashMap<>();
        StringBuilder sb = new StringBuilder();
        try {
            List<Signature> s = new LinkedList<>();
            System.out.println("Start reading features from files.");
            System.out.println("++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
            for (int i = 0; i < 1000; i++) {
                s.add(getSignature(prefix + i + ".csv", 2));
                s.get(i).setLabel("signature" + i);
                //System.out.println("Get feature " + i + " file");
            }
            Collections.shuffle(s);
            System.out.println("++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
            System.out.println("Finish reading all features!");
            System.out.println("");
            System.out.println("Start inserting features!");
            System.out.println("++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
            for (int i = 0; i < 1000; i++) {
                Signature nearest = t.updateDist(s.get(i));
                long start = System.nanoTime();
                t.insert(s.get(i), nearest);
                long end = System.nanoTime();
                //System.out.println(i + "th insertion time is " + (end - start));
                sb.append((end - start) + ",");
            }
            /*List<Pnode> res = t.cluster(10);
            String c;
            int counter = 0;
            System.out.println("The clustering result is:");
            for (Pnode p : res) {
                c = "[";
                for (Signature tmp : p.sigs) {
                    c += " " + tmp.getLabel();
                }
                c += " ] ";
                counter++;
                System.out.println("Cluster " + counter + " is" + c);
            }*/
        } catch (FileNotFoundException e) {
            System.out.println("Cannot open the file!");
        }

        /*try {
            BufferedWriter writer = new BufferedWriter(new FileWriter("microPerch.csv"));
            writer.write(sb.toString());//save the string representation of the board
            writer.close();
        } catch (IOException e) {
            System.out.println("cannot write");
        }*/
        System.out.println("Test is done!");
    }
}
