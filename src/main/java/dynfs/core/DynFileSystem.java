package dynfs.core;

import java.io.IOException;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.AccessMode;
import java.nio.file.ClosedFileSystemException;
import java.nio.file.CopyOption;
import java.nio.file.DirectoryStream;
import java.nio.file.DirectoryStream.Filter;
import java.nio.file.FileStore;
import java.nio.file.FileSystem;
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.WatchEvent.Kind;
import java.nio.file.WatchEvent.Modifier;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.FileAttributeView;
import java.nio.file.attribute.UserPrincipalLookupService;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import dynfs.core.path.DynPath;
import dynfs.core.path.DynRoute;
import dynfs.core.store.DynFileIO;
import dynfs.core.store.DynSpaceFactory;
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
        return DynPath.PATH_SEPARATOR;
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
        return getPath(DynPath.ROOT_PATH_STRING);
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

    private DynFileIO getIOInterface() {
        STATUS_LOCK.lock();
        try {
            throwIfClosed();
            return getStore().getIOInterface();
        } finally {
            STATUS_LOCK.unlock();
        }
    }

    // I/O: Existence Query
    boolean exists(DynRoute route, boolean nofollowLinks) throws IOException {
        return getIOInterface().exists(route, nofollowLinks);
    }

    // I/O: Byte Channel
    SeekableByteChannel newByteChannel(DynRoute route, Set<? extends OpenOption> options, FileAttribute<?>... attrs)
            throws IOException {
        return getIOInterface().newByteChannel(route, options, attrs);
    }

    // I/O: Directory Stream
    DirectoryStream<Path> newDirectoryStream(DynRoute dir, Filter<? super Path> filter) throws IOException {
        return getIOInterface().newDirectoryStream(dir, filter);
    }

    // I/O: Directory Creation
    void createDirectory(DynRoute dir, FileAttribute<?>... attrs) throws IOException {
        getIOInterface().createDirectory(dir, attrs);
    }

    // I/O: File Deletion
    void delete(DynRoute route) throws IOException {
        getIOInterface().delete(route);
    }

    // I/O: File Copy
    void copy(DynRoute source, DynRoute target, CopyOption... options) throws IOException {
        getIOInterface().copy(source, target, options);
    }

    // I/O: File Move
    void move(DynRoute source, DynRoute target, CopyOption... options) throws IOException {
        getIOInterface().move(source, target, options);
    }

    // I/O: Query, Files Same
    boolean isSameFile(DynRoute route1, DynRoute route2) throws IOException {
        return getIOInterface().isSameFile(route1, route2);
    }

    // I/O: Query, File Hidden
    boolean isHidden(DynRoute route) throws IOException {
        return getIOInterface().isHidden(route);
    }

    // I/O: Access Control
    void checkAccess(DynRoute route, AccessMode... modes) throws IOException {
        getIOInterface().checkAccess(route, modes);
    }

    // I/O: File Attributes, Get
    <V extends FileAttributeView> V getFileAttributeView(DynRoute route, Class<V> type, LinkOption... options) {
        return getIOInterface().getFileAttributeView(route, type, options);
    }

    // I/O: File Attributes, Read by Subclass of BasicFileAttributes
    <A extends BasicFileAttributes> A readAttributes(DynRoute route, Class<A> type, LinkOption... options)
            throws IOException {
        return getIOInterface().readAttributes(route, type, options);
    }

    // I/O: File Attributes, Read by String [Name of FileAttribute Set]
    Map<String, Object> readAttributes(DynRoute route, String attributes, LinkOption... options) throws IOException {
        return getIOInterface().readAttributes(route, attributes, options);
    }

    // I/O: File Attributes, Set
    void setAttribute(DynRoute route, String attribute, Object value, LinkOption... options) throws IOException {
        getIOInterface().setAttribute(route, attribute, value, options);
    }

}
