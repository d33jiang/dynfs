package dynfs.core.file;

import dynfs.core.DynSpace;
import dynfs.core.path.DynRoute;

public abstract class DynNode<Space extends DynSpace<Space>, Node extends DynNode<Space, Node>> {

    //
    // Helper: Name Validation

    protected static void validateName(String name) {
        validateName(name, "Node name");
    }

    protected static void validateName(String name, String lblName) {
        if (name == null)
            throw new NullPointerException(lblName + " cannot be null");
        if (name.isEmpty())
            throw new IllegalArgumentException(lblName + " must be non-empty");
        if (name.contains("/"))
            throw new IllegalArgumentException(lblName + " cannot contain '/'");
    }

    //
    // Field: Store

    private final Space store;

    public final Space getStore() {
        return store;
    }

    // Field: Parent

    private final DynDirectory<Space, ?> parent;

    public final DynDirectory<Space, ?> getParent() {
        return parent;
    }

    // Field: Name

    private final String name;

    public final String getName() {
        return name;
    }

    //
    // Field: Attributes

    private final DynAttributes<Space, Node> attributes;

    public final DynAttributes<Space, Node> attributes() {
        return attributes;
    }

    //
    // Implementation: Path String

    private final void buildPathString(StringBuilder sb) {
        if (parent != null) {
            ((DynNode<Space, ?>) parent).buildPathString(sb);
        }

        if (getName() != null) {
            sb.append(getName());
        }

        if (isDirectory()) {
            sb.append(DynRoute.PATH_SEPARATOR);
        }
    }

    public final String getPathString() {
        StringBuilder sb = new StringBuilder();
        buildPathString(sb);
        return sb.toString();
    }

    //
    // Construction

    <DirNode extends DynDirectory<Space, DirNode>> DynNode(Space store, DirNode parent, String name) {
        this.store = store;
        this.parent = parent;
        this.name = name;

        this.attributes = new DynAttributes<Space, Node>(this);
    }

    //
    // Implementation: Attributes (Abstract)

    public boolean isRegularFile() {
        return false;
    }

    public boolean isDirectory() {
        return false;
    }

    public boolean isSymbolicLink() {
        return false;
    }

    public boolean isOther() {
        return false;
    }

    //
    // Implementation Stub: Size (Abstract)

    public abstract long size();

    //
    // Implementation: FileKey (Abstract)

    public Object fileKey() {
        return null;
    }

}
