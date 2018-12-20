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

import dynfs.core.DynPath.DynPathBody;
import dynfs.core.store.DynSpaceFactory;
import dynfs.core.store.DynSpaceLoader;

public final class DynFileSystemProvider extends FileSystemProvider {

	//
	// URI

	public static final String URI_SCHEME = "dynfs";

	private static void validateUri(URI uri) {
		if (uri.getScheme() != null && !URI_SCHEME.equals(uri.getScheme()))
			throw new ProviderMismatchException("A DynFS URI must have scheme \"dynfs\" if a scheme is specified");

		if (uri.getUserInfo() != null)
			throw new IllegalArgumentException("A DynFS URI cannot contain user info");

		if (uri.getPort() >= 0)
			throw new IllegalArgumentException("A DynFS URI cannot contain a port number");
	}

	//
	// File System Resolution

	private static DynFileSystem getFileSystemFromPath(Path p) {
		FileSystem fs = p.getFileSystem();

		if (!(fs instanceof DynFileSystem))
			throw new ProviderMismatchException("File system associated with Path is not a DynFileSystem");

		if (!(fs.provider() instanceof DynFileSystemProvider))
			throw new IllegalStateException("DynFileSystem provider is not DynFileSystemProvider");

		return (DynFileSystem) fs;
	}

	//
	// Defaults

	// Environment
	private static final Map<String, ?> DEFAULT_ENV;
	static {
		Map<String, ?> env = new HashMap<>();
		DEFAULT_ENV = Collections.unmodifiableMap(env);
	}

	// Store Factory
	private static final DynSpaceFactory<? extends DynSpace> DEFAULT_STORE_FACTORY;
	static {
		// TODO: Implementation
		DEFAULT_STORE_FACTORY = null;
	}

	//
	// Managed File Systems

	private final Lock SYSTEMS_WRITE_LOCK = new ReentrantLock();
	private final Map<String, DynFileSystem> MANAGED_SYSTEMS = new ConcurrentHashMap<>();

	//
	// Store Factory

	private DynSpaceFactory<? extends DynSpace> storeFactory = DEFAULT_STORE_FACTORY;

	//
	// Instance

	public DynFileSystemProvider() {
	}

	//
	// Constant: Scheme

	@Override
	public String getScheme() {
		return URI_SCHEME;
	}

	//
	// Interface: File System Management

	// create, by domain
	public DynFileSystem newFileSystem(String domain) throws IOException {
		return newFileSystem(domain, DEFAULT_ENV);
	}

	// create, by domain, with environment
	public DynFileSystem newFileSystem(String domain, Map<String, ?> env) throws IOException {
		return newFileSystem(domain, null, env);
	}

	// create, by domain, using factory, with environment
	public DynFileSystem newFileSystem(String domain, DynSpaceFactory<? extends DynSpace> storeFactory,
			Map<String, ?> env) throws IOException {
		if (domain == null)
			throw new IllegalArgumentException("A domain must be specified");
		if (storeFactory == null)
			storeFactory = this.storeFactory;

		return __newFileSystem(domain, storeFactory, env);
	}

	// load, using loader, with load-environment and assigned domain
	public FileSystem loadFileSystem(String domain, DynSpaceLoader<? extends DynSpace> storeLoader,
			Map<String, ?> env) {
		if (domain == null)
			throw new IllegalArgumentException("A domain must be specified");
		if (storeLoader == null)
			throw new IllegalArgumentException("A DynSpaceLoader must be provided");

		return __loadFileSystem(domain, storeLoader, env);
	}

	// get managed
	@Override
	public FileSystem getFileSystem(URI uri) {
		validateUri(uri);

		String domain = uri.getHost();

		if (domain == null)
			throw new IllegalArgumentException("A domain must be specified");

		return __getFileSystem(domain, false);
	}

	// create, by URI, using default factory, with environment
	@Override
	public FileSystem newFileSystem(URI uri, Map<String, ?> env) throws IOException {
		validateUri(uri);

		String domain = uri.getHost();
		return newFileSystem(domain, env);
	}

	// TODO: Future feature? (Load FS from file)
	// load, from file
	@Override
	public FileSystem newFileSystem(Path path, Map<String, ?> env) throws IOException {
		return super.newFileSystem(path, env);
	}

	//
	// Implementation: File System Management

	public void setStoreFactory(DynSpaceFactory<? extends DynSpace> storeFactory) {
		if (storeFactory == null)
			storeFactory = DEFAULT_STORE_FACTORY;
		this.storeFactory = storeFactory;
	}

	public DynSpaceFactory<? extends DynSpace> getStoreFactory() {
		return storeFactory;
	}

	private DynFileSystem __newFileSystem(String domain, DynSpaceFactory<? extends DynSpace> storeFactory,
			Map<String, ?> env) {
		SYSTEMS_WRITE_LOCK.lock();
		try {
			if (MANAGED_SYSTEMS.containsKey(domain))
				throw new FileSystemAlreadyExistsException(
						"An open file system with domain [" + domain + "] already exists");

			DynFileSystem fs = DynFileSystem._newFileSystem(this, domain, storeFactory, env);
			MANAGED_SYSTEMS.put(domain, fs);

			return fs;
		} finally {
			SYSTEMS_WRITE_LOCK.unlock();
		}
	}

	private DynFileSystem __ensureFileSystem(String domain) throws IOException {
		SYSTEMS_WRITE_LOCK.lock();
		try {
			DynFileSystem fs = MANAGED_SYSTEMS.get(domain);

			if (fs == null) {
				fs = newFileSystem(domain);
				MANAGED_SYSTEMS.put(domain, fs);
			}

			return fs;
		} finally {
			SYSTEMS_WRITE_LOCK.unlock();
		}
	}

	private DynFileSystem __loadFileSystem(String domain, DynSpaceLoader<? extends DynSpace> storeLoader,
			Map<String, ?> env) {
		SYSTEMS_WRITE_LOCK.lock();
		try {
			if (MANAGED_SYSTEMS.containsKey(domain))
				throw new FileSystemAlreadyExistsException(
						"An open file system with domain [" + domain + "] already exists");

			DynFileSystem fs = DynFileSystem._loadFileSystem(this, domain, storeLoader, env);
			MANAGED_SYSTEMS.put(domain, fs);

			return fs;
		} finally {
			SYSTEMS_WRITE_LOCK.unlock();
		}
	}

	private DynFileSystem __getFileSystem(String domain, boolean create) {
		DynFileSystem fs = MANAGED_SYSTEMS.get(domain);

		if (fs == null) {
			if (create) {
				try {
					return __ensureFileSystem(domain);
				} catch (IOException ex) {
					throw (FileSystemNotFoundException) new FileSystemNotFoundException(
							"Could not create file system with domain [" + domain + "]").initCause(ex);
				}
			} else {
				throw new FileSystemNotFoundException("No open file system exists with domain [" + domain + "]");
			}
		}

		return fs;
	}

	@SuppressWarnings("unused")
	private boolean __decoupleFileSystem(String domain, DynFileSystem fs) {
		return MANAGED_SYSTEMS.remove(domain, fs);
	}

	//
	// Paths

	@Override
	public Path getPath(URI uri) {
		validateUri(uri);

		String domain = uri.getHost();

		if (domain == null)
			throw new IllegalArgumentException("A domain must be specified");

		DynFileSystem fs = __getFileSystem(domain, true);

		DynPath p = DynPath._newPathFromUri(fs, uri);
		return p;
	}

	//
	// File System Operations

	private static DynPathBody __getPathBody(Path path) {
		DynPath dp = (DynPath) path;
		dp = dp.toAbsolutePath();
		return dp.body();
	}

	@Override
	public SeekableByteChannel newByteChannel(Path path, Set<? extends OpenOption> options, FileAttribute<?>... attrs)
			throws IOException {
		DynFileSystem fs = getFileSystemFromPath(path);
		return fs._newByteChannel(__getPathBody(path), options);
	}

	@Override
	public DirectoryStream<Path> newDirectoryStream(Path dir, Filter<? super Path> filter) throws IOException {
		DynFileSystem fs = getFileSystemFromPath(dir);
		return fs._newDirectoryStream(__getPathBody(dir), filter);
	}

	@Override
	public void createDirectory(Path dir, FileAttribute<?>... attrs) throws IOException {
		DynFileSystem fs = getFileSystemFromPath(dir);
		fs._createDirectory(__getPathBody(dir), attrs);
	}

	@Override
	public void delete(Path path) throws IOException {
		DynFileSystem fs = getFileSystemFromPath(path);
		fs._delete(__getPathBody(path));
	}

	private void __copy(Path source, Path target, boolean deleteSource, CopyOption... options) throws IOException {
		DynFileSystem fs = getFileSystemFromPath(target);
		fs._copy(source, __getPathBody(target), deleteSource, options);
	}

	@Override
	public void copy(Path source, Path target, CopyOption... options) throws IOException {
		__copy(source, target, false, options);
	}

	@Override
	public void move(Path source, Path target, CopyOption... options) throws IOException {
		__copy(source, target, true, options);
	}

	@Override
	public boolean isSameFile(Path path1, Path path2) throws IOException {
		DynFileSystem fs = getFileSystemFromPath(path1);
		if (fs != getFileSystemFromPath(path2))
			return false;

		return fs._isSameFile(__getPathBody(path1), __getPathBody(path2));
	}

	@Override
	public boolean isHidden(Path path) throws IOException {
		DynFileSystem fs = getFileSystemFromPath(path);
		return fs._isHidden(__getPathBody(path));
	}

	@Override
	public FileStore getFileStore(Path path) throws IOException {
		DynFileSystem fs = getFileSystemFromPath(path);
		return fs.getStore();
	}

	@Override
	public void checkAccess(Path path, AccessMode... modes) throws IOException {
		DynFileSystem fs = getFileSystemFromPath(path);
		fs._checkAccess(__getPathBody(path), modes);
	}

	@Override
	public <V extends FileAttributeView> V getFileAttributeView(Path path, Class<V> type, LinkOption... options) {
		DynFileSystem fs = getFileSystemFromPath(path);
		return fs._getFileAttributeView(__getPathBody(path), type, options);
	}

	@Override
	public <A extends BasicFileAttributes> A readAttributes(Path path, Class<A> type, LinkOption... options)
			throws IOException {
		DynFileSystem fs = getFileSystemFromPath(path);
		return fs._readAttributes(__getPathBody(path), type, options);
	}

	@Override
	public Map<String, Object> readAttributes(Path path, String attributes, LinkOption... options) throws IOException {
		DynFileSystem fs = getFileSystemFromPath(path);
		return fs._readAttributes(__getPathBody(path), attributes, options);
	}

	@Override
	public void setAttribute(Path path, String attribute, Object value, LinkOption... options) throws IOException {
		DynFileSystem fs = getFileSystemFromPath(path);
		fs._setAttribute(__getPathBody(path), attribute, value, options);
	}

}
