package com.jperch;

import com.telmomenezes.jfastemd.*;

import java.util.*;
import java.io.File;
import java.io.FileNotFoundException;

class Test {
    static Signature getSignature(String fileName) throws FileNotFoundException {
        Scanner scanner = new Scanner(new File(fileName)); 
        List<Double> list = new LinkedList<>();
        while (scanner.hasNextDouble()) {
            list.add(scanner.nextDouble());
        }
        scanner.close();

        int n = list.size() / 2;
        int m = 10;
        Feature2D[] features = new Feature2D[m];
        double[] weights = new double[m];

        for (int i = 0; i < m; i++) {
            Feature2D f = new Feature2D(list.get(i), list.get(i + n));
            features[i] = f;
            weights[i] = 1.0 / m;
        }
        
        Signature signature = new Signature();
        signature.setNumberOfFeatures(m);
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
        String[] r = new String[5];
        r[0] = "r0.csv";
        r[1] = "r1.csv";
        r[2] = "r2.csv";
        r[3] = "r3.csv";
        r[4] = "r4.csv";
        try {
            Signature[] s = new Signature[5];
            for (int i = 0; i < 5; i++) {
                s[i] = getSignature(r[i]);
                s[i].setLabel("signature" + i);
            }
            for (int i = 0; i < 5; i++) {
                t.insert(s[i]);
                levelTransversal(t.root);
            }
            levelTransversal(t.root);
            List<Pnode> res = t.cluster(4);
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
            }
            t.remove(s[0]);
            levelTransversal(t.root);
            res = t.cluster(4);
            counter = 0;
            System.out.println("The post-removal clustering result is:");
            for (Pnode p : res) {
                c = "[";
                for (Signature tmp : p.sigs) {
                    c += " " + tmp.getLabel();
                }
                c += " ] ";
                counter++;
                System.out.println("Cluster " + counter + " is" + c);
            }
        } catch (FileNotFoundException e) {
            System.out.println("Cannot open the file!");
        }
        System.out.println("Test is done!");
    }
}
