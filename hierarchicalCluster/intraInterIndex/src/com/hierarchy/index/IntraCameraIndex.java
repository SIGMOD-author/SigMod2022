package com.hierarchy.index;
import com.telmomenezes.jfastemd.*;
import com.yahoo.labs.samoa.instances.Instance;
import com.yahoo.labs.samoa.instances.InstanceImpl;
import moa.clusterers.streamkm.StreamKMOMRk;
import moa.cluster.Clustering;
import moa.cluster.Cluster;
import moa.cluster.SphereCluster;
import com.jperch.*;
import java.util.List;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Set;
import java.util.Map;

public class IntraCameraIndex extends Index {
    protected String cameraId;
    protected String appId;
    protected Pnode previous;
    protected List<Feature> currentFeatureBuffer;
    protected List<Feature> outliersFeatureBuffer;
    protected StreamKMOMRk outlierClustering;
    protected Signature previousSignature;
    int firstOutlierIndex;
    long beginTimestamp;
    long maxStreamLength;
    long signatureCounter;

    public IntraCameraIndex(String appId, String cameraId, int maxStreamLength) {
        super();
        this.appId = appId;
        this.cameraId = cameraId;
        this.currentFeatureBuffer = new ArrayList<>();
        this.outliersFeatureBuffer = new ArrayList<>();
        this.previousSignature = null;
        this.firstOutlierIndex = -1;
        this.maxStreamLength = maxStreamLength;
        this.beginTimestamp = System.currentTimeMillis(); 
        outlierClustering = new StreamKMOMRk(3, 600);
        signatureCounter = 0;
    }

    /* Index Insertion pipeine */
    // Insert a data frame into current signature buffer
    // Input: the feature f
    public List<List<Signature>> insertFeature(FeatureND f) {
        List<List<Signature>> res = new LinkedList<>();
        // add f into current buffer
        currentFeatureBuffer.add(f);
        // determine whether f is an outliers
        if (previousSignature != null && isOutlier(f)) {
            if (firstOutlierIndex == -1) firstOutlierIndex = currentFeatureBuffer.size() - 1;
            // if f is an outlier, add it to outliers buffer
            outliersFeatureBuffer.add(f);
            Instance inst = new InstanceImpl(1, f.getVector());
            outlierClustering.insertInstance(inst);
            // determine whether there is a model drift
            if (isDrift()) {
                // insert the signature
                Signature[] sigs = divideCurrentBuffer();
                for (Signature s : sigs) {
                    insert(s);
                }
                // clear the buffers, reset the timestamp
                currentFeatureBuffer.clear();
                outliersFeatureBuffer.clear();
                outlierClustering = new StreamKMOMRk(3, 600);
                firstOutlierIndex = -1;
                beginTimestamp = System.currentTimeMillis(); 
                // update the intraIndex
                List<List<Signature>> tmp = updateIndex(sigs[1]);
                // res.get(0) add 
                // res.get(1) remove
                for (int i = 0; i < 2; i++) {
                    res.add(new LinkedList<>(tmp.get(i)));
                }
            }
        }
        // if the current feature buffer's lifespan is 
        // larger than a user-defined threshold
        if (res.size() == 0 && 
                System.currentTimeMillis() - beginTimestamp > maxStreamLength) {
            Signature s = convertCurrentBuffer(0, currentFeatureBuffer.size());
            insert(s);
            // clear the buffers, reset the timestamp
            currentFeatureBuffer.clear();
            outliersFeatureBuffer.clear();
            outlierClustering = new StreamKMOMRk(3, 600);
            firstOutlierIndex = -1;
            beginTimestamp = System.currentTimeMillis(); 
            // update the intraIndex
            List<List<Signature>> tmp = updateIndex(s);
            // res.get(0) add 
            // res.get(1) remove
            for (int i = 0; i < 2; i++) {
                res.add(new LinkedList<>(tmp.get(i)));
            }
        } 
        return res;
    }

    // Determine whether feature f is an outlier.
    private boolean isOutlier(FeatureND f) {
        if (previousSignature.withinBoundary(f)) {
            return false;
        }
        return true;
    }

    // Determine whether there is a model drift
    private boolean isDrift() {
        if (outliersFeatureBuffer.size() < 100) return false;
        Clustering res = outlierClustering.getClusteringResult(2, 5);
        double[] preMeans = previousSignature.getRadius();
        double meanDistance = 0;
        double counter = 0;
        for (double means : preMeans) {
            if (means < 100000) {
                meanDistance += means;
                counter++;
            }
        }
        meanDistance = meanDistance/counter;
        for (int i = 0; i < res.size(); i++) {
            SphereCluster sc = (SphereCluster) res.get(i);
            if (sc.getRadius() <= meanDistance) return true;
        }
        return false;
    }

    // Divide current buffer into two signatures
    // based on the first outlier feature
    private Signature[] divideCurrentBuffer() {
        Signature[] res = new Signature[2];
        res[0] = convertCurrentBuffer(0, firstOutlierIndex);
        res[1] = convertCurrentBuffer(firstOutlierIndex, 
                currentFeatureBuffer.size() - firstOutlierIndex);
        return res;
    }

    // Convert the features in current buffer to a signature
    private Signature convertCurrentBuffer(int beginIndex, int size) {
        Signature res = new Signature();
        Feature[] features = new FeatureND[size];
        double[] weights = new double[size];
        for (int i = beginIndex; i < beginIndex + size; i++) {
            features[i] = currentFeatureBuffer.get(i);
            weights[i] = 1;
        }
        String label = cameraId + "-" + signatureCounter;
        signatureCounter++;
        res.setNumberOfFeatures(size);
        res.setFeatures(features);
        res.setWeights(weights);
        res.setLabel(label);
        return res;
    }

    // Update the representatives of the index 
    // send the delta to update the inter-index
    private List<List<Signature>> updateIndex(Signature s) {
        List<Pnode> res = null;
        double maxSC = -1;
        for (int i = 2; i < 15; i++) {
            List<Pnode> tmp = tree.cluster(i);
            if (tree.sv > maxSC) {
                res = tmp;
                maxSC = tree.sv;
            }
        } 
        Map<Integer, List<Pnode>> deltas = tree.updateClusters(res);
        // Update the previous signature
        previousSignature = tree.getRepresentativeOfSignature(s);
        // Update the inter index
        List<Signature> add = new LinkedList<>();
        List<Signature> remove = new LinkedList<>();
        for (Pnode p : deltas.get(1)) {
            add.add(p.getRepresentative());
        }
        for (Pnode p : deltas.get(3)) {
            add.add(p.getRepresentative());
        }
        for (Pnode p : deltas.get(2)) {
            remove.add(p.getRepresentative());
        }

        List<List<Signature>> resList = new LinkedList<>();
        resList.add(add);
        resList.add(remove);
        return resList;
    }

    /* Index query pipeline */
    // Query the specific cluster that contains the signature labels in list
    // Input: Query feature, list of signature labels 
    //        that correspond to different clusters
    public List<Signature> directQuery(FeatureND query, Set<String> labels) {
        List<Signature> res = new LinkedList<>();
        for (String label : labels) {
            Pnode p = tree.signatureLabelToClusterMap.get(label);
            if (p == null) continue;
            double[] boundarys = p.getRepresentative().getBoundarys();
            double minBoundary = Double.MAX_VALUE;
            for (double boundary : boundarys) {
                if (boundary < minBoundary) minBoundary = boundary;
            }
            for (Signature s : p.sigs) {
                if (s.withinBoundary(query, minBoundary)) {
                    res.add(s);
                }
            }
        }
        return res;
    }

    public List<Signature> clusterQuery(String label) {
        List<Signature> res = new LinkedList<>();
        Pnode p = tree.signatureLabelToClusterMap.get(label);
        if (p == null) return res;
        for (Signature s : p.sigs) {
            res.add(s);
        }
        return res;
    }
}
