package dynfs.core;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.DirectoryStream;
import java.nio.file.DirectoryStream.Filter;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.NotDirectoryException;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.FileAttributeView;
import java.util.Map;

import dynfs.core.options.AccessModes;
import dynfs.core.options.CopyOptions;
import dynfs.core.options.LinkOptions;
import dynfs.core.options.OpenOptions;

public final class DynFileSystemProviderIO {

    //
    // Construction: Disabled

    private DynFileSystemProviderIO() {}

    //
    // Interface Implementation: DynFileSystemProvider I/O

    public static <Space extends DynSpace<Space>> SeekableByteChannel newByteChannel(DynFileSystem<Space> fs,
            DynRoute route, OpenOptions openOptions, FileAttribute<?>... attrs) throws IOException {
        ResolutionResult<Space> resolution = fs.resolve(route);
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
            DynDirectory<Space, ?> parentDirectory = resolution.lastParent();
            if (openOptions.sparse) {
                file = parentDirectory.createSparseFile(route.getFileName(), attrs);
            } else {
                file = parentDirectory.createFile(route.getFileName(), attrs);
            }
        }

        // FUTURE: Access Control - Check access control

        return new DynByteChannel(file, openOptions);
    }

    public static <Space extends DynSpace<Space>> DirectoryStream<Path> newDirectoryStream(DynFileSystem<Space> fs,
            DynRoute dir, Filter<? super Path> filter) throws IOException {
        DynNode<Space, ?> node = fs.resolve(dir).testExistence();

        if (!(node instanceof DynDirectory))
            throw new NotDirectoryException(dir.toString());

        // FUTURE: Access Control - Check access control

        return new DynDirectoryStream<>(fs, (DynDirectory<Space, ?>) node, filter);
    }

    public static <Space extends DynSpace<Space>> void createDirectory(DynFileSystem<Space> fs, DynRoute dir,
            FileAttribute<?>... attrs)
            throws IOException {
        ResolutionResult<Space> resolution = fs.resolve(dir);
        DynNode<Space, ?> node = resolution.testExistenceForCreation();

        if (node != null)
            throw new FileAlreadyExistsException(dir.toString());

        // FUTURE: Access Control - Check access control

        resolution.lastParent().createDirectoryImpl(dir.getFileName(), attrs);
    }

    public static <Space extends DynSpace<Space>> void delete(DynFileSystem<Space> fs, DynRoute route)
            throws IOException {
        DynNode<Space, ?> node = fs.resolve(route).testExistence();
        node.delete();
    }

    public static void copy(DynFileSystem<?> fsSrc, DynFileSystem<?> fsDst, DynRoute src, DynRoute dst,
            CopyOptions copyOptions) throws IOException {
        if (fsSrc == fsDst) {
            copy(fsSrc, src, dst, copyOptions);
        } else {
            DynFileSystemGeneralCopier.copy(fsSrc, fsDst, src, dst, copyOptions);
        }
    }

    public static void copy(DynFileSystem<?> fs, DynRoute src, DynRoute dst,
            CopyOptions copyOptions) throws IOException {
        copyImpl(fs, src, dst, copyOptions, false);
    }

    public static void move(DynFileSystem<?> fsSrc, DynFileSystem<?> fsDst, DynRoute src, DynRoute dst,
            CopyOptions copyOptions) throws IOException {
        if (fsSrc == fsDst) {
            move(fsSrc, src, dst, copyOptions);
        } else {
            DynFileSystemGeneralCopier.move(fsSrc, fsDst, src, dst, copyOptions);
        }
    }

    public static void move(DynFileSystem<?> fs, DynRoute src, DynRoute dst,
            CopyOptions copyOptions) throws IOException {
        copyImpl(fs, src, dst, copyOptions, true);
    }

    private static <Space extends DynSpace<Space>> void copyImpl(DynFileSystem<Space> fs, DynRoute src, DynRoute dst,
            CopyOptions copyOptions,
            boolean deleteSrc) throws IOException {
        ResolutionResult<Space> srcResolution = fs.resolve(src, !copyOptions.nofollowLinks);
        DynNode<Space, ?> srcNode = srcResolution.testExistence();
        ResolutionResult<Space> dstResolution = fs.resolve(dst);
        dstResolution.testExistenceForCreation();

        dstResolution.lastParent().unifiedCopyMove(srcNode, dst.getFileName(), copyOptions, deleteSrc);
    }

    public static <Space extends DynSpace<Space>> boolean isSameFile(DynFileSystem<Space> fs, DynRoute route1,
            DynRoute route2) throws IOException {
        ResolutionResult<Space> resolution1 = fs.resolve(route1);
        ResolutionResult<Space> resolution2 = fs.resolve(route2);

        DynNode<Space, ?> node1 = resolution1.testExistence();
        DynNode<Space, ?> node2 = resolution2.testExistence();

        return node1.isSameFile(node2);
    }

    public static <Space extends DynSpace<Space>> boolean isHidden(DynFileSystem<Space> fs, DynRoute route)
            throws IOException {
        DynNode<Space, ?> node = fs.resolve(route).testExistence();
        return node.isHidden();
    }

    public static <Space extends DynSpace<Space>> void checkAccess(DynFileSystem<Space> fs, DynRoute route,
            AccessModes accessModes) throws IOException {
        DynNode<Space, ?> node = fs.resolve(route).testExistence();
        node.checkAccess(accessModes);
    }

    public static <Space extends DynSpace<Space>, V extends FileAttributeView> V getFileAttributeView(
            DynFileSystem<Space> fs, DynRoute route,
            Class<V> type,
            LinkOptions linkOptions) {
        if (!type.isAssignableFrom(DynNodeFileAttributeView.class))
            return null;

        DynNodeFileAttributeView attributeView = new DynNodeFileAttributeView(fs.getStore(), route);

        // If (type.isAssignableFrom(DynNodeFileAttributeView.class)), then
        // attributeView is of type V.
        @SuppressWarnings("unchecked")
        V result = (V) attributeView;

        return result;
    }

    public static <Space extends DynSpace<Space>, A extends BasicFileAttributes> A readAttributes(
            DynFileSystem<Space> fs,
            DynRoute route, Class<A> type,
            LinkOptions linkOptions)
            throws IOException {
        DynNode<Space, ?> node = fs.resolve(route, !linkOptions.nofollowLinks).testExistence();
        return node.readAttributesAsFileAttributesClass(type);
    }

    public static <Space extends DynSpace<Space>> Map<String, Object> readAttributes(DynFileSystem<Space> fs,
            DynRoute route, String attributes,
            LinkOptions linkOptions) throws IOException {
        DynNode<Space, ?> node = fs.resolve(route, !linkOptions.nofollowLinks).testExistence();
        return node.readAttributes(attributes);
    }

    public static <Space extends DynSpace<Space>> void setAttribute(DynFileSystem<Space> fs, DynRoute route,
            String attribute, Object value,
            LinkOptions linkOptions) throws IOException {
        DynNode<Space, ?> node = fs.resolve(route, !linkOptions.nofollowLinks).testExistence();
        node.writeAttribute(attribute, value);
    }

}
