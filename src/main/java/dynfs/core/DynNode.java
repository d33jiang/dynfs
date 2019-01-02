package dynfs.core;

import java.io.IOException;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.spi.FileSystemProvider;
import java.util.Deque;
import java.util.LinkedList;
import java.util.Map;

import org.apache.commons.lang3.NotImplementedException;

import dynfs.core.options.AccessModes;
import dynfs.core.path.DynRoute;

public abstract class DynNode<Space extends DynSpace<Space>, Node extends DynNode<Space, Node>> {

    //
    // Configuration: Store

    private final Space store;

    public final Space getStore() {
        return store;
    }

    //
    // Configuration: Parent

    private final DynDirectory<Space, ?> parent;

    public final DynDirectory<Space, ?> getParent() {
        return parent;
    }

    //
    // Configuration: Name

    private final String name;

    public final String getName() {
        return name;
    }

    //
    // Construction

    <DirNode extends DynDirectory<Space, DirNode>> DynNode(Space store, DirNode parent, String name) {
        this.store = store;
        this.parent = parent;
        this.name = name;

        // TODO: See DynAttributes sections
        this.attributes = new DynAttributes<Space>(this);
    }

    //
    // Core Support: Equality Check

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof DynNode))
            return false;
        if (store != ((DynNode<?, ?>) other).store)
            return false;

        // TODO: Is this SuppressWarnings annotation removable?
        @SuppressWarnings("unchecked")
        DynNode<Space, ?> otherNode = (DynNode<Space, ?>) other;

        return isSameFile(otherNode);
    }

    //
    // Inheritable Static Support: Name Validation

    protected static void validateName(String name) {
        validateName("The node name", name);
    }

    protected static void validateName(String lblName, String name) {
        if (name == null)
            throw new NullPointerException(lblName + " cannot be null");
        // TODO: Should empty-string-named files be permissible?
        if (name.isEmpty())
            throw new IllegalArgumentException(lblName + " must be non-empty");
        if (name.contains("/"))
            throw new IllegalArgumentException(lblName + " cannot contain '/'");
    }

    //
    // Interface Implementation: Canonical Route

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
    // Interface: Route String

    public final String getRouteString() {
        return getRoute().toString();
    }

    //
    // Implementation Default: FileKey

    public Object fileKey() {
        return null;
    }

    //
    // Interface Implementation Stub: DynNode Size Attribute

    public abstract long size();

    //
    // Interface: DynAttributes, Read by Attribute Sets

    /**
     * @see FileSystemProvider#readAttributes(Path, String, LinkOption...)
     */
    public final Map<String, Object> readAttributes(String attributes)
            throws IOException {
        return attributes().readAttributes(attributes);
    }

    //
    // Implementation Stub: DynAttributes, Read

    /**
     * @see FileSystemProvider#readAttributes(Path, Class, LinkOption...)
     */
    // I/O: File Attributes, Read by Subclass of BasicFileAttributes
    public final <A extends BasicFileAttributes> A readAttributes(Class<A> type)
            throws IOException {
        if (!type.isAssignableFrom(DynAttributes.class))
            throw new UnsupportedOperationException("Attributes of the given type are not supported");

        DynAttributes<Space> attr = attributes();
        if (!type.isAssignableFrom(attr.getClass()))
            throw new UnsupportedOperationException("Attributes of the given type are not supported");
        A attrAsAnA = (A) attr;

        return (A) attributes();
    }

    //
    // Implementation Stub: DynAttributes, Write

    /**
     * @see FileSystemProvider#setAttribute(Path, String, Object, LinkOption...)
     */
    // I/O: File Attributes, Set
    public final void setAttribute(String attribute, Object value) throws IOException {
        // TODO: Check access control to write
        setAttributeImpl(attribute, value);
        // TODO: Delete this comment - attributes().setAttribute(attribute, value);
    }

    // TODO: Call extra attributes "SpecialAttributes"?
    protected abstract void setAttributeImpl(String attribute, Object value) throws IOException;

    //
    // Implementation Stub: DynAttributes

    // TODO: Lazy read of attributes
    private final DynAttributes<Space> attributes;

    // TODO: Change to protected readAttributesImpl
    // TODO: Add writeAttributesImpl?
    public final DynAttributes<Space> attributes() {
        return attributes;
    }

    // TODO: protected final setAttributes(...)
    // TODO: protected abstract setAttributesImpl(...)
    // TODO: For readAttributes(Class), if neither superclass nor subclass of
    // DynAttributes, then throw ClassCastException

    //
    // Implementation Default: DynNode Type Attributes, Cache on Load

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
    // Interface Implementation: DynNode Hidden Attribute

    public static final String HIDDEN_FILE_NAME_PREFIX = ".";

    public final boolean isHidden() throws IOException {
        return getName().startsWith(HIDDEN_FILE_NAME_PREFIX);
    }

    //
    // Interface: Access Control (Future)

    public final void checkAccess(AccessModes modes) {
        // TODO: Future feature
        throw new NotImplementedException("Access control is not yet implemented");
    }

    //
    // Implementation Stub: Node Equality Check

    // TODO: Change to protected
    public abstract boolean isSameFile(DynNode<Space, ?> other);

    //
    // Implementation Stub: DynFileSystemProvider I/O, Node Deletion

    final void preDelete() throws IOException {
        // TODO: Check access permissions (future feature)
        // TODO: Close resources? Leave resources hanging to auto-close? (like Unix ext)
        // TODO: Design philosophy: Should a failure during deleteImpl be allowed to
        // have a persistent impact on the state of the application? (e.g. closed
        // resources)
        preDeleteImpl();
    }

    protected void preDeleteImpl() throws IOException {}

    /**
     * @see FileSystemProvider#delete(Path)
     */
    public final void delete() throws IOException {
        if (parent == null)
            // TODO: Temporary exception; find better exception
            throw new UnsupportedOperationException("Cannot delete root dir");

        parent.deleteChild(getName(), this);
    }

    protected abstract void deleteImpl() throws IOException;

    protected void postDeleteImpl() {}

}
