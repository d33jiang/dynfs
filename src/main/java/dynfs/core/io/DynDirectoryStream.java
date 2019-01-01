package dynfs.core.io;

import java.io.IOException;
import java.nio.file.ClosedDirectoryStreamException;
import java.nio.file.DirectoryStream;
import java.nio.file.Path;
import java.util.Iterator;

import dynfs.core.DynDirectory;
import dynfs.core.DynSpace;

public class DynDirectoryStream<Space extends DynSpace<Space>>
        implements DirectoryStream<Path> {

    // TODO: Organize class implementation
    // TODO: Inner/anonymous class state
    // TODO: Happens-after relationships w/ close()

    private boolean isClosed;

    private final DynDirectory<Space, ?> dir;
    private Iterator<Path> iter;

    private final Filter<? super Path> filter;

    public DynDirectoryStream(DynDirectory<Space, ?> dir) {
        this(dir, p -> true);
    }

    public DynDirectoryStream(DynDirectory<Space, ?> dir, Filter<? super Path> filter) {
        this.isClosed = false;

        this.dir = dir;
        this.iter = null;

        this.filter = filter;
    }

    private void throwIfClosed() {
        if (isClosed)
            throw new ClosedDirectoryStreamException();
    }

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

    @Override
    public void close() throws IOException {
        isClosed = true;
    }

}
