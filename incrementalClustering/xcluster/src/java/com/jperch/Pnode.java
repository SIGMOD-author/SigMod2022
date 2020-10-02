package com.jperch;
import java.util.*;
import com.telmomenezes.jfastemd.*;
import com.yahoo.labs.samoa.instances.Instance;
import com.yahoo.labs.samoa.instances.InstanceImpl;
import moa.clusterers.streamkm.StreamKMOMRk;
import moa.cluster.Clustering;
import moa.cluster.Cluster;
import moa.cluster.SphereCluster;

public class Pnode {
    String uniqueId;
    Set<Pnode> children;
    Pnode parent;
    public Set<Signature> sigs;
    Set<Signature> addSigs;
    boolean removal;
    int signatureCounter;
    double cost;
    StreamKMOMRk skm;
    Signature representative;

    public Pnode() {
        uniqueId = UUID.randomUUID().toString();
        children = new HashSet<>();
        parent = null;
        sigs = new HashSet<>();
        addSigs = new HashSet<>();
        removal = false;
        signatureCounter = 0;
        cost = 0;
        // initially, the streamKM clusterer and 
        // reprsentative signature are  null
        skm = null;
        representative = null;
    }

    public Signature getRepresentative() {
        return representative;
    }
 
    /* Return the repesentative signature 
     * of all the signatures in this node */
    protected Signature representativeFinding() {
        // if this is the first time this node serves as a cluster
        // or there are some signature removal before
        if (skm == null || removal) {
            skm = new StreamKMOMRk(10, 500);
            for (Signature s : sigs) {
                Feature[] fs = s.getFeatures();
                for (Feature f : fs) {
                    Instance inst = new InstanceImpl(1, f.getVector());
                    skm.insertInstance(inst);
                }
            }

            int start = 3;
            int end = (int) Math.sqrt(skm.getNumberInstance());
            Clustering res = skm.getClusteringResult(start, end);
            double maxSC = skm.sv;

            this.representative = new Signature(); 
            Feature[] resultFeatures = new Feature[res.size()];
            double[] weights = new double[res.size()];
            double totalW = 0;
            for (int i = 0; i < res.size(); i++) {
                Cluster cl = res.get(i);
                double[] fcs = cl.getCenter();
                List<Double> featureContent = new ArrayList<>();
                for (double ele : fcs) {
                    featureContent.add(ele);
                }
                resultFeatures[i] = new FeatureND(featureContent);
                weights[i] = cl.getWeight();
                totalW += weights[i];
            }
            this.representative.setNumberOfFeatures(res.size());
            this.representative.setFeatures(resultFeatures);
            for (int i = 0; i < res.size(); i++) {
                weights[i] = weights[i] / totalW;
            }
            this.representative.setWeights(weights);
        } else {            
            if (addSigs.size() == 0) {
                return this.representative;
            } else {
                for (Signature s : addSigs) {
                    Feature[] fs = s.getFeatures();
                    for (Feature f : fs) {
                        Instance inst = new InstanceImpl(1, f.getVector());
                        skm.insertInstance(inst);
                    }
                }
                int start = 3;
                int end = (int) Math.sqrt(skm.getNumberInstance());
                Clustering res = skm.getClusteringResult(start, end);
                double maxSC = skm.sv;
                this.representative = new Signature(); 
                Feature[] resultFeatures = new Feature[res.size()];
                double[] weights = new double[res.size()];
                double[] radius = new double[res.size()];
                double[] boundarys = new double[res.size()];
                double totalW = 0;
                for (int i = 0; i < res.size(); i++) {
                    SphereCluster cl = (SphereCluster)res.get(i);
                    double[] fcs = cl.getCenter();
                    List<Double> featureContent = new ArrayList<>();
                    for (double ele : fcs) {
                        featureContent.add(ele);
                    }
                    resultFeatures[i] = new FeatureND(featureContent);
                    weights[i] = cl.getWeight();
                    radius[i] = cl.getRadius();
                    boundarys[i] = cl.getBoundary();
                    totalW += weights[i];
                }
                this.representative.setNumberOfFeatures(res.size());
                this.representative.setFeatures(resultFeatures);
                this.representative.setRadius(radius);
                this.representative.setBoundarys(boundarys);
                for (int i = 0; i < res.size(); i++) {
                    weights[i] = weights[i] / totalW;
                }
                this.representative.setWeights(weights);
            }
        }
        removal = false;
        addSigs.clear();
        return this.representative;
    }  

    /* Determine whether feature f is in this node's 
     * decision boundary or not
     */ 
    public boolean withinBoundary(FeatureND f) {
        if (this.representative == null) {
            return false;
        }
        if (this.representative.withinBoundary(f)) return true;
        return false;
    }

    /* Update a node's parameter recursively */
    protected void updateParamsRecursively() {
    }

    /* Search for the leaf node that contains Signature x in the tree 
     * rooted as self
     * Input:  the Signature x
     * Output: Pnode that contains x
     */
    protected Pnode exactMatch(Signature x) {
        Pnode cur = this.root();
        while (cur.sigs.contains(x) && cur.children.size() != 0) {
            for (Pnode child : cur.children) {
                if (child.sigs.contains(x)) {
                    cur = child;
                    break;
                }
            }
        }
        return cur;
    }

    /* Add a Pnode as a child of this node
     * Input:  Pnode newChild
     */
    protected void addChild(Pnode newChild) {
        newChild.parent = this;
        this.children.add(newChild);
    }

    /* Add a Signature to this node */
    protected void addSignature(Signature x) {
        signatureCounter++;
        sigs.add(x);
        // if this node has been clustered before and there is removal
        // operation, add the item to added signature set
        if (skm != null && removal == false) {
            addSigs.add(x); 
        }
    }

    /* Remove a Signature to this node */ 
    protected void removeSignature(Signature x) {
        signatureCounter--;
        sigs.remove(x);
        removal = true;
        addSigs.clear(); 
    }

    /* Return all of the nodes wich have no children */
    protected List<Pnode> leaves() {
        List<Pnode> lvs = new LinkedList<>();
        Queue<Pnode> queue = new LinkedList<>();
        queue.add(this);
        while (!queue.isEmpty()) {
            Pnode p = queue.poll();
            if (p.children.size() != 0) {
                for (Pnode child : p.children) {
                    queue.add(child);
                }
            } else {
                lvs.add(p);
            }
        }
        return lvs;
    }

    /* Return a list of my siblings */
    protected List<Pnode> siblings() {
        List<Pnode> res = new LinkedList<>();
        if (this.parent != null) {
            for (Pnode child : this.parent.children) {
                if (child != this) {
                    res.add(child);
                }
            }
        }
        return res;
    }

    /* Return a list of my aunts */ 
    protected List<Pnode> aunts() {
        List<Pnode> aunts = new LinkedList<>();
        if (this.parent != null && this.parent.parent != null) {
            for (Pnode child : this.parent.parent.children) {
                if (child != this.parent) {
                    aunts.add(child);
                }
            }
        }
        return aunts;
    }

    /* Return all of this nodes ancestors in order to the root */
    protected List<Pnode> ancestors() {
        List<Pnode> ancs = new LinkedList<>();
        Pnode cur = this;
        while(cur.parent != null) {
            ancs.add(cur.parent);
            cur = cur.parent;
        }
        return ancs;
    }

    /* Return the number of all ancestors */ 
    protected int depth() {
        return 0;
    }

    /* Return the height of this node */
    protected int height() {
        return 0;
    }

    /* Return all descendants of the current node */
    protected List<Pnode> descendants() {
        return null;
    }

    /* Return the lowest common ancestor between this node and node x 
     * Input:  Pnode x
     * Output: the lowest common ancestor node
     */
    protected Pnode lca(Pnode x) {
        return null;
    }

    /* Return the root of the tree */
    protected Pnode root() {
        Pnode cur = this;
        while (cur.parent != null) {
            cur = cur.parent;
        }
        return cur;
    }

    /* Return true if self is a leaf */
    protected boolean isLeaf() {
        return false;
    }

    /* Return true if self is an internal node */
    protected boolean isInternal() {
        return false;
    }

} 
