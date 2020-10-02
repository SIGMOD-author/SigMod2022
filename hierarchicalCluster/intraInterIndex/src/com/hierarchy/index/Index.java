package com.hierarchy.index;
import com.telmomenezes.jfastemd.*;
import com.yahoo.labs.samoa.instances.Instance;
import com.jperch.*;
import java.util.List;

public abstract class Index {
    protected Ptree tree = null;
    // Initialization 
    public Index() {
        tree = new Ptree();
    }
    // Insert a signature into the index
    // Input: the signature that need to be inserted
    public void insert(Signature s) {
        this.tree.insert(s);
    }
    // Remove a signature from the index
    // Input: the signature that need to be removed
    public void remove(Signature s) {
        this.tree.remove(s);
    }
}
