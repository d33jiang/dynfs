package dynfs.core;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.ClosedFileSystemException;
import java.nio.file.FileStore;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.FileAttributeView;
import java.nio.file.attribute.FileStoreAttributeView;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;

public abstract class DynSpace<Space extends DynSpace<Space>> extends FileStore implements Closeable {

    //
    // Static Support: Size Validation

    private static void validateSize(String label, long size) {
        if (size < 0) {
            if (label == null)
                label = "The size argument";
            throw new IllegalArgumentException(label + " must be nonnegative");
        }
    }

    private static void validateSize(String label, long size, String labelMaxValue, long maxValue) {
        if (size < 0) {
            if (label == null)
                label = "The size argument";
            throw new IllegalArgumentException(label + " must be nonnegative");
        }
        if (size > maxValue) {
            if (labelMaxValue == null)
                labelMaxValue = String.valueOf(maxValue);
            throw new IllegalArgumentException(label + " must be at most " + labelMaxValue);
        }
    }

    //
    // Configuration: Total Space

    private final long totalSpace;

    @Override
    public final long getTotalSpace() throws IOException {
        return totalSpace;
    }

    //
    // State: Allocated Space

    private long allocatedSpace;

    public final long getAllocatedSpace() throws IOException {
        if (isAllocatedSpaceSizeStale()) {
            refreshAllocatedSpaceSize();
        }
        return allocatedSpace;
    }

    @Override
    public final long getUnallocatedSpace() throws IOException {
        return totalSpace - getAllocatedSpace();
    }

    protected final void setAllocatedSpace(long allocatedSpace) {
        validateSize("allocatedSpace", allocatedSpace, "totalSpace", totalSpace);
        this.allocatedSpace = allocatedSpace;
    }

    protected boolean isAllocatedSpaceSizeStale() throws IOException {
        return false;
    }

    protected void refreshAllocatedSpaceSize() throws IOException {}

    //
    // State: Status

    private static final Status INITIAL_STATUS = Status.ACTIVE;

    private final Lock STATUS_LOCK = new ReentrantLock();
    private Status status = INITIAL_STATUS;

    public static enum Status {
        ACTIVE(false),
        TERMINATING(true),
        CLOSED(true),
        ERROR_WHILE_TERMINATING(true);

        private final boolean isClosed;

        private Status(boolean isClosed) {
            this.isClosed = isClosed;
        }

        public boolean isClosed() {
            return isClosed;
        }

        public boolean isOpen() {
            return !isClosed();
        }
    }

    public final Status status() {
        return status;
    }

    public final boolean isClosed() {
        return status().isClosed();
    }

    public final void throwIfClosed() throws IOException {
        if (isClosed())
            throw new ClosedFileSystemException();
    }

    @Override
    public final void close() throws IOException {
        if (status != Status.CLOSED) {
            STATUS_LOCK.lock();
            try {
                if (status != Status.CLOSED) {
                    status = Status.TERMINATING;
                    try {
                        closeImpl();
                    } catch (IOException | RuntimeException ex) {
                        status = Status.ERROR_WHILE_TERMINATING;
                    } finally {
                        if (status == Status.TERMINATING) {
                            status = Status.CLOSED;
                        }
                    }
                }
            } finally {
                STATUS_LOCK.unlock();
            }
        }
    }

    protected abstract void closeImpl() throws IOException;

    //
    // Construction

    protected DynSpace(long totalSpace) {
        this(totalSpace, 0);
    }

    protected DynSpace(long totalSpace, long usedSpace) {
        validateSize("totalSpace", totalSpace);
        validateSize("usedSpace", usedSpace, "totalSpace", totalSpace);

        this.totalSpace = totalSpace;
        this.allocatedSpace = usedSpace;
    }

    //
    // Interface Implementation Default: Usable Space

    @Override
    public long getUsableSpace() throws IOException {
        return getUnallocatedSpace();
    }

    //
    // Interface Stub: DynSpace Name

    @Override
    public abstract String name();

    //
    // Interface Stub: DynSpace Type

    @Override
    public final String type() {
        return getType().toTypeString();
    }

    public abstract DynSpaceType getType();

    //
    // Interface Stub: DynSpace Read-Only Property

    @Override
    public abstract boolean isReadOnly();

    //
    // Interface Implementation: DynSpace Attributes

    @Override
    public final <V extends FileStoreAttributeView> V getFileStoreAttributeView(Class<V> type) {
        if (!type.isAssignableFrom(DynSpaceAttributeView.class))
            return null;

        DynSpaceAttributeView attributeView = new DynSpaceAttributeView(this);

        // If (type.isAssignableFrom(DynSpaceAttributeView.class)), then attributeView
        // is of type V.
        @SuppressWarnings("unchecked")
        V result = (V) attributeView;

        return result;
    }

    @Override
    public final Object getAttribute(String attribute) throws IOException {
        switch (attribute) {
            case "name":
                return name();
        }

        throw new UnsupportedOperationException(attribute + " is not a DynSpace attribute");
    }

    //
    // Interface Implementation: Supported File Attribute Views

    @Override
    public final boolean supportsFileAttributeView(Class<? extends FileAttributeView> type) {
        return supportedFileAttributeViews().contains(type);
    }

    @Override
    public final boolean supportsFileAttributeView(String name) {
        return supportedFileAttributeViewsByName().contains(name);
    }

    public final Set<Class<? extends FileAttributeView>> supportedFileAttributeViews() {
        return ImmutableSet.of(DynNodeFileAttributeView.class, BasicFileAttributeView.class);
    }

    public final Set<String> supportedFileAttributeViewsByName() {
        return ImmutableSet.of(DynNodeFileAttributeView.FILE_ATTRIBUTE_VIEW_NAME, "basic");
    }

    //
    // Interface Default: Supported DynNode Attribute Views

    public final void checkSupportsDynNodeAttributeViews(Set<DynNodeAttribute.View> views)
            throws UnsupportedOperationException {
        Set<DynNodeAttribute.View> unsupportedViews = Sets.difference(views, supportedDynNodeAttributeViews());
        if (!unsupportedViews.isEmpty())
            throw new UnsupportedOperationException("Unsupported DynNode Attribute Views: " + unsupportedViews);
    }

    public final Set<DynNodeAttribute.View> supportedDynNodeAttributeViews() {
        return Sets.union(supportedExtraDynNodeAttributeViews(), ImmutableSet.of(DynNodeAttribute.Base.VIEW));
    }

    protected Set<DynNodeAttribute.View> supportedExtraDynNodeAttributeViews() {
        return ImmutableSet.of();
    }

    //
    // Interface Implementation Stub: Root Directory

    public abstract <DirNode extends DynDirectory<Space, DirNode>> DirNode getRootDirectory();

    //
    // Package Support: Route Resolution

    final ResolutionResult<Space> resolve(DynRoute route) throws IOException {
        return getRootDirectory().resolve(route);
    }

    final ResolutionResult<Space> resolve(DynRoute route, boolean followLinks) throws IOException {
        return getRootDirectory().resolve(route, followLinks);
    }

    // TODO: Atomic I/O - Route Exclusivity Lock?

}
