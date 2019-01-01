package dynfs.core;

import java.io.IOException;
import java.nio.file.FileSystemLoopException;
import java.nio.file.NoSuchFileException;
import java.nio.file.NotDirectoryException;

import dynfs.core.path.DynRoute;

public final class ResolutionResult<Space extends DynSpace<Space>> {

    //
    // Enum: Result Flag

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
    // Implementation: Exception Generation

    private Exception generateException() {
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
                        subresolution.generateException());
            default:
                return new IllegalStateException("ResolutionResult status is invalid");
        }
    }

    //
    // Field: Internal Data

    private final DynNode<Space, ?> node;

    private final DynRoute route;
    private final int lastIndex;
    private final int endIndex;

    private final Result status;

    private final Object cause;

    private final Exception ex;

    //
    // Construction

    ResolutionResult(DynNode<Space, ?> node, DynRoute route, int lastIndex, int endIndex, Result status) {
        this(node, route, lastIndex, endIndex, status, null);
    }

    ResolutionResult(DynNode<Space, ?> node, DynRoute route, int lastIndex, int endIndex, Result status, Object cause) {
        this.node = node;

        this.route = route;
        this.lastIndex = lastIndex;
        this.endIndex = endIndex;

        this.status = status;

        this.cause = cause;

        this.ex = generateException();
    }

    //
    // Interface: Field Access

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
    // Implementation: Throw Exception

    public void throwException() throws IOException {
        if (ex instanceof IOException)
            throw (IOException) ex;
        if (ex instanceof RuntimeException)
            throw (RuntimeException) ex;
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
    // Implementation: Query, Existence

    public boolean exists() throws IOException {
        if (isSuccess())
            return true;
        if (status() == Result.FAIL_NAME_NOT_FOUND || status() == Result.FAIL_NON_DIRECTORY_ENCOUNTERED)
            return false;

        throwException();
        return false;
    }

    //
    // Implementation: Query, Existence of Parent Directory

    public boolean existsParentDirectory() throws IOException {
        if (isSuccess())
            return true;
        if (status() == Result.FAIL_NAME_NOT_FOUND)
            return lastIndex >= endIndex - 1;

        throwException();
        return false;
    }

    //
    // Implementation: Query, Existence Test

    public DynNode<Space, ?> testExistence() throws IOException {
        if (isSuccess()) {
            // Exists at route
            return node();
        } else {
            // Resolution failure
            throwException();
            return null;
        }
    }

    public DynNode<Space, ?> testExistenceForCreation() throws IOException {
        if (isSuccess()) {
            // Exists at route
            return node();
        } else if (existsParentDirectory()) {
            // Parent directory exists; file does not exist
            return null;
        } else {
            // Resolution failure
            throwException();
            return null;
        }
    }

    //
    // Deprecated

    @Deprecated
    static class ExceptionUtils {
        private ExceptionUtils() {}

        public IOException getIOException(ResolutionResult<?> resolution) {
            if (resolution.ex instanceof IOException) {
                return (IOException) resolution.ex;
            }
            return null;
        }

        public RuntimeException getRuntimeException(ResolutionResult<?> resolution) {
            if (resolution.ex instanceof RuntimeException) {
                return (RuntimeException) resolution.ex;
            }
            return null;
        }
    }

}
