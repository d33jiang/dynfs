package dynfs.core.file;

import java.io.IOException;
import java.util.Iterator;

import dynfs.core.DynSpace;
import dynfs.core.file.ResolutionResult.Result;
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
    DynDirectory(Space store) {
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

    //
    // Interface: Child Resolution (Abstract)

    public abstract DynNode<Space, ?> resolve(String name) throws IOException;

    //
    // Implementation: DynRoute Resolution

    public final ResolutionResult<Space> resolve(DynRoute route) {
        return resolveImpl(route);
    }

    public final ResolutionResult<Space> resolve(DynRoute route, int endIndex) {
        if (endIndex < 0)
            throw new IllegalArgumentException("endIndex must be nonnegative");
        if (endIndex > route.getNameCount())
            throw new IllegalArgumentException("endIndex must be at most " + route.getNameCount());

        return resolve(route, endIndex);
    }

    private final ResolutionResult<Space> resolveImpl(DynRoute route) {
        return resolveImpl(route, route.getNameCount());
    }

    private final ResolutionResult<Space> resolveImpl(DynRoute route, int endIndex) {
        return resolveImpl(route, 0, endIndex);
    }

    private final ResolutionResult<Space> resolveImpl(DynRoute route, int startIndex, int endIndex) {
        DynNode<Space, ?> lastNode = this;
        DynDirectory<Space, ?> lastDir = this;

        while (startIndex < endIndex) {
            try {
                lastNode = lastDir.resolve(route.getRouteName(startIndex));
            } catch (IOException ex) {
                return new ResolutionResult<Space>(lastDir, route, startIndex, endIndex,
                        Result.FAIL_IO_EXCEPTION_DURING_RESOLUTION);
            }

            if (lastNode == null) {
                return new ResolutionResult<Space>(lastDir, route, startIndex, endIndex,
                        Result.FAIL_NAME_NOT_FOUND);
            }

            startIndex++;

            if (!(lastNode instanceof DynDirectory) && startIndex < endIndex) {
                return new ResolutionResult<Space>(lastNode, route, startIndex, endIndex,
                        Result.FAIL_NON_DIRECTORY_ENCOUNTERED);
            }
        }

        return new ResolutionResult<Space>(lastNode, route, startIndex, endIndex, Result.SUCCESS_END_INDEX_REACHED);
    }

}
