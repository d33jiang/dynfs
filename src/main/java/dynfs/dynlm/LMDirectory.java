package dynfs.dynlm;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import dynfs.core.DynDirectory;
import dynfs.core.DynNode;

public class LMDirectory extends DynDirectory<LMSpace, LMDirectory> {

    //
    // Field: Children

    private final Map<String, DynNode<LMSpace, ?>> children = new HashMap<>();

    // TODO: Hack
    public Map<String, DynNode<LMSpace, ?>> children() {
        return children;
    }

    //
    // Construction

    // Root Directory
    protected LMDirectory(LMSpace store) {
        super(store);
    }

    // Non-Root Directory
    protected LMDirectory(LMSpace store, LMDirectory parent, String name) {
        super(store, parent, name);
    }

    //
    // Implementation: Iterable<DynNode<>>

    @Override
    public Iterator<DynNode<LMSpace, ?>> iterator() {
        return children.values().iterator();
    }

    //
    // Implementation: Child Resolution

    @Override
    protected DynNode<LMSpace, ?> resolveChild(String name) throws IOException {
        return children.get(name);
    }

    //
    // Debug: Tree Dump

    public TreeDump getTreeDump() {
        return new TreeDump(this);
    }

    public static final class TreeDump {
        private final LMDirectory root;
        private String lastDump;

        private TreeDump(LMDirectory root) {
            this.root = root;
            this.lastDump = null;
        }

        public TreeDump build() {
            StringBuilder sb = new StringBuilder();

            dump(sb, 0, root);

            sb.deleteCharAt(sb.length() - 1);
            lastDump = sb.toString();

            return this;
        }

        private void dump(StringBuilder sb, int newDepth, DynNode<LMSpace, ?> newRoot) {
            sb.append(newRoot.getPathString());
            sb.append('\n');
            if (newRoot instanceof LMDirectory) {
                LMDirectory dir = (LMDirectory) newRoot;
                for (DynNode<LMSpace, ?> child : dir) {
                    dump(sb, newDepth + 1, child);
                }
            }
        }

        @Override
        public String toString() {
            return lastDump;
        }
    }

    //
    // Interface: Equals Node

    @Override
    public boolean equalsNode(DynNode<LMSpace, ?> other) {
        return this == other;
    }

}
