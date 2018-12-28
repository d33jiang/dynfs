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
import java.nio.file.spi.FileSystemProvider;
import java.util.Map;
import java.util.Set;

import dynfs.core.path.DynRoute;

public interface DynFileIO {

    //
    // Existence Query

    public boolean exists(DynRoute route, boolean nofollowLinks) throws IOException;

    //
    // Byte Channel

    /**
     * @see FileSystemProvider#newByteChannel(Path, Set, FileAttribute...)
     */
    public SeekableByteChannel newByteChannel(DynRoute route, Set<? extends OpenOption> options,
            FileAttribute<?>... attrs) throws IOException;

    //
    // Directory Operations

    /**
     * @see FileSystemProvider#newDirectoryStream(Path, Filter)
     */
    public DirectoryStream<Path> newDirectoryStream(DynRoute dir, Filter<? super Path> filter) throws IOException;

    /**
     * @see FileSystemProvider#createDirectory(Path, FileAttribute...)
     */
    public void createDirectory(DynRoute dir, FileAttribute<?>... attrs) throws IOException;

    //
    // File Operations

    /**
     * @see FileSystemProvider#delete(Path)
     */
    public void delete(DynRoute route) throws IOException;

    /**
     * @see FileSystemProvider#copy(Path, Path, CopyOption...)
     */
    public void copy(DynRoute source, DynRoute target, CopyOption... options) throws IOException;

    /**
     * @see FileSystemProvider#move(Path, Path, CopyOption...)
     */
    public void move(DynRoute source, DynRoute target, CopyOption... options) throws IOException;

    /**
     * @see FileSystemProvider#isSameFile(Path, Path)
     */
    public boolean isSameFile(DynRoute route1, DynRoute route2) throws IOException;

    //
    // Hidden Files

    /**
     * @see FileSystemProvider#isHidden(Path)
     */
    public boolean isHidden(DynRoute route) throws IOException;

    //
    // Access Control

    /**
     * @see FileSystemProvider#checkAccess(Path, AccessMode...)
     */
    public void checkAccess(DynRoute route, AccessMode... modes) throws IOException;

    //
    // File Attributes

    /**
     * @see FileSystemProvider#getFileAttributeView(Path, Class, LinkOption...)
     */
    public <V extends FileAttributeView> V getFileAttributeView(DynRoute route, Class<V> type, LinkOption... options);

    /**
     * @see FileSystemProvider#readAttributes(Path, Class, LinkOption...)
     */
    public <A extends BasicFileAttributes> A readAttributes(DynRoute route, Class<A> type, LinkOption... options)
            throws IOException;

    /**
     * @see FileSystemProvider#readAttributes(Path, String, LinkOption...)
     */
    public Map<String, Object> readAttributes(DynRoute route, String attributes, LinkOption... options)
            throws IOException;

    /**
     * @see FileSystemProvider#setAttribute(Path, String, Object, LinkOption...)
     */
    public void setAttribute(DynRoute route, String attribute, Object value, LinkOption... options)
            throws IOException;

}
