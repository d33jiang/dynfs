package dynfs.core;

import java.io.IOException;
import java.net.URI;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.AccessMode;
import java.nio.file.CopyOption;
import java.nio.file.DirectoryStream;
import java.nio.file.DirectoryStream.Filter;
import java.nio.file.FileStore;
import java.nio.file.FileSystem;
import java.nio.file.FileSystemAlreadyExistsException;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.ProviderMismatchException;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.FileAttributeView;
import java.nio.file.spi.FileSystemProvider;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import dynfs.core.options.AccessModes;
import dynfs.core.options.CopyOptions;
import dynfs.core.options.LinkOptions;
import dynfs.core.options.OpenOptions;
import dynfs.core.store.DynSpaceFactory;
import dynfs.core.store.DynSpaceLoader;

public final class DynFileSystemProvider extends FileSystemProvider {

    // TODO: Attribute I/O

    // TODO: Atomic I/O

    // TODO: Attribute I/O - Reads / writes update DynFile times (See
    // DynNode.touchByRead and .touchByWrite)

    // FUTURE: Access Control - Centralized (AccessControlClass).checkRead(),
    // .checkCopy(), .checkWrite(), etc.
    // -> Beware of read-only DynSpace instances

    // TODO: Code Base Style - Explore Nullable, NonNull, etc. annotations; very
    // likely not retained during runtime; might be retained beyond compilation?

    // TODO: Documentation - Javadocs (project-wide)
    // TODO: API Adherence - NIO Interface Specification Adherence (project-wide)

    // TODO: Testing - Invariance-Based Tests for Small-Scale Systems
    // TODO: Testing - Unit Tests

    // TODO: Coverage

    //
    // Constant: URI Scheme

    public static final String URI_SCHEME = "dynfs";

    @Override
    public String getScheme() {
        return URI_SCHEME;
    }

    //
    // State: Managed DynFileSystem Instances

    private final Lock managedSystemsWriteLock = new ReentrantLock();
    private final Map<String, DynFileSystem<?>> managedSystems = new ConcurrentHashMap<>();

    //
    // Construction: Unmanaged Singleton

    public DynFileSystemProvider() {}

    //
    // Static Support: URI Validation

    private static void validateUri(URI uri) {
        if (uri == null)
            throw new NullPointerException("uri is null");

        if (uri.getScheme() != null && !URI_SCHEME.equals(uri.getScheme()))
            throw new ProviderMismatchException("A DynFS URI must have scheme \"dynfs\" if a scheme is specified");

        if (uri.getUserInfo() != null)
            throw new IllegalArgumentException("A DynFS URI cannot contain user info");

        if (uri.getPort() >= 0)
            throw new IllegalArgumentException("A DynFS URI cannot contain a port number");
    }

    //
    // Static Support: File System Resolution

    private static DynFileSystem<?> getFileSystemFromPath(Path p) {
        if (p == null)
            throw new NullPointerException("Path is null");

        FileSystem fs = p.getFileSystem();
        validateFileSystem(fs);
        return (DynFileSystem<?>) fs;
    }

    private static void validateFileSystem(FileSystem fs) {
        if (fs == null)
            throw new NullPointerException("File system associated with Path is null");

        if (!(fs instanceof DynFileSystem))
            throw new ProviderMismatchException("File system associated with Path is not a DynFileSystem");

        if (!(fs.provider() instanceof DynFileSystemProvider))
            // This should never be reached given that the previous check passes
            throw new IllegalStateException("DynFileSystem provider is not DynFileSystemProvider");
    }

    //
    // Static Support: DynRoute Resolution

    private static DynRoute getDynRoute(Path path) {
        // It is assumed that (path instanceof DynPath) since Path arguments to the
        // methods in this class must be associated with DynFileSystemProvider.
        DynPath dp = (DynPath) path;
        dp = dp.toAbsolutePath();
        return dp.route();
    }

    //
    // Interface: DynPath Creation

    @Override
    public DynPath getPath(URI uri) {
        validateUri(uri);

        String domain = uri.getHost();
        if (domain == null)
            throw new IllegalArgumentException("A domain must be specified");

        DynFileSystem<?> fs = getFileSystemImpl(domain);
        return DynPath.newPathFromUri(fs, uri);
    }

    //
    // Interface: DynFileSystem Creation

    // by URI, with factory from provided environment
    @Override
    public DynFileSystem<?> newFileSystem(URI uri, Map<String, ?> env) throws IOException {
        validateUri(uri);

        String domain = uri.getHost();
        return newFileSystem(domain, env);
    }

    // by domain, with factory from provided environment
    public DynFileSystem<?> newFileSystem(String domain, Map<String, ?> env) throws IOException {
        return newFileSystem(domain, (DynSpaceFactory<?>) env.get("storeFactory"), env);
    }

    // by domain, with factory from explicit parameter
    public <Space extends DynSpace<Space>> DynFileSystem<Space> newFileSystem(String domain,
            DynSpaceFactory<Space> storeFactory, Map<String, ?> env)
            throws IOException {
        if (domain == null)
            throw new NullPointerException("domain is null");
        if (domain.isEmpty())
            throw new IllegalArgumentException("A domain must be specified");
        if (storeFactory == null)
            throw new NullPointerException("storeFactory is null");

        return newFileSystemImpl(domain, storeFactory, env);
    }

    private <Space extends DynSpace<Space>> DynFileSystem<Space> newFileSystemImpl(String domain,
            DynSpaceFactory<Space> storeFactory, Map<String, ?> env) throws IOException {
        managedSystemsWriteLock.lock();
        try {
            if (managedSystems.containsKey(domain)) {
                throw new FileSystemAlreadyExistsException(
                        "An open file system with domain [" + domain + "] already exists");
            }

            DynFileSystem<Space> fs = DynFileSystem.newFileSystem(this, domain, storeFactory, env);
            managedSystems.put(domain, fs);

            return fs;
        } finally {
            managedSystemsWriteLock.unlock();
        }
    }

    //
    // Interface: DynFileSystem Access

    @Override
    public DynFileSystem<?> getFileSystem(URI uri) {
        validateUri(uri);

        String domain = uri.getHost();
        if (domain == null)
            throw new NullPointerException("domain is null");
        if (domain.isEmpty())
            throw new IllegalArgumentException("A domain must be specified");

        return getFileSystemImpl(domain);
    }

    public DynFileSystem<?> getFileSystem(String domain) {
        DynFileSystem<?> fs = getFileSystemImpl(domain);
        if (fs == null) {
            throw new FileSystemNotFoundException("No open file system exists with domain [" + domain + "]");
        } else {
            return fs;
        }
    }

    private DynFileSystem<?> getFileSystemImpl(String domain) {
        return managedSystems.get(domain);
    }

    //
    // Interface: DynFileSystem Loading

    // from provided loader
    public <Space extends DynSpace<Space>> DynFileSystem<Space> loadFileSystem(String domain,
            DynSpaceLoader<Space> storeLoader) throws IOException {
        if (domain == null)
            throw new IllegalArgumentException("A domain must be specified");
        if (storeLoader == null)
            throw new IllegalArgumentException("A DynSpaceLoader must be provided");

        return loadFileSystemImpl(domain, storeLoader);
    }

    private <Space extends DynSpace<Space>> DynFileSystem<Space> loadFileSystemImpl(String domain,
            DynSpaceLoader<Space> storeLoader) throws IOException {
        managedSystemsWriteLock.lock();
        try {
            if (managedSystems.containsKey(domain)) {
                throw new FileSystemAlreadyExistsException(
                        "An open file system with domain [" + domain + "] already exists");
            }

            DynFileSystem<Space> fs = DynFileSystem.loadFileSystem(this, domain, storeLoader);
            managedSystems.put(domain, fs);

            return fs;
        } finally {
            managedSystemsWriteLock.unlock();
        }
    }

    // from file
    @Override
    public FileSystem newFileSystem(Path path, Map<String, ?> env) throws IOException {
        // FUTURE: Loading / Saving - Load FS from file?
        return super.newFileSystem(path, env);
    }

    //
    // Interface Implementation: File System Decoupling (Unused)

    @SuppressWarnings("unused")
    @Deprecated
    private boolean __decoupleFileSystem(String domain, DynFileSystem<?> fs) {
        managedSystemsWriteLock.lock();
        try {
            return managedSystems.remove(domain, fs);
        } finally {
            managedSystemsWriteLock.unlock();
        }
    }

    //
    // Interface: File System Operations
    // Delegation to DynFileSystemProviderIO

    @Override
    public SeekableByteChannel newByteChannel(Path path, Set<? extends OpenOption> options, FileAttribute<?>... attrs)
            throws IOException {
        return DynFileSystemProviderIO.newByteChannel(getFileSystemFromPath(path), getDynRoute(path),
                OpenOptions.parse(options),
                attrs);
    }

    @Override
    public DirectoryStream<Path> newDirectoryStream(Path dir, Filter<? super Path> filter) throws IOException {
        return DynFileSystemProviderIO.newDirectoryStream(getFileSystemFromPath(dir), getDynRoute(dir), filter);
    }

    @Override
    public void createDirectory(Path dir, FileAttribute<?>... attrs) throws IOException {
        DynFileSystemProviderIO.createDirectory(getFileSystemFromPath(dir), getDynRoute(dir), attrs);
    }

    @Override
    public void delete(Path path) throws IOException {
        DynFileSystemProviderIO.delete(getFileSystemFromPath(path), getDynRoute(path));
    }

    @Override
    public void copy(Path src, Path dst, CopyOption... options) throws IOException {
        DynFileSystemProviderIO.copy(getFileSystemFromPath(src), getFileSystemFromPath(dst), getDynRoute(src),
                getDynRoute(dst),
                CopyOptions.parse(options));
    }

    @Override
    public void move(Path src, Path dst, CopyOption... options) throws IOException {
        DynFileSystemProviderIO.move(getFileSystemFromPath(src), getFileSystemFromPath(dst), getDynRoute(src),
                getDynRoute(dst),
                CopyOptions.parse(options));
    }

    @Override
    public boolean isSameFile(Path path1, Path path2) throws IOException {
        DynFileSystem<?> fs = getFileSystemFromPath(path1);
        if (!fs.equals(getFileSystemFromPath(path2)))
            return false;

        return DynFileSystemProviderIO.isSameFile(fs, getDynRoute(path1), getDynRoute(path2));
    }

    @Override
    public boolean isHidden(Path path) throws IOException {
        return DynFileSystemProviderIO.isHidden(getFileSystemFromPath(path), getDynRoute(path));
    }

    @Override
    public FileStore getFileStore(Path path) throws IOException {
        return getFileSystemFromPath(path).getStore();
    }

    @Override
    public void checkAccess(Path path, AccessMode... modes) throws IOException {
        DynFileSystemProviderIO.checkAccess(getFileSystemFromPath(path), getDynRoute(path), AccessModes.parse(modes));
    }

    @Override
    public <V extends FileAttributeView> V getFileAttributeView(Path path, Class<V> type, LinkOption... options) {
        return DynFileSystemProviderIO.getFileAttributeView(getFileSystemFromPath(path), getDynRoute(path), type,
                LinkOptions.parse(options));
    }

    @Override
    public <A extends BasicFileAttributes> A readAttributes(Path path, Class<A> type, LinkOption... options)
            throws IOException {
        return DynFileSystemProviderIO.readAttributes(getFileSystemFromPath(path), getDynRoute(path), type,
                LinkOptions.parse(options));
    }

    @Override
    public Map<String, Object> readAttributes(Path path, String attributes, LinkOption... options) throws IOException {
        return DynFileSystemProviderIO.readAttributes(getFileSystemFromPath(path), getDynRoute(path), attributes,
                LinkOptions.parse(options));
    }

    @Override
    public void setAttribute(Path path, String attribute, Object value, LinkOption... options) throws IOException {
        DynFileSystemProviderIO.setAttribute(getFileSystemFromPath(path), getDynRoute(path), attribute, value,
                LinkOptions.parse(options));
    }

}
