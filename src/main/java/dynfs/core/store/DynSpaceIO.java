package dynfs.core.store;

import java.io.IOException;
import java.nio.file.CopyOption;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.FileAttributeView;
import java.nio.file.spi.FileSystemProvider;
import java.util.Map;
import java.util.Set;

import dynfs.core.DynDirectory;
import dynfs.core.DynFile;
import dynfs.core.DynNode;
import dynfs.core.DynSpace;
import dynfs.core.options.CopyOptions;
import dynfs.core.options.LinkOptions;
import dynfs.core.path.DynRoute;

public interface DynSpaceIO<Space extends DynSpace<Space>> {

    // TODO: lock(Route), facilities for atomic operations

    //
    // File Creation

    /**
     * @see FileSystemProvider#newByteChannel(Path, Set, FileAttribute...)
     */
    public DynFile<Space, ?> createFile(DynDirectory<Space, ?> parent, String name, FileAttribute<?>... attrs)
            throws IOException;

    //
    // Directory Creation

    /**
     * @see FileSystemProvider#createDirectory(Path, FileAttribute...)
     */
    public DynDirectory<Space, ?> createDirectory(DynDirectory<Space, ?> parent, String name, FileAttribute<?>... attrs)
            throws IOException;

    //
    // File Operations

    /**
     * @see FileSystemProvider#delete(Path)
     */
    public void delete(DynNode<Space, ?> node) throws IOException;

    /**
     * @see FileSystemProvider#copy(Path, Path, CopyOption...)
     * @see FileSystemProvider#move(Path, Path, CopyOption...)
     */
    public void copy(DynNode<Space, ?> src, DynNode<Space, ?> dstParent, String dstName, boolean deleteSrc)
            throws IOException;

    //
    // File Attributes

    /**
     * @see FileSystemProvider#getFileAttributeView(Path, Class, LinkOption...)
     */
    public <V extends FileAttributeView> V getFileAttributeView(DynRoute route, Class<V> type, LinkOptions options);

    /**
     * @see FileSystemProvider#readAttributes(Path, Class, LinkOption...)
     */
    public <A extends BasicFileAttributes> A readAttributes(DynRoute route, Class<A> type, LinkOptions options)
            throws IOException;

    /**
     * @see FileSystemProvider#readAttributes(Path, String, LinkOption...)
     */
    public Map<String, Object> readAttributes(DynRoute route, String attributes, LinkOptions options)
            throws IOException;

    /**
     * @see FileSystemProvider#setAttribute(Path, String, Object, LinkOption...)
     */
    public void setAttribute(DynRoute route, String attribute, Object value, LinkOptions options)
            throws IOException;

}
