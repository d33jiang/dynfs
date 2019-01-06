package dynfs.core;

import java.io.IOException;
import java.nio.file.ClosedDirectoryStreamException;
import java.nio.file.DirectoryIteratorException;
import java.nio.file.DirectoryStream;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.NoSuchElementException;

final class DynDirectoryStream<Space extends DynSpace<Space>>
        implements DirectoryStream<Path> {

    //
    // Configuration: DynFileSystem

    private final DynFileSystem<Space> fs;

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
        iter = new DynDirectoryStreamIterator();
    }

    @Override
    public Iterator<Path> iterator() {
        throwIfClosed();

        if (iter != null)
            throw new IllegalStateException("A previous call to iterator() has already been made");

        initializeIterator();
        return iter;
    }

    //
    // Construction

    DynDirectoryStream(DynFileSystem<Space> fs, DynDirectory<Space, ?> dir) {
        this(fs, dir, p -> true);
    }

    DynDirectoryStream(DynFileSystem<Space> fs, DynDirectory<Space, ?> dir, Filter<? super Path> filter) {
        this.fs = fs;
        this.dir = dir;
        this.filter = filter;

        this.isClosed = false;

        this.iter = null;
    }

    //
    // Support Structure: Iterator

    private class DynDirectoryStreamIterator implements Iterator<Path> {

        //
        // Configuration: DynDirectory Iterator

        private final Iterator<DynNode<Space, ?>> dirIter;

        //
        // State: Read-Ahead Cache

        private DynPath nextPath;

        //
        // Construction

        private DynDirectoryStreamIterator() {
            dirIter = dir.iterator();
            nextPath = null;
        }

        //
        // Interface: Has Next Element, Read-Ahead

        @Override
        public boolean hasNext() {
            if (nextPath != null) {
                // Next path is already cached
                return true;
            }

            if (isClosed) {
                // End of stream has been reached
                return false;
            }

            while (dirIter.hasNext()) {
                DynNode<Space, ?> node = dirIter.next();
                DynPath path = DynPath.newPath(fs, node.getRoute());

                boolean isPathAccepted = false;
                try {
                    isPathAccepted = filter.accept(path);
                } catch (IOException ex) {
                    throw new DirectoryIteratorException(ex);
                }

                if (isPathAccepted) {
                    nextPath = path;
                    return true;
                }
            }

            return false;
        }

        //
        // Interface: Get Next Element

        @Override
        public DynPath next() {
            if (hasNext()) {
                try {
                    return nextPath;
                } finally {
                    nextPath = null;
                }
            } else {
                throw new NoSuchElementException();
            }
        }

    }

}
