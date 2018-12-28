package dynfs.dynlm;

import java.util.HashMap;
import java.util.Map;

import dynfs.core.file.DynNode;

public class LMDirectory extends LMNode {

    //
    // Field: Children

    private final Map<String, DynNode<Space, ?>> children = new HashMap<>();

    //
    // Construction: Root Directories

    static LMDirectory createRootDirectory(LMSpace store) {
        return new LMDirectory(store);
    }

    private LMDirectory(LMSpace store) {
        super(store, null, null, true);
    }

    //
    // Regular Directories

    LMDirectory(LMNode parent, String name) {
        super(parent.getStore(), parent, name, true);
        validateName(name);
    }

    @Override
    public boolean isRegularFile() {
        return false;
    }

    @Override
    public boolean isDirectory() {
        return true;
    }

    @Override
    public boolean isSymbolicLink() {
        return false;
    }

    @Override
    public boolean isOther() {
        return false;
    }

    @Override
    public long size() {
        // NOTE: Naive recursive definition
        long s = 0;

        for (LMNode n : this) {
            s += n.size();
        }

        return s;
    }

}
