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
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import dynfs.core.path.DynPath;
import dynfs.core.path.DynRoute;
import dynfs.core.store.DynSpaceFactory;
import dynfs.core.store.DynSpaceLoader;

public final class DynFileSystemProvider extends FileSystemProvider {

    //
    // Instance

    public DynFileSystemProvider() {}

    //
    // File System Resolution

    private static void validateFileSystem(FileSystem fs) {
        if (fs == null)
            throw new NullPointerException("File system associated with Path is null");

        if (!(fs instanceof DynFileSystem))
            throw new ProviderMismatchException("File system associated with Path is not a DynFileSystem");

        if (!(fs.provider() instanceof DynFileSystemProvider))
            throw new IllegalStateException("DynFileSystem provider is not DynFileSystemProvider");
    }

    private static DynFileSystem<?> getFileSystemFromPath(Path p) {
        if (p == null)
            throw new NullPointerException("Path is null");

        FileSystem fs = p.getFileSystem();
        validateFileSystem(fs);
        return (DynFileSystem<?>) fs;
    }

    //
    // URI Resolution

    public static final String URI_SCHEME = "dynfs";

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

    @Override
    public String getScheme() {
        return URI_SCHEME;
    }

    @Override
    public Path getPath(URI uri) {
        validateUri(uri);

        String domain = uri.getHost();
        if (domain == null)
            throw new IllegalArgumentException("A domain must be specified");

        DynFileSystem<?> fs = getFileSystemImpl(domain, true);

        DynPath p = DynPath.newPathFromUri(fs, uri);
        return p;
    }

    //
    // File System Management: State

    private final Lock managedSystemsWriteLock = new ReentrantLock();
    private final Map<String, DynFileSystem<?>> managedSystems = new ConcurrentHashMap<>();

    //
    // File System Creation: Defaults

    private static final DynSpaceFactory<?> DEFAULT_STORE_FACTORY;
    static {
        DEFAULT_STORE_FACTORY = null; // TODO: Implementation
    }

    private static final Map<String, ?> DEFAULT_ENV;
    static {
        Map<String, ?> env = new HashMap<>();
        DEFAULT_ENV = Collections.unmodifiableMap(env);
    }

    private DynSpaceFactory<?> storeFactory = DEFAULT_STORE_FACTORY;

    public DynSpaceFactory<?> getStoreFactory() {
        return storeFactory;
    }

    public void setStoreFactory(DynSpaceFactory<?> storeFactory) {
        if (storeFactory == null)
            storeFactory = DEFAULT_STORE_FACTORY;

        this.storeFactory = storeFactory;
    }

    //
    // Implementation: File System Creation

    // by URI, using existing factory, with provided environment
    @Override
    public DynFileSystem<?> newFileSystem(URI uri, Map<String, ?> env) throws IOException {
        validateUri(uri);

        String domain = uri.getHost();
        return newFileSystem(domain, env);
    }

    // by domain, using existing factory, with default environment
    public DynFileSystem<?> newFileSystem(String domain) throws IOException {
        return newFileSystem(domain, DEFAULT_ENV);
    }

    // by domain, using existing factory, with provided environment
    public DynFileSystem<?> newFileSystem(String domain, Map<String, ?> env) throws IOException {
        return newFileSystem(domain, null, env);
    }

    // by domain, using provided factory, with provided environment
    public DynFileSystem<?> newFileSystem(String domain, DynSpaceFactory<?> storeFactory, Map<String, ?> env)
            throws IOException {
        if (domain == null) {
            throw new IllegalArgumentException("A domain must be specified");
        }

        if (storeFactory == null) {
            storeFactory = this.storeFactory;
        }

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
    // Implementation: File System Access

    @Override
    public DynFileSystem<?> getFileSystem(URI uri) {
        validateUri(uri);

        String domain = uri.getHost();
        if (domain == null)
            throw new IllegalArgumentException("A domain must be specified");

        return getFileSystemImpl(domain, false);
    }

    private DynFileSystem<?> getFileSystemImpl(String domain, boolean createIfNonExistent) {
        if (createIfNonExistent) {
            try {
                return ensureFileSystem(domain);
            } catch (IOException ex) {
                throw (FileSystemNotFoundException) new FileSystemNotFoundException(
                        "Could not create file system with domain [" + domain + "]").initCause(ex);
            }
        } else {
            DynFileSystem<?> fs = managedSystems.get(domain);
            if (fs == null) {
                throw new FileSystemNotFoundException("No open file system exists with domain [" + domain + "]");
            } else {
                return fs;
            }
        }
    }

    private DynFileSystem<?> ensureFileSystem(String domain) throws IOException {
        managedSystemsWriteLock.lock();
        try {
            DynFileSystem<?> fs = managedSystems.get(domain);

            if (fs == null) {
                fs = newFileSystem(domain);
                managedSystems.put(domain, fs);
            }

            return fs;
        } finally {
            managedSystemsWriteLock.unlock();
        }
    }

    //
    // Implementation: File System Loading

    // provided domain, using provided loader, with provided environment
    public <Space extends DynSpace<Space>> DynFileSystem<Space> loadFileSystem(String domain,
            DynSpaceLoader<Space> storeLoader, Map<String, ?> env) throws IOException {
        if (domain == null)
            throw new IllegalArgumentException("A domain must be specified");
        if (storeLoader == null)
            throw new IllegalArgumentException("A DynSpaceLoader must be provided");

        return loadFileSystemImpl(domain, storeLoader, env);
    }

    private <Space extends DynSpace<Space>> DynFileSystem<Space> loadFileSystemImpl(String domain,
            DynSpaceLoader<Space> storeLoader, Map<String, ?> env) throws IOException {
        managedSystemsWriteLock.lock();
        try {
            if (managedSystems.containsKey(domain)) {
                throw new FileSystemAlreadyExistsException(
                        "An open file system with domain [" + domain + "] already exists");
            }

            DynFileSystem<Space> fs = DynFileSystem.loadFileSystem(this, domain, storeLoader, env);
            managedSystems.put(domain, fs);

            return fs;
        } finally {
            managedSystemsWriteLock.unlock();
        }
    }

    // from file
    @Override
    public FileSystem newFileSystem(Path path, Map<String, ?> env) throws IOException {
        // TODO: Future feature? (Load FS from file)
        return super.newFileSystem(path, env);
    }

    //
    // Implementation: File System Decoupling

    @SuppressWarnings("unused")
    private boolean __decoupleFileSystem(String domain, DynFileSystem<?> fs) {
        return managedSystems.remove(domain, fs);
    }

    //
    // Interface Delegation: File System Operations
    //
    // DynFileSystemProvider and DynFileSystem are standardized. File system
    // operation implementations are DynSpace-dependent. FS-identifying elements are
    // stripped from DynPath inputs and resulting DynRoute instances are forwarded
    // to DynFileSystem and DynSpace implementations.
    //

    private static DynRoute getDynRoute(Path path) {
        DynPath dp = (DynPath) path;
        dp = dp.toAbsolutePath();
        return dp.route();
    }

    @Override
    public SeekableByteChannel newByteChannel(Path path, Set<? extends OpenOption> options, FileAttribute<?>... attrs)
            throws IOException {
        DynFileSystem<?> fs = getFileSystemFromPath(path);
        return fs.newByteChannel(getDynRoute(path), options, attrs);
    }

    @Override
    public DirectoryStream<Path> newDirectoryStream(Path dir, Filter<? super Path> filter) throws IOException {
        DynFileSystem<?> fs = getFileSystemFromPath(dir);
        return fs.newDirectoryStream(getDynRoute(dir), filter);
    }

    @Override
    public void createDirectory(Path dir, FileAttribute<?>... attrs) throws IOException {
        DynFileSystem<?> fs = getFileSystemFromPath(dir);
        fs.createDirectory(getDynRoute(dir), attrs);
    }

    @Override
    public void delete(Path path) throws IOException {
        DynFileSystem<?> fs = getFileSystemFromPath(path);
        fs.delete(getDynRoute(path));
    }

    @Override
    public void copy(Path source, Path target, CopyOption... options) throws IOException {
        DynFileSystem<?> fsSrc = getFileSystemFromPath(source);
        DynFileSystem<?> fsDst = getFileSystemFromPath(target);
        if (fsSrc == fsDst) {
            fsSrc.copy(getDynRoute(source), getDynRoute(target), options);
        } else {
            DynFileSystemGeneralCopier.copy(fsSrc, fsDst, getDynRoute(source), getDynRoute(target), options);
        }
    }

    @Override
    public void move(Path source, Path target, CopyOption... options) throws IOException {
        DynFileSystem<?> fsSrc = getFileSystemFromPath(source);
        DynFileSystem<?> fsDst = getFileSystemFromPath(target);
        if (fsSrc == fsDst) {
            fsSrc.move(getDynRoute(source), getDynRoute(target), options);
        } else {
            DynFileSystemGeneralCopier.move(fsSrc, fsDst, getDynRoute(source), getDynRoute(target), options);
        }
    }

    @Override
    public boolean isSameFile(Path path1, Path path2) throws IOException {
        DynFileSystem<?> fs = getFileSystemFromPath(path1);
        if (!fs.equals(getFileSystemFromPath(path2)))
            return false;

        return fs.isSameFile(getDynRoute(path1), getDynRoute(path2));
    }

    @Override
    public boolean isHidden(Path path) throws IOException {
        DynFileSystem<?> fs = getFileSystemFromPath(path);
        return fs.isHidden(getDynRoute(path));
    }

    @Override
    public FileStore getFileStore(Path path) throws IOException {
        DynFileSystem<?> fs = getFileSystemFromPath(path);
        return fs.getStore();
    }

    @Override
    public void checkAccess(Path path, AccessMode... modes) throws IOException {
        DynFileSystem<?> fs = getFileSystemFromPath(path);
        fs.checkAccess(getDynRoute(path), modes);
    }

    @Override
    public <V extends FileAttributeView> V getFileAttributeView(Path path, Class<V> type, LinkOption... options) {
        DynFileSystem<?> fs = getFileSystemFromPath(path);
        return fs.getFileAttributeView(getDynRoute(path), type, options);
    }

    @Override
    public <A extends BasicFileAttributes> A readAttributes(Path path, Class<A> type, LinkOption... options)
            throws IOException {
        DynFileSystem<?> fs = getFileSystemFromPath(path);
        return fs.readAttributes(getDynRoute(path), type, options);
    }

    @Override
    public Map<String, Object> readAttributes(Path path, String attributes, LinkOption... options) throws IOException {
        DynFileSystem<?> fs = getFileSystemFromPath(path);
        return fs.readAttributes(getDynRoute(path), attributes, options);
    }

    @Override
    public void setAttribute(Path path, String attribute, Object value, LinkOption... options) throws IOException {
        DynFileSystem<?> fs = getFileSystemFromPath(path);
        fs.setAttribute(getDynRoute(path), attribute, value, options);
    }

}
