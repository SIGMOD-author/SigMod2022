package com.jperch;
import java.util.*;
import com.telmomenezes.jfastemd.*;

public class Ptree {
    Pnode root;
    Map<Signature, Map<Signature, Double>> dist;
    Set<Signature> sigs;
    public double sv;
    public Set<Pnode> clusters;
    public Map<String, Pnode> signatureLabelToClusterMap;
    // Pruning-based search
    private int emdCountWithoutPrune;
    private int emdCountWithPrune;
    private int emdCountMasking;
    Map<Signature, Map<Signature, Double>> realDist;

    class Tuple {
        Signature s;
        double dist;
        public Tuple(Signature s, double dist) {
            this.s = s;
            this.dist = dist;
        }
    }

    public Ptree() {
        root = new Pnode();
        dist = new HashMap<>();
        realDist = new HashMap<>();
        sigs = new HashSet<>();
        clusters = new HashSet<>();
        signatureLabelToClusterMap = new HashMap<>();
        sv = 0;
        emdCountWithPrune = 0;
        emdCountWithoutPrune = 0;
        emdCountMasking = 0;
    }

    public Signature getRepresentativeOfSignature(Signature s) {
        for (Pnode p : clusters) {
            if (p.sigs.contains(s)) {
                return p.representative;
            }
        }
        return null;
    }

    // Update the clusters
    // Input: new list of representative Pnode
    // Output: Map<mode, pnodes>
    public Map<Integer, List<Pnode>> updateClusters(List<Pnode> list) {
        Map<Integer, List<Pnode>> res = new HashMap<>();
        /* 
         * Mode = 0: unchange
         * Mode = 1: change but exist previous
         * Mode = 2: newly removed
         * Mode = 3: newly added
         * */
        for (int i = 0; i < 4; i++) {
            res.put(i, new LinkedList<>());
        }
        // initialization
        if (clusters.size() == 0) {
            for (Pnode p : list) {
                p.representativeFinding();
                signatureLabelToClusterMap.put(p.representative.getLabel(), p);
                clusters.add(p);
            }
            res.put(3, list); 
            return res;
        } 
        Set<Pnode> newClusters = new HashSet<>();
        newClusters.addAll(list);
        for (Pnode p : clusters) {
            int mode = -1;
            if (newClusters.contains(p) && p.addSigs.isEmpty() && p.removal == false) {
                mode = 0;
            } else if (newClusters.contains(p) && (p.removal == true || !p.addSigs.isEmpty())) {
                mode = 1;
            } else if (!newClusters.contains(p)) {
                mode = 2;
            }
            List<Pnode> tmp = res.get(mode);
            tmp.add(p);
        }
        signatureLabelToClusterMap.clear();
        for (Pnode p : newClusters) {
            if (!clusters.contains(p)) {
                List<Pnode> tmp = res.get(3);
                tmp.add(p);
            }
            p.representativeFinding();
            signatureLabelToClusterMap.put(p.representative.getLabel(), p);
        }
        clusters = newClusters;
        return res;
    }
    

    /* Update distance map and return the nearest Signature */
    public Signature updateDist(Signature x) {
        dist.put(x, new HashMap<>());
        realDist.put(x, new HashMap<>());
        Map<Signature, Double> hm = dist.get(x);
        Signature nearest = null;
        double min = Double.MAX_VALUE;
        for (Signature s : sigs) {
            double d = JFastEMD.distance(s, x, 0, 0.7);
            emdCountWithoutPrune++;
            //System.out.println(d);
            Map<Signature, Double> tmp = dist.get(s);
            tmp.put(x, d);
            dist.put(s, tmp);
            hm.put(s, d);
            if (d < min) {
                min = d;
                nearest = s;
            }
        }
        Signature nbr = pruneNbrSearch(x);
        assert(nbr == nearest);
        if (nearest != null) {
            assert(dist.get(x).get(nearest) == dist.get(x).get(nbr));
        }
        dist.put(x, hm);
        sigs.add(x);
        return nearest;
    }

    public Signature pruneNbrSearch(Signature x) {
        realDist.put(x, new HashMap<>());
        Set<Signature> visited = new HashSet<>();
        PriorityQueue<Tuple> pq = new PriorityQueue<Tuple>((m, n) -> m.dist < n.dist ? -1 : 1);
        for (Signature s : sigs) {
            double ocd = JFastEMD.calECD(s, x);
            Tuple t = new Tuple(s, ocd);
            pq.add(t);
        }
        Signature cur = null;
        while (!pq.isEmpty() && !visited.contains(pq.peek().s)) {
            Tuple t = pq.poll();
            double omd = JFastEMD.distance(t.s, x, 0, 0.7);
            realDist.get(x).put(t.s, omd);
            realDist.get(t.s).put(x, omd);
            t.dist = omd;
            pq.add(t);
            visited.add(t.s);
            emdCountWithPrune++;
        }
        return pq.isEmpty() ? null : pq.peek().s;
    }

    private void updateRealDist(Signature s1, Signature s2, double d) {
        if (!realDist.get(s1).containsKey(s2)) {
            realDist.get(s1).put(s2, d);
            realDist.get(s2).put(s1, d);
            emdCountMasking++;
        }
    }

    /* Compute the minimum/maximium distance between the leaves node of  
     * Pnode x and Pnode y
     * Input:  Pnode x and Pnode y
     * Output: minimum/maximum distance
     * */
    private double minDistance(Pnode x, Pnode y) {
        double min = Double.MAX_VALUE;
        for (Signature s1 : x.sigs) {
            for (Signature s2 : y.sigs)
            {
                double d = dist.get(s1).get(s2);
                updateRealDist(s1, s2, d);
                if (d < min) min = d;
            }
        }
        return min;
    } 

    private double maxDistance(Pnode x, Pnode y) {
        double max = Double.MIN_VALUE;
        for (Signature s1 : x.sigs) {
            for (Signature s2 : y.sigs) {
                double d = dist.get(s1).get(s2);
                updateRealDist(s1, s2, d);
                if (d > max) max = d;
            }
        }
        return max;
    }

    public void setDist(Map<Signature, Map<Signature, Double>> dist) {
        this.dist = dist;
    }

    /* Insert a new Signature into the tree
     * Input:  the new Signature s
     */
    public void insert(Signature s) {
        Signature nearest = updateDist(s);
        if(root.sigs.size() == 0) {
            root.addSignature(s);
        } else {
            // creat new node and insert it next to the nearest neighbor
            Pnode cur = root.exactMatch(nearest);
            Pnode newLeaf = splitDown(s, cur);
            List<Pnode> ancs = newLeaf.parent.ancestors();
            for (Pnode a : ancs) {
                a.addSignature(s);
            }
            updateInsertCostRecursively(newLeaf.parent.parent, newLeaf.parent, s);

            // rotate based on masking and balance
            recursiveRotateIfMasked(newLeaf.siblings().get(0));
            recursiveRotateIfUnbalanced(newLeaf.siblings().get(0));
            root = newLeaf.root();
        }
    }

    /* Insert a new Signature into the tree
     * Input:  the new Signature s
     */
    public void insert(Signature s, Signature nearest) {
        if(root.sigs.size() == 0) {
            root.addSignature(s);
            int tmp = emdCountWithPrune + emdCountMasking;
            System.out.println("Without: " + emdCountWithoutPrune 
                             + " With: " + emdCountWithPrune 
                             + " Masking: " + emdCountMasking
                             + " Total: " + tmp);
        } else {
            // creat new node and insert it next to the nearest neighbor
            Pnode cur = root.exactMatch(nearest);
            Pnode newLeaf = splitDown(s, cur);
            List<Pnode> ancs = newLeaf.parent.ancestors();
            for (Pnode a : ancs) {
                a.addSignature(s);
            }
            updateInsertCostRecursively(newLeaf.parent.parent, newLeaf.parent, s);

            // rotate based on masking and balance
            recursiveRotateIfMasked(newLeaf.siblings().get(0));
            recursiveRotateIfUnbalanced(newLeaf.siblings().get(0));
            root = newLeaf.root();
            int tmp = emdCountWithPrune + emdCountMasking;
            System.out.println("Without: " + emdCountWithoutPrune 
                             + " With: " + emdCountWithPrune 
                             + " Masking: " + emdCountMasking
                             + " Total: " + tmp);
        }
    }

    /* Remove an existing Signature in the tree
     * Input: Signature x that needs to be removed
     */
    public void remove(Signature s) {
        Pnode cur = root.exactMatch(s);
        if (cur == root) {
            // if there is only one signature in the tree
            // just remove the signature stored in the root node
            root.removeSignature(s);
        } else {
            // remove the current node and make its parent be a leaf node
            Pnode preInternal = deleteNode(cur);
            List<Pnode> ancs = preInternal.ancestors();
            for (Pnode a : ancs) {
                a.removeSignature(s);
            }
            updateRemoveCostRecursively(preInternal.parent);

            // rotate based on balance
            // no need to check masking, as per definition
            // remove a node will not cause masking problem
            recursiveRotateIfUnbalanced(preInternal);
            root = preInternal.root();
        }
    }

    /* Create a new node for Signature x as the sibling of node p
     * Input:  Signature x, Pnode p
     * Output: a pointer to the new node containing x
     */
    private Pnode splitDown(Signature x, Pnode p) {
        Pnode newInternal = new Pnode();
        newInternal.sigs.addAll(p.sigs);
        newInternal.signatureCounter = p.signatureCounter;

        if(p.parent != null) {
            p.parent.addChild(newInternal);
            p.parent.children.remove(p);
            newInternal.addChild(p);
        } else {
            newInternal.addChild(p);
        }
        
        Pnode newLeaf = new Pnode();
        newLeaf.addSignature(x);
        newInternal.addChild(newLeaf);
        newInternal.addSignature(x);
        for (Signature s1 : p.sigs) {
            newInternal.cost = Math.max(dist.get(s1).get(x), newInternal.cost);
            updateRealDist(s1, x, dist.get(s1).get(x));
        }
        return newLeaf;
    }

    /* Delete current node and merge its parent and sibling node 
     * Input: the node that need to be deleted
     * Output: its parent node pointer
     */
    private Pnode deleteNode(Pnode p) {
        Pnode sibling = p.siblings().get(0);
        Pnode preInternal = p.parent;
        p.parent = null;
        sibling.parent = null;
        preInternal.children.clear();

        if (preInternal.parent != null) {
            preInternal.parent.children.remove(preInternal);
            preInternal.parent.addChild(sibling);
        } else {
            root = sibling;
        }
        return sibling;
    }

    /* Update a node's cost recursively because of node insertion 
     * Input: node p, p's child that actually change cost, newly added signature s
     */
    private void updateInsertCostRecursively(Pnode p, Pnode changeChild, Signature s) {
        if (p == null) return;
        Pnode cur = p;
        Pnode cc = changeChild;
        double preCost = cur.cost;
        updateInsertCost(cur, cc, s);
        if (preCost == cur.cost) return;
        while (cur.parent != null) {
            cc = cur;
            cur = cur.parent;
            preCost = cur.cost;
            updateInsertCost(cur, cc, s);
            if (preCost == cur.cost) return;
        }
    }

    /* Update node cur's cost because of insertion
     * Triggered by the cost update of one of its children Pnode changeChild
     * the newly inserted signature is delta
     * Input: Pnode cur, Pnode changeChild, Signature delta
     */ 
    private void updateInsertCost(Pnode cur, Pnode changeChild, Signature delta) {
        Pnode sibling = changeChild.siblings().get(0);
        cur.cost = Math.max(cur.cost, changeChild.cost);
        double preCost = cur.cost; 
        for (Signature s : sibling.sigs) {
            cur.cost = Math.max(cur.cost, dist.get(s).get(delta));
            updateRealDist(s, delta, dist.get(s).get(delta));
        }
        //System.out.println("once!");
        //if (preCost != changeChild.cost && preCost != cur.cost) System.out.println("ab!");
    }

    /* Update a node's cost recursively because of node removal
     * Input: node p
     */
    private void updateRemoveCostRecursively(Pnode p) {
        if (p == null) return;
        Pnode cur = p;
        updateRemoveCost(cur);
        while (cur.parent != null) {
            cur = cur.parent;
            updateRemoveCost(cur);
        }
    }

    /* Update a node's cost because of node removal
     * Input: Pnode p
     * TODO: current version is compuatation inefficient O(n^2lgn)
     *       need further optimization
     */
    private void updateRemoveCost(Pnode p) {
        p.cost = cost(p);
    }

    /* Swaps the position of Pnode x and x's aunt in the tree */
    private void rotate(Pnode x) {
        Pnode aunt = x.aunts().get(0);
        Pnode sibling = x.siblings().get(0);
        Pnode grandParent = x.parent.parent;
        
        // Make the aunt and x have the same parent
        Pnode newParent = new Pnode();
        newParent.addChild(aunt);
        newParent.addChild(x);
        newParent.sigs.addAll(aunt.sigs);
        newParent.sigs.addAll(x.sigs);
        newParent.signatureCounter = aunt.signatureCounter + x.signatureCounter;
        newParent.cost = Math.max(Math.max(aunt.cost, x.cost), maxDistance(aunt, x));

        // Set the children of the granprent to be the newParent 
        // and x's sibling
        grandParent.children.clear();
        grandParent.addChild(newParent);
        grandParent.addChild(sibling);
    }

    /* Rotate from Pnode x recursively if masking detected */
    private void recursiveRotateIfMasked(Pnode x) {
        Pnode cur = x;
        boolean masked = true;
        while (cur != root && masked) {
            if (cur.parent != null && cur.parent.parent != null && isCloserToAunt(cur)) {
                rotate(cur);
            } else if (!cur.children.isEmpty()) {
                masked = false;
            }
            cur = cur.parent;
        }
    } 

    /* Rotate recursively from Pnode x if balance candidate detected */
    private void recursiveRotateIfUnbalanced(Pnode x) {
        Pnode cur = x;
        while (cur != root) {
            Pnode sibling = cur.siblings().get(0);
            Pnode rotate0 = cur;
            Pnode rotate1 = sibling;
            if (cur.signatureCounter > sibling.signatureCounter) {
                rotate0 = sibling;
                rotate1 = cur;
            }
            if (cur.parent != null && cur.parent.parent != null 
                    && canRotateForBalance(rotate0)) {
                rotate(rotate0);
                cur = rotate0.parent;
            } else if (cur.parent != null && cur.parent.parent != null 
                    && canRotateForBalance(rotate1)) {
                rotate(rotate1);
                cur = rotate1.parent;
            } else {
                cur = cur.parent;
            }
        }
    }

    /* Determine if self is closer to its aunt than its sibling */
    private boolean isCloserToAunt(Pnode x) {
        if (x.parent != null && x.parent.parent != null) {
            Pnode aunt = x.aunts().get(0);
            Pnode sibling = x.siblings().get(0);
            double auntMinDistance = minDistance(x, aunt);
            double siblingMaxDistance = maxDistance(x, sibling);
            return siblingMaxDistance > auntMinDistance;
        }
        return false;
    }

    /* Determine if Pnode x can be swapped to improve balance */
    protected boolean rotateWithoutMasking(Pnode x) {
        if (x.parent != null && x.parent.parent != null) {
            Pnode aunt = x.aunts().get(0);
            Pnode sibling = x.siblings().get(0);
            double siblingMaxDist = maxDistance(x, sibling);
            double auntMinDist = minDistance(x, aunt);
            return siblingMaxDist > auntMinDist;
        }
        return false;
    }

    /* Check if rotating Pnode x would produce better balance */
    protected boolean rotateImproveBalance(Pnode x) {
        if (x.parent != null && x.parent.parent != null) {
            double auntSize = x.aunts().get(0).signatureCounter;
            double parentSize = x.parent.signatureCounter;
            double selfSize = x.signatureCounter;
            double siblingSize = parentSize - selfSize;
            double newParentSize = selfSize + auntSize;
            double bal = Math.min(selfSize, siblingSize) / Math.max(selfSize, siblingSize)
                + Math.min(auntSize, parentSize) / Math.max(auntSize, parentSize);
            double balRot = Math.min(selfSize, auntSize) / Math.max(selfSize, auntSize) 
                + Math.min(siblingSize, newParentSize) 
                / Math.max(siblingSize, newParentSize);
            return bal < balRot;
        }
        return false;
    }

    /* Return true if Pnode is a balance candidate */ 
    protected boolean canRotateForBalance(Pnode x) {
        return rotateImproveBalance(x) && rotateWithoutMasking(x); 
    }

    /* Return the cost of Pnode x, defined as the maximum distance among the lvs */
    protected double cost(Pnode x) {
        List<Signature> tmp = new LinkedList<>(x.sigs);
        double maxDistance = 0;
        for (int i = 0; i < tmp.size(); i++) { 
            for (int j = i + 1; j < tmp.size(); j++) {
                double d = dist.get(tmp.get(i)).get(tmp.get(j));
                //updateRealDist(tmp.get(i), tmp.get(j), d);
                if (d > maxDistance) maxDistance = d;
            }
        }
        return maxDistance;
    }

    // Determine the number of clusters based on a criterion.
    protected double silhouetteValue(List<Pnode> list) {
        double sv = 0;
        for (Pnode p : list) {
            for (Signature s : p.sigs) {
                if (p.sigs.size() == 1) {
                    sv += 0;
                    continue;
                }
                double intra= 0;
                double inter = Double.MAX_VALUE;
                // calculate the average dissimilarity intra-cluster
                for (Signature other : p.sigs) {
                    if(other == s) continue;
                    intra += dist.get(s).get(other);
                    updateRealDist(s, other, dist.get(s).get(other));
                }
                intra = intra / (p.sigs.size() - 1);
                // calculate the average dissimlarity inter-cluster
                for (Pnode otherCluster : list) {
                    if (otherCluster == p) continue;
                    double d = 0;
                    for (Signature s2 : otherCluster.sigs) {
                        d += dist.get(s).get(s2);
                        updateRealDist(s, s2, dist.get(s).get(s2));
                    }
                    d = d / otherCluster.sigs.size();
                    if (d < inter) inter = d;
                }
                // calculate the silhouette of this data point
                // and add it to total silhouette value
                sv += (inter - intra) / Math.max(inter, intra);
            }
        }
        sv = sv / root.sigs.size();
        return sv;
    }

    /* Partition the tree into K clusters based on the cost of each node */
    public List<Pnode> cluster(int k) {
        // if the number of clusters is large or equal to
        // the total number of streams, simply return
        // all the streams, each one represents a unique cluster
        if (k >= root.sigs.size()) return root.leaves();
        PriorityQueue<Pnode> pq = new PriorityQueue<Pnode>(
                new Comparator<Pnode>() {
                    @Override
                    public int compare(Pnode p1, Pnode p2) {
                        if (p1.cost < p2.cost) return 1;
                        else return -1;
                    }
                });
        pq.add(root);
        while (pq.size() < k) {
            Pnode p = pq.poll();
            for (Pnode child : p.children) {
                pq.add(child);
            } 
        }
        this.sv = silhouetteValue(new LinkedList<Pnode>(pq)); 
        return new LinkedList<Pnode>(pq);
    }
}

