package dynfs.core;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.CopyOption;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.spi.FileSystemProvider;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import dynfs.core.ResolutionResult.Result;
import dynfs.core.options.CopyOptions;

public abstract class DynDirectory<Space extends DynSpace<Space>, Node extends DynDirectory<Space, Node>>
        extends DynNode<Space, Node>
        implements Iterable<DynNode<Space, ?>> {

    //
    // Construction

    // Root Directory
    protected DynDirectory(Space store) {
        super(store, null, null);
    }

    // Non-Root Directory
    protected <DirNode extends DynDirectory<Space, DirNode>> DynDirectory(Space store, DirNode parent, String name) {
        super(store, parent, name);
        validateName(name);
    }

    //
    // Interface Implementation: DynNode Type Attributes

    @Override
    public final boolean isRegularFile() {
        return false;
    }

    @Override
    public final boolean isDirectory() {
        return true;
    }

    @Override
    public final boolean isSymbolicLink() {
        return false;
    }

    @Override
    public final boolean isOther() {
        return false;
    }

    //
    // Implementation Stub: DynFileSystemProvider I/O, File Creation

    public final DynFile<Space, ?> createFile(String name, FileAttribute<?>... attrs)
            throws IOException {
        // FUTURE: Access Control - Check access control
        return createFileImpl(name, attrs);
    }

    /**
     * @see FileSystemProvider#newByteChannel(Path, Set, FileAttribute...)
     */
    protected abstract DynFile<Space, ?> createFileImpl(String name, FileAttribute<?>... attrs)
            throws IOException;

    //
    // Implementation Default: I/O, Sparse File Creation

    public final DynFile<Space, ?> createSparseFile(String name, FileAttribute<?>... attrs)
            throws IOException {
        // FUTURE: Access Control - Check access control
        return createSparseFileImpl(name, attrs);
    }

    /**
     * @see StandardOpenOption#SPARSE
     */
    protected DynFile<Space, ?> createSparseFileImpl(String name, FileAttribute<?>... attrs)
            throws IOException {
        return createFileImpl(name, attrs);
    }

    //
    // Implementation Stub: DynFileSystemProvider I/O, Directory Creation

    public final DynDirectory<Space, ?> createDirectory(String name, FileAttribute<?>... attrs)
            throws IOException {
        // FUTURE: Access Control - Check access control
        return createDirectoryImpl(name, attrs);
    }

    /**
     * @see FileSystemProvider#createDirectory(Path, FileAttribute...)
     */
    protected abstract DynDirectory<Space, ?> createDirectoryImpl(String name, FileAttribute<?>... attrs)
            throws IOException;

    //
    // Implementation Stub: DynFileSystemProvider I/O, Child Deletion

    final void deleteChild(String name, DynNode<Space, ?> node) throws IOException {
        DynNode<Space, ?> resolvedNode = resolveChildImpl(name);
        if (resolvedNode != node)
            throw new IllegalStateException("Resolving name gives a different node");

        node.preDelete();

        deleteChildImpl(name, node);
        node.deleteImpl();

        node.postDeleteImpl();
    }

    protected abstract void deleteChildImpl(String name, DynNode<Space, ?> node) throws IOException;

    //
    // Interface: DynFileSystemProvider I/O, Copy / Move

    public final void copy(DynNode<Space, ?> src, String dstName, CopyOptions copyOptions) throws IOException {
        unifiedCopyMove(src, dstName, copyOptions, false);
    }

    public final void move(DynNode<Space, ?> src, String dstName, CopyOptions copyOptions) throws IOException {
        unifiedCopyMove(src, dstName, copyOptions, true);
    }

    void unifiedCopyMove(DynNode<Space, ?> src, String dstName, CopyOptions copyOptions, boolean deleteSrc)
            throws IOException {
        // FUTURE: Access Control - Check access control
        copyImpl(src, dstName, copyOptions, deleteSrc);
    }

    //
    // Implementation Stub: DynFileSystemProvider I/O, Copy / Move

    /**
     * @see FileSystemProvider#copy(Path, Path, CopyOption...)
     * @see FileSystemProvider#move(Path, Path, CopyOption...)
     */
    protected void copyImpl(DynNode<Space, ?> srcNode, String dstName, CopyOptions copyOptions, boolean deleteSrc)
            throws IOException {
        DynNode<Space, ?> dstNode = resolveChildImpl(dstName);

        if (copyOptions.atomicMove) {
            // TODO: Atomic I/O - FS structure locks ...
        }

        if (dstNode != null) {
            if (copyOptions.replaceExisting) {
                if (dstNode instanceof DynDirectory) {
                    if (!((DynDirectory<Space, ?>) dstNode).isEmpty())
                        throw new DirectoryNotEmptyException(dstNode.getRouteString());
                } else if (dstNode instanceof DynLink) {
                    throw new FileAlreadyExistsException(dstNode.getRouteString(), null,
                            "Target file is a symbolic link");
                }

                dstNode.delete();
            } else {
                throw new FileAlreadyExistsException(dstNode.getRouteString());
            }
        }

        if (srcNode.isDirectory()) {
            createDirectoryImpl(dstName);
        } else {
            copySimpleImpl(srcNode, dstName);
        }

        dstNode = resolveChildImpl(dstName);

        if (copyOptions.copyAttributes) {
            try {
                Map<DynNodeAttribute, Object> srcAttributes = srcNode.readAllAttributes();
                dstNode.writeAttributes(srcAttributes);
            } catch (IOException ex) {
                // Makes a best effort to copy the DynNode attributes.
                // If an exception is encountered, the DynFileSystem should be left in a
                // consistent state by DynNode.writeAttributes.
            }
        }

        // TODO: Should lastAccessTime be modified? creationTime?

        if (deleteSrc) {
            srcNode.delete();
        }
    }

    // NOTE: Javadoc Note - Implement this or implement a no-op and override
    // copyImpl.
    protected abstract void copySimpleImpl(DynNode<Space, ?> srcNode, String dstName) throws IOException;

    //
    // Interface Implementation Stub: Iterable<DynNode>

    @Override
    public abstract Iterator<DynNode<Space, ?>> iterator();

    public final boolean isEmpty() {
        return !iterator().hasNext();
    }

    //
    // Interface Implementation Stub: Child Resolution

    public final DynNode<Space, ?> resolveChild(String name) throws IOException {
        // FUTURE: Access Control - Check access control

        DynNode<Space, ?> node = resolveChildImpl(name);
        if (node == null)
            throw new FileNotFoundException(getRoute().resolve(name).toString());

        return node;
    }

    private DynNode<Space, ?> resolveRouteName(String name) throws IOException {
        if (DynRoute.PATH_CURDIR.equals(name))
            return this;
        if (DynRoute.PATH_PARENT.equals(name))
            return getParent();

        return resolveChildImpl(name);
    }

    protected abstract DynNode<Space, ?> resolveChildImpl(String name) throws IOException;

    //
    // Interface: DynRoute Resolution

    public final ResolutionResult<Space> resolve(DynRoute route) throws IOException {
        return resolve(route, true);
    }

    public final ResolutionResult<Space> resolve(DynRoute route, boolean followLinks) throws IOException {
        return resolve(route, followLinks, false);
    }

    public final ResolutionResult<Space> resolve(DynRoute route, boolean followLinks, boolean followIfLinkNode)
            throws IOException {
        if (route == null)
            throw new NullPointerException("route is null");

        return resolveImpl(route, followLinks, followIfLinkNode);
    }

    public final ResolutionResult<Space> resolve(DynRoute route, int endIndex) throws IOException {
        return resolve(route, true, endIndex);
    }

    public final ResolutionResult<Space> resolve(DynRoute route, boolean followLinks, int endIndex) throws IOException {
        return resolve(route, followLinks, false, endIndex);
    }

    public final ResolutionResult<Space> resolve(DynRoute route, boolean followLinks, boolean followIfLinkNode,
            int endIndex) throws IOException {
        if (route == null)
            throw new NullPointerException("route is null");
        if (endIndex < 0)
            throw new IllegalArgumentException("endIndex must be nonnegative");
        if (endIndex > route.getNameCount())
            throw new IllegalArgumentException("endIndex must be at most " + route.getNameCount());

        return resolveImpl(route, followLinks, followIfLinkNode, endIndex);
    }

    private ResolutionResult<Space> resolveImpl(DynRoute route, boolean followLinks, boolean followIfLinkNode)
            throws IOException {
        return resolveImpl(route, followLinks, followIfLinkNode, route.getNameCount());
    }

    private ResolutionResult<Space> resolveImpl(DynRoute route, boolean followLinks, boolean followIfLinkNode,
            int endIndex)
            throws IOException {
        return resolveImpl(route, followLinks, followIfLinkNode, 0, endIndex);
    }

    //
    // Implementation: DynRoute Resolution

    // FUTURE: Access Control + UserPrincipal Support - Result.READ_ACCESS_DENIED is
    // possible when reading directories / following links
    private ResolutionResult<Space> resolveImpl(DynRoute route, boolean followLinks, boolean followIfLinkNode,
            int index, int endIndex) throws IOException {
        getStore().throwIfClosed();

        DynDirectory<Space, ?> lastParent = this;
        DynNode<Space, ?> lastNode = this;

        while (index < endIndex) {
            try {
                lastNode = lastParent.resolveRouteName(route.getName(index));
            } catch (IOException ex) {
                return new ResolutionResult<>(lastParent, lastParent, route, index, endIndex,
                        Result.FAIL_IO_EXCEPTION_DURING_RESOLUTION, ex);
            }

            if (lastNode == null) {
                return new ResolutionResult<>(lastParent, lastParent, route, index, endIndex,
                        Result.FAIL_NAME_NOT_FOUND);
            }

            lastParent = lastNode.getParent();
            index++;

            if (index < endIndex || followIfLinkNode) {
                if (followLinks && lastNode instanceof DynLink) {
                    Set<DynLink<Space, ?>> visitedLinks = new HashSet<>();
                    while (lastNode instanceof DynLink) {
                        DynLink<Space, ?> link = (DynLink<Space, ?>) lastNode;
                        if (visitedLinks.contains(link)) {
                            return new ResolutionResult<>(lastParent, lastNode, route, index, endIndex,
                                    Result.FAIL_LINK_LOOP, link.getRoute());
                        }

                        visitedLinks.add(link);

                        ResolutionResult<Space> resolution = getStore().resolve(link.follow());
                        if (!resolution.isSuccess()) {
                            return new ResolutionResult<>(lastParent, lastNode, route, index, endIndex,
                                    Result.FAIL_SUBRESOLUTION_FAILURE, resolution);
                        }

                        lastNode = resolution.node();
                    }
                }

                if (!(lastNode instanceof DynDirectory)) {
                    return new ResolutionResult<>(lastParent, lastNode, route, index, endIndex,
                            Result.FAIL_NON_DIRECTORY_ENCOUNTERED);
                }

                lastParent = (DynDirectory<Space, ?>) lastNode;
            }
        }

        return new ResolutionResult<>(lastParent, lastNode, route, index, endIndex,
                Result.SUCCESS_END_INDEX_REACHED);
    }

}
