package dynfs.core.io;

import java.io.IOException;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.DirectoryStream;
import java.nio.file.DirectoryStream.Filter;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.FileAttributeView;
import java.util.Map;

import dynfs.core.DynAttributesView;
import dynfs.core.DynDirectory;
import dynfs.core.DynFileSystem;
import dynfs.core.DynFileSystemGeneralCopier;
import dynfs.core.DynNode;
import dynfs.core.DynSpace;
import dynfs.core.ResolutionResult;
import dynfs.core.options.AccessModes;
import dynfs.core.options.CopyOptions;
import dynfs.core.options.LinkOptions;
import dynfs.core.options.OpenOptions;
import dynfs.core.path.DynRoute;

public final class DynFileSystemIO {

    // TODO: Create/delete/access curDir / parentDir?
    // TODO: Rename to DynFileSystemProviderIO?

    //
    // Construction: Disabled

    private DynFileSystemIO() {}

    //
    // Interface Implementation: DynFileSystemProvider I/O

    public static SeekableByteChannel newByteChannel(DynFileSystem<?> fs, DynRoute route, OpenOptions openOptions,
            FileAttribute<?>... attrs)
            throws IOException {
        return fs.newByteChannel(route, openOptions, attrs);
    }

    public static DirectoryStream<Path> newDirectoryStream(DynFileSystem<?> fs, DynRoute dir,
            Filter<? super Path> filter)
            throws IOException {
        return fs.newDirectoryStream(dir, filter);
    }

    public static <Space extends DynSpace<Space>> void createDirectory(DynFileSystem<Space> fs, DynRoute dir,
            FileAttribute<?>... attrs)
            throws IOException {
        ResolutionResult<Space> resolution = fs.resolve(dir);
        DynNode<Space, ?> node = resolution.testExistenceForCreation();

        if (node != null)
            throw new FileAlreadyExistsException(dir.toString());

        // TODO: Check access control

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

    // TODO: Adhere to API specification of Files.copy (re: copy to link, etc.)
    // TODO: Redesign / restructure
    private static <Space extends DynSpace<Space>> void copyImpl(DynFileSystem<Space> fs, DynRoute src, DynRoute dst,
            CopyOptions copyOptions,
            boolean deleteSrc) throws IOException {
        ResolutionResult<Space> srcResolution = fs.resolve(src, !copyOptions.nofollowLinks);
        DynNode<Space, ?> srcNode = srcResolution.testExistence();
        ResolutionResult<Space> dstResolution = fs.resolve(dst);
        DynNode<Space, ?> dstNode = dstResolution.testExistenceForCreation();

        // TODO: Check access control

        if (copyOptions.atomicMove) {
            // TODO: FS structure locks ...
        }

        DynDirectory<Space, ?> dstParentNode = dstNode == null ? (DynDirectory<Space, ?>) dstResolution.node()
                : dstNode.getParent();

        BasicFileAttributes srcAttributes = srcNode.readAttributes(BasicFileAttributes.class);

        if (dstNode != null) {
            if (copyOptions.replaceExisting) {
                if ((dstNode instanceof DynDirectory)
                        && !((DynDirectory<Space, ?>) dstNode).isEmpty()) {
                    throw new DirectoryNotEmptyException(dst.toString());
                }
                dstNode.delete();
            } else {
                throw new FileAlreadyExistsException(dst.toString());
            }
        }

        if (srcAttributes.isDirectory()) {
            dstParentNode.createDirectoryImpl(dst.getFileName());
        } else {
            dstParentNode.copyImpl(srcNode, dst.getFileName(), deleteSrc);
        }

        // TODO: Should this be handled by implementation instead of framework?
        if (copyOptions.copyAttributes) {
            try {
                BasicFileAttributeView dstAttributes = getFileAttributeView(fs, dst,
                        BasicFileAttributeView.class,
                        copyOptions.getLinkOptions());
                dstAttributes.setTimes(srcAttributes.lastModifiedTime(), srcAttributes.lastAccessTime(),
                        srcAttributes.creationTime());
            } catch (IOException ex) {
                try {
                    // TODO: Revert
                    throw new IOException("Dummy");
                } catch (IOException ex0) {
                    ex.addSuppressed(ex0);
                }

                throw ex;
            }
        }

        // TODO: Should lastAccessTime be modified? creationTime?
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

    // TODO: Is there a workaround instead of the SupressWarnings annotation?
    @SuppressWarnings("unchecked")
    public static <Space extends DynSpace<Space>, V extends FileAttributeView> V getFileAttributeView(
            DynFileSystem<Space> fs, DynRoute route,
            Class<V> type,
            LinkOptions linkOptions) {
        if (!type.isAssignableFrom(DynAttributesView.class))
            return null;

        return (V) new DynAttributesView<Space>(fs.getStore(), route);
    }

    public static <Space extends DynSpace<Space>, A extends BasicFileAttributes> A readAttributes(
            DynFileSystem<Space> fs,
            DynRoute route, Class<A> type,
            LinkOptions linkOptions)
            throws IOException {
        DynNode<Space, ?> node = fs.resolve(route, !linkOptions.nofollowLinks).testExistence();
        return node.readAttributes(type);
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
        node.setAttribute(attribute, value);
    }

}
