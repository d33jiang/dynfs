package dynfs.core;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.ClosedFileSystemException;
import java.nio.file.FileStore;
import java.nio.file.attribute.FileAttributeView;
import java.nio.file.attribute.FileStoreAttributeView;
import java.util.Set;

import dynfs.core.path.DynRoute;

public abstract class DynSpace<Space extends DynSpace<Space>> extends FileStore implements Closeable {

    // TODO: Lazy size calculation interface

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

    // TODO: API Clarification - Is totalSpace every allowed to change?

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

    // TODO: 3-phase w/ sync
    // TODO: Does DynFileSystem really need 3-phase w/ sync if this class employs
    // 3-phase sync?

    private boolean isClosed;

    public final boolean isClosed() {
        return isClosed;
    }

    public final void throwIfClosed() throws IOException {
        if (isClosed)
            throw new ClosedFileSystemException();
    }

    @Override
    public final void close() throws IOException {
        closeImpl();
        isClosed = true;
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

        this.isClosed = false;
    }

    //
    // Interface Implementation Default: Usable Space

    @Override
    public long getUsableSpace() throws IOException {
        return getUnallocatedSpace();
    }

    //
    // Interface Implementation Stub: DynSpace Properties

    // TODO: Review API specification for name()
    @Override
    public abstract String name();

    @Override
    public abstract String type();

    // TODO: API Clarification - Is isReadOnly allowed to change?
    @Override
    public abstract boolean isReadOnly();

    //
    // Implementation Stub: DynSpace Attributes

    // TODO: Concrete implementation to get name?
    // TODO: Concrete implementation to get default DynSpaceAttributes
    // create default DynSpace attributes class
    // abstract impl to get SpecialAttributes
    // TODO: Rename DynAttributes to DynNodeAttributes

    @Override
    public abstract <V extends FileStoreAttributeView> V getFileStoreAttributeView(Class<V> type);

    @Override
    public abstract Object getAttribute(String attribute) throws IOException;

    //
    // Implementation Stub: Supported File Attribute Views

    @Override
    public final boolean supportsFileAttributeView(Class<? extends FileAttributeView> type) {
        return supportedFileAttributeViews().contains(type);
    }

    @Override
    public final boolean supportsFileAttributeView(String name) {
        return supportedFileAttributeViewsByName().contains(name);
    }

    // TODO: Concrete implementation to get default DynFileAttributesView types
    // union with SpecialAttributeViews

    public abstract Set<Class<? extends FileAttributeView>> supportedFileAttributeViews();

    public abstract Set<String> supportedFileAttributeViewsByName();

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

}
