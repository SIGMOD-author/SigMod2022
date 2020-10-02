package com.telmomenezes.jfastemd;


/**
 * @author Telmo Menezes (telmo@telmomenezes.com)
 *
 */
public class Signature {
    private int numberOfFeatures;
    private Feature[] features;
    private Feature centroid;
    private double[] weights;
    private double[] radius;
    private double[] boundarys;
    private String label;
    private String startFeatureLabel;
    private String endFeatureLabel;
    
    public Signature() {
        centroid = null;
    }

    public int getNumberOfFeatures() {
        return numberOfFeatures;
    }
    
    public void setNumberOfFeatures(int numberOfFeatures) {
        this.numberOfFeatures = numberOfFeatures;
    }

    public Feature[] getFeatures() {
        return features;
    }

    public Feature getCentroid() {
        if (centroid == null) {
            double[] resVec = new double[features[0].getVector().length];
            for (int i = 0; i < numberOfFeatures; i++) {
                double[] vector = features[i].getVector();
                for (int j = 0; j < resVec.length; j++) {
                    resVec[j] += weights[i] * vector[j];
                }
            }
            centroid = new FeatureND(resVec);
        }
        return centroid;
    }

    public void setFeatures(Feature[] features) {
        this.features = features;
        this.startFeatureLabel = features[0].getLabel();
        this.endFeatureLabel = features[features.length - 1].getLabel();
    }

    public double[] getWeights() {
        return weights;
    }

    public void setWeights(double[] weights) {
        this.weights = weights;
    }

    public void setLabel(String s) {
        this.label = s;
    }

    public String getLabel() {
        return this.label;
    }

    public void setRadius(double[] radius) {
        this.radius = radius;
    }

    public double[] getRadius() {
        return radius;
    }

    public void setBoundarys(double[] boundarys) {
        this.boundarys = boundarys;
    }

    public double[] getBoundarys() {
        return this.boundarys;
    }
    
    public String[] getStartEndLabels() {
        return new String[]{startFeatureLabel, endFeatureLabel}; 
    }

    // This is for the representative signature
    public boolean withinBoundary(Feature f) {
        for (int i = 0; i < features.length; i++) {
            if (features[i].groundDist(f) <= boundarys[i]) return true;
        }
        return false;
    }

    // This is for the leaf signature
    public boolean withinBoundary(Feature f, double threshold) {
        for (int i = 0; i < features.length; i++) {
            if (features[i].groundDist(f) <= threshold) return true;
        } 
        return false;
    }
}
