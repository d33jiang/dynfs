package dynfs.dynlm;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.ClosedFileSystemException;
import java.util.Arrays;

public class LMByteChannel implements SeekableByteChannel {

	private boolean isClosed;
	private final WeakReference<LMFile> file;
	int position;

	//
	// Construction

	LMByteChannel(LMFile file) {
		if (file == null)
			throw new NullPointerException("file must be non-null");

		this.isClosed = false;
		this.file = new WeakReference<>(file);
		this.position = 0;
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
	}

	//
	// Position

	@Override
	public long position() throws IOException {
		return position;
	}

	private static int verifySize(long size, String label) {
		if (size < 0)
			throw new IllegalArgumentException(label + " must be nonnegative");
		if (size > Integer.MAX_VALUE)
			throw new UnsupportedOperationException("long sized indices are not supported");
		return (int) size;
	}
	
	@Override
	public SeekableByteChannel position(long newPosition) throws IOException {
		throwIfClosed();
		position = verifySize(newPosition, "newPosition");
		return this;
	}

	//
	// I/O: File Size

	private LMFile file() {
		LMFile f = file.get();
		if (f == null)
			throw new ClosedFileSystemException();
		return f;
	}
	
	@Override
	public long size() throws IOException {
		throwIfClosed();
		return file().size();
	}

	@Override
	public SeekableByteChannel truncate(long size) throws IOException {
		throwIfClosed();
		file().setSize(verifySize(size, "size"));
		return this;
	}

	//
	// I/O: Read / Write

	private final byte[] buf = new byte[512];
	
	@Override
	public int read(ByteBuffer dst) throws IOException {
		throwIfClosed();
		// Mechanism for interrupting write on close? (requires synchronization w/
		// throwIfClosed)
		
		int fileSize = (int) file().size();
		int bytesToRead;
		
		int remainderOfFile = fileSize - position;
		if (remainderOfFile >= 0) {
			bytesToRead = Math.min(dst.remaining(), remainderOfFile);
			for (int rem = bytesToRead; rem > 0;) {
				int bytesRead = Math.min(rem, buf.length);
				file().getData().uncheckedRead(position, buf, 0, bytesRead);
				dst.put(buf, 0, bytesRead);
				position += bytesRead;
				rem -= bytesRead;
			}
		} else {
			bytesToRead = 0;
			position = fileSize;
		}
		
		return bytesToRead;
	}

	@Override
	public int write(ByteBuffer src) throws IOException {
		throwIfClosed();
		// Mechanism for interrupting write on close? (requires synchronization w/
		// throwIfClosed)
		// TODO Implementation: Method

		int fileSize = (int) file().size();
		int bytesToWrite = src.remaining();
		
		int requireMinimumFileSize = position + bytesToWrite;
		if (requireMinimumFileSize > fileSize) {
			file().setSize(requireMinimumFileSize);
		}
		
		if (position > fileSize) {
			Arrays.fill(buf, (byte) 0);
			for (int off = fileSize; off < position;) {
				int bytesCleared = Math.min(buf.length, position - off);
				file().getData().uncheckedWrite(off, buf, 0, bytesCleared);
				off += bytesCleared;
			}
		}
		
		for (int rem = bytesToWrite; rem > 0;) {
			int bytesWritten = Math.min(buf.length, rem);
			src.get(buf, 0, bytesWritten);
			file().getData().uncheckedWrite(position, buf, 0, bytesWritten);
			position += bytesWritten;
			rem -= bytesWritten;
		}
		
		return bytesToWrite;
	}

}
