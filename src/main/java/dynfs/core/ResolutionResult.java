package dynfs.core;

import java.io.IOException;
import java.nio.file.FileSystemLoopException;
import java.nio.file.NoSuchFileException;
import java.nio.file.NotDirectoryException;

public final class ResolutionResult<Space extends DynSpace<Space>> {

    //
    // Configuration: DynRoute Resolution Result

    private final DynDirectory<Space, ?> lastParent;
    private final DynNode<Space, ?> node;

    private final DynRoute route;
    private final int lastIndex;
    private final int endIndex;

    private final Result status;

    private final Object cause;

    //
    // Configuration: Cached Exception

    private final Exception ex;

    //
    // Construction

    ResolutionResult(DynDirectory<Space, ?> lastParent, DynNode<Space, ?> node, DynRoute route, int lastIndex,
            int endIndex, Result status) {
        this(lastParent, node, route, lastIndex, endIndex, status, null);
    }

    ResolutionResult(DynDirectory<Space, ?> lastParent, DynNode<Space, ?> node, DynRoute route, int lastIndex,
            int endIndex, Result status, Object cause) {
        this.lastParent = lastParent;
        this.node = node;

        this.route = route;
        this.lastIndex = lastIndex;
        this.endIndex = endIndex;

        this.status = status;

        this.cause = cause;

        this.ex = constructException();
    }

    //
    // Enumerable: Result Flag

    public static enum Result {
        INCONSISTENT_STATE_ERROR,
        SUCCESS_END_INDEX_REACHED,
        FAIL_NON_DIRECTORY_ENCOUNTERED,
        FAIL_NAME_NOT_FOUND,
        FAIL_IO_EXCEPTION_DURING_RESOLUTION,
        FAIL_LINK_LOOP,
        FAIL_SUBRESOLUTION_FAILURE;
    }

    //
    // Support: Exception Construction

    private Exception constructException() {
        switch (status) {
            case INCONSISTENT_STATE_ERROR:
                return new IllegalStateException(cause.toString());
            case SUCCESS_END_INDEX_REACHED:
                return null;
            case FAIL_NON_DIRECTORY_ENCOUNTERED:
                return new NotDirectoryException(route.subroute(0, lastIndex).toString());
            case FAIL_NAME_NOT_FOUND:
                return new NoSuchFileException(route.subroute(0, lastIndex).toString());
            case FAIL_IO_EXCEPTION_DURING_RESOLUTION:
                return new IOException(
                        "I/O Exception encountered when resolving " + route.subroute(0, lastIndex + 1),
                        (IOException) cause);
            case FAIL_LINK_LOOP:
                return new FileSystemLoopException(cause.toString());
            case FAIL_SUBRESOLUTION_FAILURE:
                @SuppressWarnings("unchecked")
                ResolutionResult<Space> subresolution = (ResolutionResult<Space>) cause;
                return new IOException("Subresolution failure while resolving " + subresolution.route,
                        subresolution.constructException());
            default:
                return new IllegalStateException("ResolutionResult status is invalid");
        }
    }

    //
    // Interface: Field Access

    public DynDirectory<Space, ?> lastParent() {
        return lastParent;
    }

    public DynNode<Space, ?> node() {
        return node;
    }

    public DynRoute route() {
        return route;
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

    public Object cause() {
        return cause;
    }

    public Exception getException() {
        return ex;
    }

    //
    // Implementation: Exception Generation

    public static class DynRouteResolutionFailureException extends IOException {
        private static final long serialVersionUID = 1L;

        private DynRouteResolutionFailureException(Exception inner) {
            super(inner);
        }
    }

    public void throwException() throws IOException {
        if (ex instanceof IOException)
            throw new DynRouteResolutionFailureException(ex);
        if (ex instanceof RuntimeException)
            throw new DynRouteResolutionFailureException(ex);
        if (ex == null)
            return;

        throw new IllegalStateException("Unexpectable exception encountered", ex);
    }

    //
    // Interface: Query, Resolution Success

    public boolean isSuccess() {
        return status() == Result.SUCCESS_END_INDEX_REACHED;
    }

    //
    // Interface Implementation: Query, Existence

    public boolean exists() throws IOException {
        if (isSuccess())
            return true;
        if (status() == Result.FAIL_NAME_NOT_FOUND || status() == Result.FAIL_NON_DIRECTORY_ENCOUNTERED)
            return false;

        throwException();
        return false;
    }

    //
    // Interface Implementation: Query, Existence of Parent Directory

    public boolean existsParentDirectory() throws IOException {
        if (isSuccess())
            return true;
        if (status() == Result.FAIL_NAME_NOT_FOUND)
            return lastIndex >= endIndex - 1;

        throwException();
        return false;
    }

    //
    // Interface Implementation: Resolution Query, Existence

    public DynNode<Space, ?> testExistence() throws IOException {
        if (isSuccess()) {
            // DynNode resolved from DynRoute
            return node();
        } else {
            // Resolution failure or DynNode does not exist
            throwException();
            return null;
        }
    }

    public DynNode<Space, ?> testExistenceForCreation() throws IOException {
        if (isSuccess()) {
            // DynNode resolved from DynRoute
            return node();
        } else if (existsParentDirectory()) {
            // Parent DynDirectory exists; DynNode does not exist
            return null;
        } else {
            // Resolution failure or parent DynDirectory does not exist
            throwException();
            return null;
        }
    }

}
