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
    // Validation Helper: Size

    private static void validateSize(long size, String label) {
        if (size < 0) {
            if (label == null)
                label = "The size argument";
            throw new IllegalArgumentException(label + " must be nonnegative");
        }
    }

    private static void validateSize(long size, String label, long maxValue, String labelMaxValue) {
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
    // Field: Total Space

    private final long totalSpace;

    @Override
    public final long getTotalSpace() throws IOException {
        return totalSpace;
    }

    //
    // Field: Allocated Space

    private long allocatedSpace;

    public final long getAllocatedSpace() {
        return allocatedSpace;
    }

    protected final void setAllocatedSpace(long allocatedSpace) {
        validateSize(allocatedSpace, "usedSpace", totalSpace, "totalSpace");
        this.allocatedSpace = allocatedSpace;
    }

    @Override
    public final long getUnallocatedSpace() throws IOException {
        return totalSpace - allocatedSpace;
    }

    //
    // Interface: Usable Space (Abstract)

    @Override
    public long getUsableSpace() throws IOException {
        return getUnallocatedSpace();
    }

    //
    // Construction

    protected DynSpace(long totalSpace) {
        this(totalSpace, 0);
    }

    protected DynSpace(long totalSpace, long usedSpace) {
        validateSize(totalSpace, "totalSpace");
        validateSize(usedSpace, "usedSpace", totalSpace, "totalSpace");

        this.totalSpace = totalSpace;
        this.allocatedSpace = usedSpace;
    }

    //
    // Implementation Stub: DynSpace Properties (Abstract)

    @Override
    public abstract String name();

    @Override
    public abstract String type();

    @Override
    public abstract boolean isReadOnly();

    //
    // Implementation Stub: DynSpace Attributes (Abstract)

    @Override
    public abstract <V extends FileStoreAttributeView> V getFileStoreAttributeView(Class<V> type);

    @Override
    public abstract Object getAttribute(String attribute) throws IOException;

    //
    // Implementation Stub: Supported File Attribute Views (Abstract)

    public abstract Set<Class<? extends FileAttributeView>> supportedFileAttributeViews();

    public abstract Set<String> supportedFileAttributeViewsByName();

    @Override
    public final boolean supportsFileAttributeView(Class<? extends FileAttributeView> type) {
        return supportedFileAttributeViews().contains(type);
    }

    @Override
    public final boolean supportsFileAttributeView(String name) {
        return supportedFileAttributeViewsByName().contains(name);
    }

    //
    // Implementation Stub: Close (Abstract)

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
    // Implementation Stub: Root Directory

    public abstract <DirNode extends DynDirectory<Space, DirNode>> DirNode getRootDirectory();

    //
    // Interface: Route Resolution

    final ResolutionResult<Space> resolve(DynRoute route) throws IOException {
        return getRootDirectory().resolve(route);
    }

    final ResolutionResult<Space> resolve(DynRoute route, boolean followLinks) throws IOException {
        return getRootDirectory().resolve(route, followLinks);
    }

}
