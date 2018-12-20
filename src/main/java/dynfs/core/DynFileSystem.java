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

import dynfs.core.DynPath.DynPathBody;
import dynfs.core.store.DynFileIO;
import dynfs.core.store.DynSpaceFactory;
import dynfs.core.store.DynSpaceLoader;

public class DynFileSystem extends FileSystem {

	//
	// File System Provider

	private final DynFileSystemProvider provider;

	@Override
	public DynFileSystemProvider provider() {
		return provider;
	}

	//
	// Domain

	private final String domain;

	public String domain() {
		return domain;
	}

	//
	// Status

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
	// File Store

	private final DynSpace store;

	public DynSpace getStore() {
		return store;
	}

	//
	// Construction

	private DynFileSystem(DynFileSystemProvider provider, String domain, DynSpace store) {
		this.provider = provider;
		this.domain = domain;
		this.store = store;
	}

	static DynFileSystem _newFileSystem(DynFileSystemProvider provider, String domain,
			DynSpaceFactory<? extends DynSpace> storeFactory, Map<String, ?> env) {
		DynSpace store = storeFactory.createStore(env);
		return new DynFileSystem(provider, domain, store);
	}

	static DynFileSystem _loadFileSystem(DynFileSystemProvider provider, String domain,
			DynSpaceLoader<? extends DynSpace> storeLoader, Map<String, ?> env) {
		DynSpace store = storeLoader.loadStore(env);
		return new DynFileSystem(provider, domain, store);
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
					__close();
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
		return DynPath._newPath(this, first, more);
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
		return store.supportedFileAttributeViews();
	}

	//
	// User Principals (Unsupported)

	@Override
	public UserPrincipalLookupService getUserPrincipalLookupService() {
		// NOTE: Future feature?
		throw new UnsupportedOperationException("Users and groups are not supported");
	}

	//
	// Close

	private void __close() throws IOException {
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
	// Internal: Status

	private void __throwIfClosed() {
		if (status.isClosed())
			throw new ClosedFileSystemException();
	}

	private void __lockStatusLock() {
		while (status.isOpen()) {
			try {
				STATUS_LOCK.lockInterruptibly();
				break;
			} catch (InterruptedException ex) {
			}
		}
	}

	private void __unlockStatusLock() {
		STATUS_LOCK.unlock();
	}

	//
	// I/O

	private DynFileIO getIOInterface() {
		return getStore().getIOInterface();
	}

	//
	// I/O: Watch Services (Unsupported)

	@Override
	public WatchService newWatchService() throws IOException {
		// NOTE: Future feature?
		throw new UnsupportedOperationException("Watch services are not supported");
	}

	WatchKey _register(WatchService watcher, Kind<?>[] events) throws IOException {
		// NOTE: Future feature?
		throw new UnsupportedOperationException("Watch services are not supported");
	}

	WatchKey _register(WatchService watcher, Kind<?>[] events, Modifier[] modifiers) throws IOException {
		// NOTE: Future feature?
		throw new UnsupportedOperationException("Watch services are not supported");
	}

	//
	// I/O: Byte Channel

	protected SeekableByteChannel _newByteChannel(DynPathBody path, Set<? extends OpenOption> options,
			FileAttribute<?>... attrs) throws IOException {
		__lockStatusLock();
		try {
			__throwIfClosed();
			return getIOInterface().newByteChannel(path, options, attrs);
		} finally {
			__unlockStatusLock();
		}
	}

	//
	// I/O: Directory Operations

	protected DirectoryStream<Path> _newDirectoryStream(DynPathBody dir, Filter<? super Path> filter)
			throws IOException {
		__lockStatusLock();
		try {
			__throwIfClosed();
			return getIOInterface().newDirectoryStream(dir, filter);
		} finally {
			__unlockStatusLock();
		}
	}

	protected void _createDirectory(DynPathBody dir, FileAttribute<?>... attrs) throws IOException {
		__lockStatusLock();
		try {
			__throwIfClosed();
			getIOInterface().createDirectory(dir, attrs);
		} finally {
			__unlockStatusLock();
		}
	}

	// I/O: File Operations

	protected void _delete(DynPathBody path) throws IOException {
		__lockStatusLock();
		try {
			__throwIfClosed();
			getIOInterface().delete(path);
		} finally {
			__unlockStatusLock();
		}
	}

	protected void _copy(Path source, DynPathBody target, boolean deleteSource, CopyOption... options)
			throws IOException {
		__lockStatusLock();
		try {
			__throwIfClosed();
			getIOInterface().copy(source, target, deleteSource, options);
		} finally {
			__unlockStatusLock();
		}
	}

	protected boolean _isSameFile(DynPathBody path1, DynPathBody path2) throws IOException {
		__lockStatusLock();
		try {
			__throwIfClosed();
			return getIOInterface().isSameFile(path1, path2);
		} finally {
			__unlockStatusLock();
		}
	}

	//
	// I/O: Hidden Files

	protected boolean _isHidden(DynPathBody path) throws IOException {
		__lockStatusLock();
		try {
			__throwIfClosed();
			return getIOInterface().isHidden(path);
		} finally {
			__unlockStatusLock();
		}
	}

	//
	// I/O: Access Control

	protected void _checkAccess(DynPathBody path, AccessMode... modes) throws IOException {
		__lockStatusLock();
		try {
			__throwIfClosed();
			getIOInterface().checkAccess(path, modes);
		} finally {
			__unlockStatusLock();
		}
	}

	// I/O: File Attributes

	protected <V extends FileAttributeView> V _getFileAttributeView(DynPathBody path, Class<V> type,
			LinkOption... options) {
		__lockStatusLock();
		try {
			__throwIfClosed();
			return getIOInterface().getFileAttributeView(path, type, options);
		} finally {
			__unlockStatusLock();
		}
	}

	protected <A extends BasicFileAttributes> A _readAttributes(DynPathBody path, Class<A> type, LinkOption... options)
			throws IOException {
		__lockStatusLock();
		try {
			__throwIfClosed();
			return getIOInterface().readAttributes(path, type, options);
		} finally {
			__unlockStatusLock();
		}
	}

	protected Map<String, Object> _readAttributes(DynPathBody path, String attributes, LinkOption... options)
			throws IOException {
		__lockStatusLock();
		try {
			__throwIfClosed();
			return getIOInterface().readAttributes(path, attributes, options);
		} finally {
			__unlockStatusLock();
		}
	}

	protected void _setAttribute(DynPathBody path, String attribute, Object value, LinkOption... options)
			throws IOException {
		__lockStatusLock();
		try {
			__throwIfClosed();
			getIOInterface().setAttribute(path, attribute, value, options);
		} finally {
			__unlockStatusLock();
		}
	}

}
