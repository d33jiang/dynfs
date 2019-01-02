package dynfs.core.io;

import java.io.IOException;
import java.nio.file.ClosedDirectoryStreamException;
import java.nio.file.DirectoryStream;
import java.nio.file.Path;
import java.util.Iterator;

import dynfs.core.DynDirectory;
import dynfs.core.DynSpace;

public final class DynDirectoryStream<Space extends DynSpace<Space>>
        implements DirectoryStream<Path> {

    // TODO: Organize class implementation
    // TODO: Inner/anonymous class state
    // TODO: Happens-after relationships w/ close()

    //
    // Configuration: DynDirectory

    private final DynDirectory<Space, ?> dir;

    //
    // Configuration: Filter

    private final Filter<? super Path> filter;

    //
    // State: Status

    private boolean isClosed;

    @Override
    public void close() throws IOException {
        isClosed = true;
    }

    private void throwIfClosed() {
        if (isClosed)
            throw new ClosedDirectoryStreamException();
    }

    //
    // Lazy: Iterator

    private Iterator<Path> iter;

    private void initializeIterator() {
        // TODO: Implementation
    }

    @Override
    public Iterator<Path> iterator() {
        if (iter != null)
            throw new IllegalStateException("A previous call to iterator() has already been made");

        initializeIterator();
        return iter;
    }

    //
    // Construction

    // TODO: should be package-private
    public DynDirectoryStream(DynDirectory<Space, ?> dir) {
        this(dir, p -> true);
    }

    // TODO: should be package-private
    public DynDirectoryStream(DynDirectory<Space, ?> dir, Filter<? super Path> filter) {
        this.dir = dir;
        this.filter = filter;

        this.isClosed = false;

        this.iter = null;
    }

}
