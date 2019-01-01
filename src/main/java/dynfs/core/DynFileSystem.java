package dynfs.core;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.AccessMode;
import java.nio.file.ClosedFileSystemException;
import java.nio.file.CopyOption;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.DirectoryStream;
import java.nio.file.DirectoryStream.Filter;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileStore;
import java.nio.file.FileSystem;
import java.nio.file.NotDirectoryException;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.WatchEvent.Kind;
import java.nio.file.WatchEvent.Modifier;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.FileAttributeView;
import java.nio.file.attribute.UserPrincipalLookupService;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import dynfs.core.io.DynByteChannel;
import dynfs.core.io.DynDirectoryStream;
import dynfs.core.options.AccessModes;
import dynfs.core.options.CopyOptions;
import dynfs.core.options.LinkOptions;
import dynfs.core.options.OpenOptions;
import dynfs.core.path.DynPath;
import dynfs.core.path.DynRoute;
import dynfs.core.store.DynSpaceFactory;
import dynfs.core.store.DynSpaceIO;
import dynfs.core.store.DynSpaceLoader;

public final class DynFileSystem<Space extends DynSpace<Space>> extends FileSystem {

    //
    // Field: File System Provider

    private final DynFileSystemProvider provider;

    @Override
    public DynFileSystemProvider provider() {
        return provider;
    }

    //
    // Field: Domain

    private final String domain;

    public String domain() {
        return domain;
    }

    //
    // Field: Status

    private static final Status INITIAL_STATUS = Status.ACTIVE;

    private final Lock STATUS_LOCK = new ReentrantLock();
    private Status status = INITIAL_STATUS;

    private static enum Status {
        ACTIVE, TERMINATION, CLOSED;

        public boolean isOpen() {
            return !isClosed();
        }

        public boolean isClosed() {
            return this == Status.TERMINATION || this == CLOSED;
        }
    }

    //
    // Field: File Store

    private final Space store;

    public Space getStore() {
        return store;
    }

    //
    // Construction

    private DynFileSystem(DynFileSystemProvider provider, String domain, Space store) {
        this.provider = provider;
        this.domain = domain;
        this.store = store;
    }

    static <Space extends DynSpace<Space>> DynFileSystem<Space> newFileSystem(DynFileSystemProvider provider,
            String domain, DynSpaceFactory<Space> storeFactory, Map<String, ?> env) throws IOException {
        Space store = storeFactory.createStore(env);
        return new DynFileSystem<>(provider, domain, store);
    }

    static <Space extends DynSpace<Space>> DynFileSystem<Space> loadFileSystem(DynFileSystemProvider provider,
            String domain, DynSpaceLoader<? extends Space> storeLoader, Map<String, ?> env) throws IOException {
        Space store = storeLoader.loadStore(env);
        return new DynFileSystem<>(provider, domain, store);
    }

    //
    // Interface: Status

    @Override
    public boolean isOpen() {
        return status.isOpen();
    }

    @Override
    public void close() throws IOException {
        if (status != Status.CLOSED) {
            STATUS_LOCK.lock();
            try {
                if (status != Status.CLOSED)
                    closeImpl();
            } finally {
                STATUS_LOCK.unlock();
            }
        }
    }

    //
    // Interface: Read-Only Property

    @Override
    public boolean isReadOnly() {
        return store.isReadOnly();
    }

    //
    // Interface: Paths

    @Override
    public String getSeparator() {
        return DynRoute.PATH_SEPARATOR;
    }

    @Override
    public DynPath getPath(String first, String... more) {
        return DynPath.newPath(this, first, more);
    }

    @Override
    public PathMatcher getPathMatcher(String syntaxAndPattern) {
        return DynPath.getPathMatcher(syntaxAndPattern);
    }

    //
    // Interface: Root Directories

    public DynPath getRootDirectory() {
        return getPath(DynRoute.ROOT_PATH_STRING);
    }

    @Override
    public Iterable<Path> getRootDirectories() {
        return Arrays.asList(getRootDirectory());
    }

    //
    // Interface: File Stores

    @Override
    public Iterable<FileStore> getFileStores() {
        return Arrays.asList(getStore());
    }

    //
    // File Store: File Attributes

    @Override
    public Set<String> supportedFileAttributeViews() {
        return store.supportedFileAttributeViewsByName();
    }

    //
    // User Principals (Unsupported)

    @Override
    public UserPrincipalLookupService getUserPrincipalLookupService() {
        // NOTE: Future feature?
        throw new UnsupportedOperationException("Users and groups are not supported");
    }

    //
    // Implementation: Close

    private void closeImpl() throws IOException {
        status = Status.TERMINATION;

        IOException ex = null;

        try {
            getStore().close();
        } catch (IOException e) {
            if (ex == null)
                ex = e;
        }

        status = Status.CLOSED;

        if (ex != null)
            throw ex;
    }

    //
    // Helper: Status Check

    private void throwIfClosed() {
        if (status.isClosed())
            throw new ClosedFileSystemException();
    }

    //
    // Implementation: WatchService Support (Unsupported)

    @Override
    public WatchService newWatchService() throws IOException {
        // NOTE: Future feature?
        throw new UnsupportedOperationException("Watch services are not supported");
    }

    public WatchKey register(WatchService watcher, DynRoute route, Kind<?>[] events) throws IOException {
        // NOTE: Future feature?
        // NOTE: Delegate to WatchService
        throw new UnsupportedOperationException("Watch services are not supported");
    }

    public WatchKey register(WatchService watcher, DynRoute route, Kind<?>[] events, Modifier[] modifiers)
            throws IOException {
        // NOTE: Future feature?
        // NOTE: Delegate to WatchService
        throw new UnsupportedOperationException("Watch services are not supported");
    }

    //
    // Interface Delegation: DynFileIO

    private DynSpaceIO<Space> getIOInterface() {
        STATUS_LOCK.lock();
        try {
            throwIfClosed();
            return getStore().getIOInterface();
        } finally {
            STATUS_LOCK.unlock();
        }
    }

    //
    // Implementation: I/O

    // I/O: Existence Query
    boolean exists(DynRoute route, boolean nofollowLinks) throws IOException {
        return getStore().resolve(route, !nofollowLinks).exists();
    }

    // I/O: Byte Channel
    SeekableByteChannel newByteChannel(DynRoute route,
            OpenOptions openOptions,
            FileAttribute<?>... attrs)
            throws IOException {
        ResolutionResult<Space> resolution = store.resolve(route);
        DynNode<Space, ?> node = resolution.testExistenceForCreation();

        if (node != null && !(node instanceof DynFile)) {
            // Non-file exists at route
            throw new FileAlreadyExistsException(route.toString(), null, "Non-file already exists");
        }
        DynFile<Space, ?> file = (DynFile<Space, ?>) node;

        if (file != null && openOptions.createNew)
            throw new FileAlreadyExistsException(route.toString());
        if (file == null && !(openOptions.create || openOptions.createNew))
            throw new FileNotFoundException(route.toString());

        if (file == null) {
            file = getIOInterface().createFile((DynDirectory<Space, ?>) resolution.node(), route.getFileNameAsString(),
                    attrs);
        }

        return new DynByteChannel<>(file, openOptions);
    }

    // I/O: Directory Stream
    DirectoryStream<Path> newDirectoryStream(DynRoute dir, Filter<? super Path> filter) throws IOException {
        DynNode<Space, ?> node = store.resolve(dir).testExistence();
        if (!(node instanceof DynDirectory))
            throw new NotDirectoryException(dir.toString());

        return new DynDirectoryStream<Space>((DynDirectory<Space, ?>) node, filter);
    }

    // I/O: Directory Creation
    void createDirectory(DynRoute dir, FileAttribute<?>... attrs) throws IOException {
        ResolutionResult<Space> resolution = store.resolve(dir);
        DynNode<Space, ?> node = resolution.testExistenceForCreation();

        if (node != null)
            throw new FileAlreadyExistsException(dir.toString());

        getIOInterface().createDirectory((DynDirectory<Space, ?>) resolution.node(), dir.getFileNameAsString(), attrs);
    }

    // I/O: File Deletion
    void delete(DynRoute route) throws IOException {
        DynNode<Space, ?> node = store.resolve(route).testExistence();
        getIOInterface().delete(node);
    }

    // I/O: File Copy & Move
    private void copyImpl(DynRoute src, DynRoute dst, CopyOptions copyOptions, boolean deleteSrc) throws IOException {
        ResolutionResult<Space> srcResolution = store.resolve(src, !copyOptions.nofollowLinks);
        DynNode<Space, ?> srcNode = srcResolution.testExistence();
        ResolutionResult<Space> dstResolution = store.resolve(dst, !copyOptions.nofollowLinks);
        DynNode<Space, ?> dstNode = dstResolution.testExistenceForCreation();

        if (copyOptions.atomicMove) {
            // TODO: FS structure locks ...
        }

        DynDirectory<Space, ?> dstParentNode = dstNode == null ? (DynDirectory<Space, ?>) dstResolution.node()
                : dstNode.getParent();

        BasicFileAttributes srcAttributes = srcNode.readAttributes(BasicFileAttributes.class);

        if (dstNode != null) {
            if (copyOptions.replaceExisting) {
                if ((dstNode instanceof DynDirectory)
                        && !((DynDirectory<Space, ?>) dstNode).isEmpty()) {
                    throw new DirectoryNotEmptyException(dst.toString());
                }
                getIOInterface().delete(dstNode);
            } else {
                throw new FileAlreadyExistsException(dst.toString());
            }
        }

        if (srcAttributes.isDirectory()) {
            getIOInterface().createDirectory(dstParentNode, dst.getFileNameAsString());
        } else {
            getIOInterface().copy(srcNode, dstParentNode, dst.getFileNameAsString(), deleteSrc);
        }

        if (copyOptions.copyAttributes) {
            try {
                BasicFileAttributeView dstAttributes = getIOInterface().getFileAttributeView(dst,
                        BasicFileAttributeView.class,
                        copyOptions.getLinkOptions());
                dstAttributes.setTimes(srcAttributes.lastModifiedTime(), srcAttributes.lastAccessTime(),
                        srcAttributes.creationTime());
            } catch (IOException ex) {
                try {
                    // TODO: Revert
                    throw new IOException("Dummy");
                } catch (IOException ex0) {
                    ex.addSuppressed(ex0);
                }

                throw ex;
            }
        }

    }

    // I/O: File Copy
    void copy(DynRoute source, DynRoute target, CopyOptions copyOptions) throws IOException {
        copyImpl(source, target, copyOptions, false);
    }

    // I/O: File Move
    void move(DynRoute source, DynRoute target, CopyOptions copyOptions) throws IOException {
        copyImpl(source, target, copyOptions, true);
    }

    // I/O: Query, Files Same
    boolean isSameFile(DynRoute route1, DynRoute route2) throws IOException {
        ResolutionResult<Space> resolution1 = store.resolve(route1);
        ResolutionResult<Space> resolution2 = store.resolve(route2);

        DynNode<Space, ?> node1 = resolution1.testExistence();
        DynNode<Space, ?> node2 = resolution2.testExistence();

        return node1.equalsNode(node2);
    }

    // I/O: Query, File Hidden
    boolean isHidden(DynRoute route) throws IOException {
        DynNode<Space, ?> node = store.resolve(route).testExistence();
        return node.isHidden();
    }

    // I/O: Access Control
    void checkAccess(DynRoute route, AccessModes accessModes) throws IOException {
        DynNode<Space, ?> node = store.resolve(route).testExistence();
        node.checkAccess(accessModes);
    }

    // I/O: File Attributes, Get
    @SuppressWarnings("unchecked")
    <V extends FileAttributeView> V getFileAttributeView(DynRoute route, Class<V> type, LinkOptions options) {
        if (!type.isAssignableFrom(DynAttributesView.class))
            return null;

        return (V) new DynAttributesView<Space>(store, route);
    }

    // I/O: File Attributes, Read by Subclass of BasicFileAttributes
    <A extends BasicFileAttributes> A readAttributes(DynRoute route, Class<A> type, LinkOptions options)
            throws IOException {
        DynNode<Space, ?> node = store.resolve(route, !options.nofollowLinks).testExistence();
        return node.readAttributes(type);
    }

    // I/O: File Attributes, Read by String [Name of FileAttribute Set]
    Map<String, Object> readAttributes(DynRoute route, String attributes, LinkOptions options) throws IOException {
        DynNode<Space, ?> node = store.resolve(route, !options.nofollowLinks).testExistence();
        return node.readAttributes(attributes);
    }

    // I/O: File Attributes, Set
    void setAttribute(DynRoute route, String attribute, Object value, LinkOptions options) throws IOException {
        DynNode<Space, ?> node = store.resolve(route, !options.nofollowLinks).testExistence();
        node.setAttribute(attribute, value);
    }

}
