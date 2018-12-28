package dynfs.core.file;

import dynfs.core.DynSpace;
import dynfs.core.path.DynRoute;

public final class ResolutionResult<Space extends DynSpace<Space>> {
    public static enum Result {
        INCONSISTENT_STATE_ERROR,
        SUCCESS_END_INDEX_REACHED,
        FAIL_NON_DIRECTORY_ENCOUNTERED,
        FAIL_NAME_NOT_FOUND,
        FAIL_IO_EXCEPTION_DURING_RESOLUTION;
    }

    private final DynNode<Space, ?> node;

    private final DynRoute route;
    private final int lastIndex;
    private final int endIndex;

    private final Result status;

    ResolutionResult(DynNode<Space, ?> node, DynRoute route, int lastIndex, int endIndex, Result status) {
        this.node = node;
        this.route = route;
        this.lastIndex = lastIndex;
        this.endIndex = endIndex;
        this.status = status;
    }

    public DynRoute route() {
        return route;
    }

    public DynNode<Space, ?> node() {
        return node;
    }

    public int lastIndex() {
        return lastIndex;
    }

    public int endIndex() {
        return endIndex;
    }

    public Result status() {
        return status;
    }
}
