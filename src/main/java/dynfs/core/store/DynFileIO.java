package dynfs.core.store;

import java.io.IOException;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.AccessMode;
import java.nio.file.CopyOption;
import java.nio.file.DirectoryStream;
import java.nio.file.DirectoryStream.Filter;
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.FileAttributeView;
import java.util.Map;
import java.util.Set;

import dynfs.core.DynPath.DynPathBody;

public interface DynFileIO {

	//
	// Byte Channel

	public SeekableByteChannel newByteChannel(DynPathBody path, Set<? extends OpenOption> options,
			FileAttribute<?>... attrs) throws IOException;

	//
	// Directory Operations

	public DirectoryStream<Path> newDirectoryStream(DynPathBody dir, Filter<? super Path> filter) throws IOException;

	public void createDirectory(DynPathBody dir, FileAttribute<?>... attrs) throws IOException;

	//
	// File Operations

	public void delete(DynPathBody path) throws IOException;

	public void copy(Path source, DynPathBody target, boolean deleteSource, CopyOption... options) throws IOException;

	public boolean isSameFile(DynPathBody path1, DynPathBody path2) throws IOException;

	//
	// Hidden Files

	public boolean isHidden(DynPathBody path) throws IOException;

	//
	// Access Control

	public void checkAccess(DynPathBody path, AccessMode... modes) throws IOException;

	//
	// File Attributes

	public <V extends FileAttributeView> V getFileAttributeView(DynPathBody path, Class<V> type, LinkOption... options);

	public <A extends BasicFileAttributes> A readAttributes(DynPathBody path, Class<A> type, LinkOption... options)
			throws IOException;

	public Map<String, Object> readAttributes(DynPathBody path, String attributes, LinkOption... options)
			throws IOException;

	public void setAttribute(DynPathBody path, String attribute, Object value, LinkOption... options)
			throws IOException;

}
