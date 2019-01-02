package dynfs.core;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.DirectoryStream;
import java.nio.file.DirectoryStream.Filter;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileStore;
import java.nio.file.FileSystem;
import java.nio.file.NotDirectoryException;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.WatchEvent.Kind;
import java.nio.file.WatchEvent.Modifier;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.UserPrincipalLookupService;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import dynfs.core.io.DynByteChannel;
import dynfs.core.io.DynDirectoryStream;
import dynfs.core.options.OpenOptions;
import dynfs.core.path.DynPath;
import dynfs.core.path.DynRoute;
import dynfs.core.store.DynSpaceFactory;
import dynfs.core.store.DynSpaceLoader;

public final class DynFileSystem<Space extends DynSpace<Space>> extends FileSystem {

    //
    // Configuration: File System Provider

    private final DynFileSystemProvider provider;

    @Override
    public DynFileSystemProvider provider() {
        return provider;
    }

    //
    // Configuration: Domain

    private final String domain;

    public String domain() {
        return domain;
    }

    //
    // Configuration: DynStore

    private final Space store;

    public Space getStore() {
        return store;
    }

    //
    // State: Status

    private static final Status INITIAL_STATUS = Status.ACTIVE;

    private final Lock STATUS_LOCK = new ReentrantLock();
    private Status status = INITIAL_STATUS;

    public static enum Status {
        ACTIVE, TERMINATION, CLOSED, ERROR_ON_CLOSE;

        public boolean isOpen() {
            return !isClosed();
        }

        public boolean isClosed() {
            return this == Status.TERMINATION || this == CLOSED;
        }
    }

    public Status status() {
        return status;
    }

    @Override
    public boolean isOpen() {
        return status.isOpen();
    }

    @Override
    public void close() throws IOException {
        if (status != Status.CLOSED) {
            STATUS_LOCK.lock();
            try {
                if (status != Status.CLOSED) {
                    closeImpl();
                }
            } finally {
                STATUS_LOCK.unlock();
            }
        }
    }

    private void closeImpl() throws IOException {
        status = Status.TERMINATION;

        try {
            getStore().close();
        } catch (IOException | RuntimeException ex) {
            // TODO: Design Review - Should this distinction be made?
            status = Status.ERROR_ON_CLOSE;
        } finally {
            if (status == Status.TERMINATION) {
                status = Status.CLOSED;
            }
        }
    }

    //
    // Construction: Factory

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
            String domain, DynSpaceLoader<? extends Space> storeLoader) throws IOException {
        Space store = storeLoader.loadStore();
        return new DynFileSystem<>(provider, domain, store);
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
    // Interface: Supported File Attributes

    @Override
    public Set<String> supportedFileAttributeViews() {
        return store.supportedFileAttributeViewsByName();
    }

    //
    // Interface: UserPrincipal Support (Unsupported)

    @Override
    public UserPrincipalLookupService getUserPrincipalLookupService() {
        // NOTE: Future feature?
        throw new UnsupportedOperationException("Users and groups are not supported");
    }

    //
    // Interface: WatchService Support (Unsupported)

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
    // Interface: Route Resolution

    public final ResolutionResult<Space> resolve(DynRoute route) throws IOException {
        return getStore().resolve(route);
    }

    public final ResolutionResult<Space> resolve(DynRoute route, boolean followLinks) throws IOException {
        return getStore().resolve(route, followLinks);
    }

    //
    // Interface Implementation: DynFileSystemProvider I/O

    // TODO: Move this section into DynFileSystemProviderIO

    // Byte Channel
    public SeekableByteChannel newByteChannel(DynRoute route,
            OpenOptions openOptions,
            FileAttribute<?>... attrs)
            throws IOException {
        // TODO: Check access control

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
            file = ((DynDirectory<Space, ?>) resolution.node()).createFileImpl(route.getFileName(),
                    attrs);
        }

        return new DynByteChannel<>(file, openOptions);
    }

    // Directory Stream
    public DirectoryStream<Path> newDirectoryStream(DynRoute dir, Filter<? super Path> filter) throws IOException {
        DynNode<Space, ?> node = store.resolve(dir).testExistence();
        if (!(node instanceof DynDirectory))
            throw new NotDirectoryException(dir.toString());

        return new DynDirectoryStream<Space>((DynDirectory<Space, ?>) node, filter);
    }

}
