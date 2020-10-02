package com.hierarchy.index;
import com.telmomenezes.jfastemd.*;
import com.yahoo.labs.samoa.instances.Instance;
import com.jperch.*;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;

public class InterCameraIndex extends Index {
    protected String appId;
    protected Map<String, Signature> database;
    protected Map<Signature, String> signatureToCameraID;

    public InterCameraIndex(String appId) {
        super();
        this.appId = appId;
        database = new HashMap<>();
        signatureToCameraID = new HashMap<>();
    }
    
    /* Index insertion pipeline */
    // Update the inter-index based on delta signature sets
    public void updateIndex(List<List<Signature>> deltas, String cameraId) {
        List<Signature> add = deltas.get(0);
        List<Signature> remove = deltas.get(1);
        for (Signature s : add) {
            if (database.containsKey(s.getLabel())) {
                remove(database.get(s.getLabel()));
            }
            insert(s);
            database.put(s.getLabel(), s);
            signatureToCameraID.put(s, cameraId);
        }
        for (Signature s : remove) {
            remove(database.get(s.getLabel()));
            database.remove(s.getLabel());
        }
        update();
    }

    // Update the representatives of the index
    private void update() {
        List<Pnode> res = null;
        double maxSC = -1;
        for (int i = 2; i < 15; i++) {
            List<Pnode> tmp = tree.cluster(i);
            if (tree.sv > maxSC) {
                res = tmp;
                maxSC = tree.sv;
            }
        } 
        tree.updateClusters(res);
    }
    
    /* Index query pipeline */
    // Query the two-level index
    // Input: Query's feature
    // Output: a map of cameraid and related signatures' labels
    public Map<String, Set<String>> directQuery(FeatureND query) {
        List<Pnode> candidates = new ArrayList<>();
        Map<String, Set<String>> dispatchMap = new HashMap<>();
        for (Pnode p : tree.clusters) {
            // check whether the query is in the node's decision 
            // boundary or not
            if (p.withinBoundary(query)) {
                double[] boundarys = p.getRepresentative().getBoundarys();
                double boundaryThreshold = Double.MAX_VALUE;
                for (double boundary : boundarys) {
                    boundaryThreshold = Math.min(boundary, boundaryThreshold);
                }
                for (Signature s : p.sigs) {
                    if (s.withinBoundary(query, boundaryThreshold)) {
                        String cameraId = signatureToCameraID.get(s);
                        if (!dispatchMap.containsKey(cameraId)) {
                            dispatchMap.put(cameraId, new HashSet<>());
                        }
                        dispatchMap.get(cameraId).add(s.getLabel());
                    }
                }
            }
        }
        return dispatchMap;
    }

    public List<String> clusterQuery(Signature targetSignature) {
        List<String> res = new LinkedList<>();
        Signature nbr = tree.pruneNbrSearch(targetSignature);
        if (nbr == null) return res;
        String cameraId = signatureToCameraID.get(nbr); 
        res.add(cameraId);
        res.add(nbr.getLabel());
        return res;
    }
}
