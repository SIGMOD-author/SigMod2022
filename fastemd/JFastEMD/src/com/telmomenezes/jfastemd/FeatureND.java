package com.telmomenezes.jfastemd;

import java.util.List;
import java.util.ArrayList;

/**
 * @author author
 *
 */
public class FeatureND implements Feature {
    private List<Double> vector;
    private String label;

    public FeatureND(List<Double> vector) {
        this.vector = vector;
        this.label = "U";
    }

    public FeatureND(double[] vals) {
        this.vector = new ArrayList<>();
        for (double val : vals) {
            this.vector.add(val);
        }
    }

    public void setLabel(String label) {
        this.label = label;
    }
    
    public double groundDist(Feature f) {
        FeatureND fnd = (FeatureND)f;
        assert(fnd.vector.size() == this.vector.size());
        double sum = 0;
        for (int i = 0; i < fnd.vector.size(); i++) {
            double delta = fnd.vector.get(i) - this.vector.get(i);
            sum += delta * delta;
        }
        return Math.sqrt(sum);
    }

    public double[] getVector() {
        double[] res = new double[this.vector.size()];
        for (int i = 0; i < res.length; i++) {
            res[i] = vector.get(i);
        }
        return res;
    }

    public String getLabel() {
        return label;
    }
}
