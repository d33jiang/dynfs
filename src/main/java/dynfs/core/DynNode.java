package dynfs.core;

import java.io.IOException;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.nio.file.spi.FileSystemProvider;
import java.time.Instant;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.lang3.NotImplementedException;

import com.google.common.collect.ImmutableMap;

import dynfs.core.options.AccessModes;

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
    // State: Status

    public enum DynNodeStatus {
        NORMAL,
        LOCKED,
        DELETED;

        public boolean isDeleted() {
            return this == DELETED;
        }
    }

    private DynNodeStatus status = DynNodeStatus.NORMAL;

    public DynNodeStatus status() {
        return status;
    }

    //
    // Construction

    <DirNode extends DynDirectory<Space, DirNode>> DynNode(Space store, DirNode parent, String name) {
        this.store = store;
        this.parent = parent;
        this.name = name;
    }

    //
    // Core Support: Equality Check

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof DynNode))
            return false;
        if (store != ((DynNode<?, ?>) other).store)
            return false;

        // If (store == other.store), then (store.getClass() == other.store.getClass()).
        // The type parameter (Space) has deliberately been made inflexible so that it
        // is ascertainable that other is of type (DynNode<Space, ?>).
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
        if (name.isEmpty())
            throw new IllegalArgumentException(lblName + " must be non-empty");
        if (name.contains("/"))
            throw new IllegalArgumentException(lblName + " cannot contain '/'");
    }

    //
    // Interface Implementation: Canonical Route

    private final void constructRoutePath(Deque<String> routeNames) {
        if (parent != null) {
            ((DynNode<Space, ?>) parent).constructRoutePath(routeNames);
        }

        if (getName() != null) {
            routeNames.add(getName());
        }
    }

    public final DynRoute getRoute() {
        LinkedList<String> routeNames = new LinkedList<>();
        constructRoutePath(routeNames);
        return DynRoute.fromRouteNameList(true, routeNames);
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
    // Interface: Root Node Check

    public boolean isRoot() {
        return parent == null;
    }

    //
    // Interface Implementation Stub: DynNode Size, Cache on Load

    public abstract long readSize() throws IOException;

    // NOTE: Javadoc Note - Default implementation
    protected void writeSize(long newSize) throws IOException {
        throw new UnsupportedOperationException("The size of this DynNode cannot be rewritten");
    }

    //
    // Implementation Default: DynNode Type Attributes, Cache on Load
    // Best Practice: Determined by Class

    public abstract boolean isRegularFile();

    public abstract boolean isDirectory();

    public abstract boolean isSymbolicLink();

    public abstract boolean isOther();

    //
    // Interface: Access Control (Future)

    public final void checkAccess(AccessModes modes) {
        // FUTURE: Access Control
        throw new NotImplementedException("Access control is not yet implemented");
    }

    //
    // Support Structure: Attribute I/O, Failure Output

    // NOTE: Preemptive support structure interface definition
    public static final class DynNodeAttributeIOFailure {

        //
        // Configuration: Failure Details

        private final boolean onRead;
        private final String message;
        private final Object cause;

        //
        // Construction: Factory

        private DynNodeAttributeIOFailure(boolean onRead, String message, Object cause) {
            this.onRead = onRead;
            this.message = message;
            this.cause = cause;
        }

        private static DynNodeAttributeIOFailure onRead(String message, Object cause) {
            return new DynNodeAttributeIOFailure(true, message, cause);
        }

        private static DynNodeAttributeIOFailure onWrite(String message, Object cause) {
            return new DynNodeAttributeIOFailure(true, message, cause);
        }

        //
        // Core Support: Conversion to String

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();

            sb.append(super.toString());
            sb.append(System.lineSeparator());

            sb.append("Failure on ");
            sb.append(onRead ? "Read" : "Write");
            sb.append(System.lineSeparator());

            sb.append("Message: ");
            sb.append(message);
            sb.append(System.lineSeparator());

            sb.append("Cause:");
            sb.append(System.lineSeparator());
            sb.append(cause.toString());

            return sb.toString();
        }

        //
        // Inheritable Static Support: Construction by Cause

        protected static DynNodeAttributeIOFailure ioExceptionOnRead(IOException ex) {
            return onRead("IOException Caught", ex);
        }

        protected static DynNodeAttributeIOFailure ioExceptionOnWrite(IOException ex) {
            return onWrite("IOException Caught", ex);
        }

    }

    //
    // Static Support: Map Key-Transformation

    private static <K1, V, K2> Map<K2, V> transformMapKeys(Map<K1, V> map, Function<K1, K2> keyMapper) {
        return map.entrySet().stream()
                .collect(Collectors.toMap(e -> keyMapper.apply(e.getKey()), Map.Entry::getValue));
    }

    //
    // Implementation Stub: Attribute I/O, Read by Key Set

    public final Map<DynNodeAttribute, Object> readAttributes(Set<DynNodeAttribute> keys) throws IOException {
        return readAttributesImpl(keys);
    }

    protected abstract Map<DynNodeAttribute, Object> readAttributesImpl(Set<DynNodeAttribute> keys) throws IOException;

    protected abstract Map<DynNodeAttribute, Object> readAllAttributes() throws IOException;

    /**
     * @see FileSystemProvider#readAttributes(Path, String, LinkOption...)
     */
    public final Map<String, Object> readAttributes(String attributes)
            throws IOException {
        Set<DynNodeAttribute> attrs = DynNodeAttribute.parseToSet(attributes);

        if (attrs.isEmpty())
            throw new IllegalArgumentException("No attributes are specified");

        getStore().checkSupportsDynNodeAttributeViews(DynNodeAttribute.getDynNodeAttributeViews(attrs));

        return transformMapKeys(readAttributes(attrs), DynNodeAttribute::toString);
    }

    //
    // Package Support: Touch DynNode

    final void touchByRead() throws IOException {
        // FUTURE: Access Control - Return immediately with no-op if no write access

        FileTime t = FileTime.from(Instant.now());
        writeTimes(null, null, t);
    }

    final void touchByWrite() throws IOException {
        FileTime t = FileTime.from(Instant.now());
        writeTimes(null, t, t);
    }

    //
    // Implementation Default: Attribute I/O, Write Times

    final void writeTimes(FileTime creationTime, FileTime lastModifiedTime, FileTime lastAccessTime)
            throws IOException {
        // FUTURE: Access Control - Check write access
        // -> Beware of read-only DynSpace instances

        writeTimesImpl(creationTime, lastModifiedTime, lastAccessTime);
    }

    // NOTE: Javadoc Note - Although it is tempting to cache the write operation, it
    // must appear as though that the change has been committed after the
    // termination of this method call; i.e. readAttributesAsDynNodeFileAttributes
    // must return the new values on subsequent calls
    protected void writeTimesImpl(FileTime creationTime, FileTime lastModifiedTime, FileTime lastAccessTime)
            throws IOException {
        Map<DynNodeAttribute, FileTime> newMappings = new HashMap<>();

        if (creationTime != null) {
            newMappings.put(DynNodeAttribute.Base.CREATION_TIME, creationTime);
        }
        if (lastModifiedTime != null) {
            newMappings.put(DynNodeAttribute.Base.LAST_MODIFIED_TIME, lastModifiedTime);
        }
        if (lastAccessTime != null) {
            newMappings.put(DynNodeAttribute.Base.LAST_ACCESS_TIME, lastAccessTime);
        }

        if (!newMappings.isEmpty()) {
            writeAttributes(newMappings);
        }
    }

    //
    // Implementation Stub: Attribute I/O, Write by Key-Value Map

    /**
     * @see FileSystemProvider#setAttribute(Path, String, Object, LinkOption...)
     */
    public final Object writeAttribute(String attribute, Object value) throws IOException {
        return writeAttributeImpl(attribute, value);
    }

    public final Object writeAttribute(DynNodeAttribute attribute, Object value) throws IOException {
        return writeAttributeImpl(attribute, value);
    }

    public final Map<DynNodeAttribute, Object> writeAttributes(Map<DynNodeAttribute, ?> newMappings)
            throws IOException {
        // FUTURE: Access Control - Check write access
        // -> Beware of read-only DynSpace instances

        return transformMapKeys(writeAttributesImpl(transformMapKeys(newMappings, DynNodeAttribute::toString)),
                DynNodeAttribute::parse);
    }

    private Object writeAttributeImpl(String attribute, Object value) throws IOException {
        return writeAttributeImpl(DynNodeAttribute.parse(attribute), value);
    }

    private Object writeAttributeImpl(DynNodeAttribute attribute, Object value) throws IOException {
        return writeAttributesImpl(ImmutableMap.of(attribute.toString(), value)).get(attribute.toString());
    }

    protected abstract Map<String, Object> writeAttributesImpl(Map<String, ?> newMappings) throws IOException;

    //
    // Implementation: Attribute I/O, Read to DynNodeAttributes Instance

    /**
     * @see FileSystemProvider#readAttributes(Path, Class, LinkOption...)
     */
    @SuppressWarnings("unchecked")
    public final <A extends BasicFileAttributes> A readAttributesAsFileAttributesClass(Class<A> type)
            throws IOException {
        if (!type.isAssignableFrom(DynNodeFileAttributes.class))
            throw new UnsupportedOperationException("Attributes of the given type are not supported");

        return (A) readAttributesAsDynNodeFileAttributes();
    }

    // NOTE: This method is package-private because the BasicFileAttributes
    // interface is not supported by DynFileSystems beyond the necessary interfaces
    final DynNodeFileAttributes readAttributesAsDynNodeFileAttributes() throws IOException {
        return new DynNodeFileAttributes(this);
    }

    //
    // Interface Implementation: DynNode Hidden Attribute

    public static final String HIDDEN_FILE_NAME_PREFIX = ".";

    public final boolean isHidden() throws IOException {
        return getName().startsWith(HIDDEN_FILE_NAME_PREFIX);
    }

    //
    // Implementation Stub: DynFileSystemProvider I/O, Node Equality Check

    protected abstract boolean isSameFile(DynNode<Space, ?> other);

    //
    // Implementation Stub: DynFileSystemProvider I/O, Node Deletion

    final void preDelete() throws IOException {
        // FUTURE: Access Control - Check access control
        // TODO: Close resources? or leave resources hanging to auto-close? (like Unix?)
        // -> Design Consideration - Should a failure during deleteImpl be allowed to
        // have a persistent impact on the state of the application? (e.g. closed
        // resources)
        preDeleteImpl();
    }

    protected void preDeleteImpl() throws IOException {}

    /**
     * @see FileSystemProvider#delete(Path)
     */
    public final void delete() throws IOException {
        if (status.isDeleted())
            throw new NoSuchFileException(getRouteString());
        if (isRoot())
            // TODO: Design Consideration - Temporary exception; find better exception
            throw new UnsupportedOperationException("Cannot delete root node");

        parent.deleteChild(getName(), this);
    }

    protected abstract void deleteImpl() throws IOException;

    protected void postDeleteImpl() {}

}
