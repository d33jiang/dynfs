package dynfs.core.io;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SeekableByteChannel;
import java.util.Arrays;

import dynfs.core.DynFile;
import dynfs.core.DynSpace;
import dynfs.core.options.OpenOptions;

public class DynByteChannel<Space extends DynSpace<Space>>
        implements SeekableByteChannel {

    // TODO: Implement use of OpenOptions
    // NOTE: OpenOptions.DSYNC is ignored; all data updates are synchronous
    // NOTE: OpenOptions.SYNC is ignored; all updates are synchronous
    // NOTE: OpenOptions.SPARSE is ignored; unsupported feature

    //
    // Field: State

    private boolean isClosed;
    private final DynFile<Space, ?> file;
    private long position;
    private final boolean isReadOnly;
    private final boolean deleteOnClose;

    //
    // Construction

    public DynByteChannel(DynFile<Space, ?> file, OpenOptions options) throws IOException {
        if (file == null)
            throw new NullPointerException("file must be non-null");

        this.isClosed = false;
        this.file = file;
        this.position = 0;

        this.isReadOnly = !(options.append || options.write);

        if (options.truncateExisting) {
            truncate(0);
        }
        if (options.append) {
            this.position = size();
        }

        this.deleteOnClose = options.deleteOnClose;
    }

    //
    // Status

    @Override
    public boolean isOpen() {
        return !isClosed;
    }

    private void throwIfClosed() throws ClosedChannelException {
        if (isClosed)
            throw new ClosedChannelException();
    }

    @Override
    public void close() throws IOException {
        this.isClosed = true;
        if (deleteOnClose) {
            file.delete();
        }
    }

    //
    // Position

    private static long verifyPosition(long size, String label) {
        if (size < 0)
            throw new IllegalArgumentException(label + " must be nonnegative");

        return size;
    }

    @Override
    public long position() throws IOException {
        return position;
    }

    @Override
    public SeekableByteChannel position(long newPosition) throws IOException {
        throwIfClosed();
        position = verifyPosition(newPosition, "newPosition");
        return this;
    }

    //
    // I/O: File Access

    private DynFileIO<Space, ?> file() throws IOException {
        return file.getIOInterface();
    }

    private void throwIfReadOnly() throws IOException {
        if (isReadOnly)
            throw new UnsupportedOperationException("This DynByteChannel is read-only");
    }

    //
    // I/O: File Size

    @Override
    public long size() throws IOException {
        throwIfClosed();
        return file().size();
    }

    @Override
    public SeekableByteChannel truncate(long size) throws IOException {
        throwIfClosed();
        throwIfReadOnly();
        file().setSize(verifyPosition(size, "size"));
        return this;
    }

    //
    // I/O: Read / Write

    private final byte[] buf = new byte[512];

    private static int truncateLongToInt(long v) {
        if (v > Integer.MAX_VALUE)
            return Integer.MAX_VALUE;
        else if (v < Integer.MIN_VALUE)
            return Integer.MIN_VALUE;
        else
            return (int) v;
    }

    @Override
    public int read(ByteBuffer dst) throws IOException {
        throwIfClosed();
        // TODO: Mechanism for interrupting read on close? (requires synchronization w/
        // throwIfClosed)

        long fileSize = file().size();
        int bytesToRead;

        int remainderOfFile = truncateLongToInt(fileSize - position);
        if (remainderOfFile > 0) {
            bytesToRead = Math.min(dst.remaining(), remainderOfFile);
            for (int rem = bytesToRead; rem > 0;) {
                int bytesRead = Math.min(rem, buf.length);
                file().uncheckedRead(position, buf, 0, bytesRead);
                dst.put(buf, 0, bytesRead);
                position += bytesRead;
                rem -= bytesRead;
            }
        } else {
            bytesToRead = -1;
            position = fileSize;
        }

        return bytesToRead;
    }

    @Override
    public int write(ByteBuffer src) throws IOException {
        throwIfClosed();
        throwIfReadOnly();
        // TODO: Mechanism for interrupting write on close? (requires synchronization w/
        // throwIfClosed)

        long fileSize = file().size();
        int bytesToWrite = src.remaining();

        long requireMinimumFileSize = position + bytesToWrite;
        if (requireMinimumFileSize > fileSize) {
            file().setSize(requireMinimumFileSize);
        }

        if (position > fileSize) {
            Arrays.fill(buf, (byte) 0);
            for (long off = fileSize; off < position;) {
                int bytesCleared = truncateLongToInt(Math.min(buf.length, position - off));
                file().uncheckedWrite(off, buf, 0, bytesCleared);
                off += bytesCleared;
            }
        }

        for (int rem = bytesToWrite; rem > 0;) {
            int bytesWritten = Math.min(buf.length, rem);
            src.get(buf, 0, bytesWritten);
            file().uncheckedWrite(position, buf, 0, bytesWritten);
            position += bytesWritten;
            rem -= bytesWritten;
        }

        return bytesToWrite;
    }

}
