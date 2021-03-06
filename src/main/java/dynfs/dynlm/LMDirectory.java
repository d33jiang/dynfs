package dynfs.dynlm;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.attribute.FileAttribute;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.NotImplementedException;

import dynfs.core.DynDirectory;
import dynfs.core.DynFile;
import dynfs.core.DynNode;
import dynfs.core.DynNodeAttribute;
import dynfs.core.options.CopyOptions;

public class LMDirectory extends DynDirectory<LMSpace, LMDirectory> {

    //
    // State: Children

    private final Map<String, DynNode<LMSpace, ?>> children = new HashMap<>();

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
    // Interface Implementation: DynNode Size

    @Override
    public long readSize() {
        return 0;
    }

    //
    // Implementation: I/O, Node Equality Check

    @Override
    protected boolean isSameFile(DynNode<LMSpace, ?> other) {
        return this == other;
    }

    //
    // Implementation: I/O, Node Deletion

    @Override
    protected void preDeleteImpl() throws IOException {
        if (!isEmpty())
            throw new DirectoryNotEmptyException(getRouteString());
    }

    @Override
    protected void deleteImpl() throws IOException {
        // No-op.
    }

    //
    // Implementation: I/O, Child Operations

    @Override
    protected DynFile<LMSpace, ?> createFileImpl(String name, FileAttribute<?>... attrs) throws IOException {
        LMFile file = new LMFile(getStore(), this, name);
        children.put(name, file);
        return file;
    }

    @Override
    protected DynDirectory<LMSpace, ?> createDirectoryImpl(String name, FileAttribute<?>... attrs) throws IOException {
        LMDirectory file = new LMDirectory(getStore(), this, name);
        children.put(name, file);
        return file;
    }

    @Override
    protected void deleteChildImpl(String name, DynNode<LMSpace, ?> node) throws IOException {
        if (!children.remove(name, node))
            throw new FileNotFoundException(node.getRouteString());
    }

    @Override
    protected void copyImpl(DynNode<LMSpace, ?> srcNode, String dstName, CopyOptions copyOptions, boolean deleteSrc)
            throws IOException {
        // TODO: IntraSystem Copy - Implementation
        throw new NotImplementedException("Method stub");
    }

    @Override
    protected void copySimpleImpl(DynNode<LMSpace, ?> srcNode, String dstName) throws IOException {
        // TODO: IntraSystem Copy - Test default implementation.
        // No-op.
    }

    //
    // Implementation: Iterable<DynNode>

    @Override
    public Iterator<DynNode<LMSpace, ?>> iterator() {
        return children.values().iterator();
    }

    //
    // Implementation: Child Resolution

    @Override
    protected DynNode<LMSpace, ?> resolveChildImpl(String name) throws IOException {
        return children.get(name);
    }

    //
    // Implementation: Attribute I/O

    @Override
    protected Map<DynNodeAttribute, Object> readAttributesImpl(Set<DynNodeAttribute> keys) throws IOException {
        // TODO: Attribute I/O - Implementation
        throw new NotImplementedException("DynFile Attributes are not yet supported by LMSpace");
    }

    @Override
    protected Map<DynNodeAttribute, Object> readAllAttributes() throws IOException {
        // TODO: Attribute I/O - Implementation
        throw new NotImplementedException("DynFile Attributes are not yet supported by LMSpace");
    }

    @Override
    protected Map<String, Object> writeAttributesImpl(Map<String, ?> newMappings) throws IOException {
        // TODO: Attribute I/O - Implementation
        throw new NotImplementedException("DynFile Attributes are not yet supported by LMSpace");
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
            sb.append(newRoot.getRouteString());
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

}
