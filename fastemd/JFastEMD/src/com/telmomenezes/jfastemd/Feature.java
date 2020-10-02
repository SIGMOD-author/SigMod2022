package com.telmomenezes.jfastemd;

public interface Feature {
    public double groundDist(Feature f);
    public double[] getVector();
    public String getLabel();
}
