package dynfs.core;

import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import dynfs.core.ResolutionResult.Result;
import dynfs.core.path.DynRoute;

public abstract class DynDirectory<Space extends DynSpace<Space>, Node extends DynDirectory<Space, Node>>
        extends DynNode<Space, Node>
        implements Iterable<DynNode<Space, ?>> {

    //
    // Implementation: Attributes

    @Override
    public final boolean isDirectory() {
        return true;
    }

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
    // Implementation: Size (Abstract)

    @Override
    public long size() {
        return 0L;
    }

    //
    // Interface: Iterable<DynNode<>> (Abstract)

    @Override
    public abstract Iterator<DynNode<Space, ?>> iterator();

    public final boolean isEmpty() {
        return !iterator().hasNext();
    }

    //
    // Implementation: Child Resolution (Abstract)

    private DynNode<Space, ?> resolveRouteName(String name) throws IOException {
        if (DynRoute.PATH_CURDIR.equals(name))
            return this;
        if (DynRoute.PATH_PARENT.equals(name))
            return getParent();
        return resolveChild(name);
    }

    protected abstract DynNode<Space, ?> resolveChild(String name) throws IOException;

    //
    // Implementation: DynRoute Resolution

    public final ResolutionResult<Space> resolve(DynRoute route) throws IOException {
        return resolve(route, true);
    }

    public final ResolutionResult<Space> resolve(DynRoute route, boolean followLinks) throws IOException {
        return resolveImpl(route, followLinks);
    }

    public final ResolutionResult<Space> resolve(DynRoute route, int endIndex) throws IOException {
        return resolve(route, true, endIndex);
    }

    public final ResolutionResult<Space> resolve(DynRoute route, boolean followLinks, int endIndex) throws IOException {
        if (endIndex < 0)
            throw new IllegalArgumentException("endIndex must be nonnegative");
        if (endIndex > route.getNameCount())
            throw new IllegalArgumentException("endIndex must be at most " + route.getNameCount());

        return resolveImpl(route, followLinks, endIndex);
    }

    private final ResolutionResult<Space> resolveImpl(DynRoute route, boolean followLinks) throws IOException {
        return resolveImpl(route, followLinks, route.getNameCount());
    }

    private final ResolutionResult<Space> resolveImpl(DynRoute route, boolean followLinks, int endIndex)
            throws IOException {
        return resolveImpl(route, followLinks, 0, endIndex);
    }

    private final ResolutionResult<Space> resolveImpl(DynRoute route, boolean followLinks,
            int startIndex, int endIndex) throws IOException {
        getStore().throwIfClosed();

        DynNode<Space, ?> lastNode = this;
        DynDirectory<Space, ?> lastDir = this;

        while (startIndex < endIndex) {
            try {
                lastNode = lastDir.resolveRouteName(route.getNameAsString(startIndex));
            } catch (IOException ex) {
                return new ResolutionResult<Space>(lastDir, route, startIndex, endIndex,
                        Result.FAIL_IO_EXCEPTION_DURING_RESOLUTION, ex);
            }

            if (lastNode == null) {
                return new ResolutionResult<Space>(lastDir, route, startIndex, endIndex,
                        Result.FAIL_NAME_NOT_FOUND);
            }

            startIndex++;

            if (startIndex < endIndex) {
                if (followLinks && lastNode instanceof DynLink) {
                    Set<DynLink<Space, ?>> visitedLinks = new HashSet<>();
                    while (lastNode instanceof DynLink) {
                        DynLink<Space, ?> link = (DynLink<Space, ?>) lastNode;
                        if (visitedLinks.contains(link)) {
                            return new ResolutionResult<Space>(lastNode, route, startIndex, endIndex,
                                    Result.FAIL_LINK_LOOP, link.getRoute());
                        }

                        visitedLinks.add(link);

                        ResolutionResult<Space> resolution = getStore().resolve(link.follow());
                        if (!resolution.isSuccess()) {
                            return new ResolutionResult<Space>(lastNode, route, startIndex, endIndex,
                                    Result.FAIL_SUBRESOLUTION_FAILURE, resolution);
                        }

                        lastNode = resolution.node();
                    }
                }

                if (!(lastNode instanceof DynDirectory)) {
                    return new ResolutionResult<Space>(lastNode, route, startIndex, endIndex,
                            Result.FAIL_NON_DIRECTORY_ENCOUNTERED);
                }
            }
        }

        return new ResolutionResult<Space>(lastNode, route, startIndex, endIndex, Result.SUCCESS_END_INDEX_REACHED);
    }

}
