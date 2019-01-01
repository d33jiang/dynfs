package dynfs.core;

import java.io.IOException;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Deque;
import java.util.LinkedList;
import java.util.Map;

import org.apache.commons.lang3.NotImplementedException;

import dynfs.core.options.AccessModes;
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

    //
    // Field: Parent

    private final DynDirectory<Space, ?> parent;

    public final DynDirectory<Space, ?> getParent() {
        return parent;
    }

    //
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
    // Interface: Attributes

    // I/O: File Attributes, Read by Subclass of BasicFileAttributes
    @SuppressWarnings("unchecked")
    public final <A extends BasicFileAttributes> A readAttributes(Class<A> type)
            throws IOException {
        if (!type.isAssignableFrom(DynAttributes.class))
            throw new UnsupportedOperationException("Attributes of the given type are not supported");

        return (A) attributes();
    }

    // I/O: File Attributes, Read by String [Names of Attribute Sets]
    public Map<String, Object> readAttributes(String attributes)
            throws IOException {
        return attributes().readAttributes(attributes);
    }

    // I/O: File Attributes, Set
    public void setAttribute(String attribute, Object value) throws IOException {
        attributes().setAttribute(attribute, value);
    }

    //
    // Implementation: Access Control

    public final void checkAccess(AccessModes modes) {
        throw new NotImplementedException("Access control is not yet implemented");
    }

    //
    // Implementation: Absolute Route

    private final void getRoute(Deque<String> pathNames) {
        if (parent != null) {
            ((DynNode<Space, ?>) parent).getRoute(pathNames);
        }

        if (getName() != null) {
            pathNames.add(getName());
        }
    }

    public final DynRoute getRoute() {
        LinkedList<String> pathNames = new LinkedList<>();
        getRoute(pathNames);
        return DynRoute.fromPathNameList(true, pathNames);
    }

    //
    // Implementation: Path String

    public final String getPathString() {
        return getRoute().toString();
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

    //
    // Implementation: Hidden (Abstract)

    public boolean isHidden() {
        return getName().startsWith(DynRoute.PREFIX_HIDDEN);
    }

    //
    // Interface: Equals (Abstract)

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof DynNode))
            return false;
        if (store != ((DynNode<?, ?>) other).store)
            return false;

        @SuppressWarnings("unchecked")
        DynNode<Space, ?> otherNode = (DynNode<Space, ?>) other;

        return equalsNode(otherNode);
    }

    public abstract boolean equalsNode(DynNode<Space, ?> other);

    //
    // Interface: DynSpaceIO

    // TODO: Extend interface

    public void delete() throws IOException {
        store.getIOInterface().delete(this);
    }

}
