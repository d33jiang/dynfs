package dynfs.dynlm;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.ClosedFileSystemException;

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

	@Override
	public SeekableByteChannel position(long newPosition) throws IOException {
		throwIfClosed();

		if (newPosition > Integer.MAX_VALUE)
			throw new UnsupportedOperationException("long sized positions are not supported");

		position = (int) newPosition;

		return this;
	}

	//
	// I/O: File Size

	@Override
	public long size() throws IOException {
		throwIfClosed();

		LMFile f = file.get();
		if (f == null)
			throw new ClosedFileSystemException();

		return f.size();
	}

	@Override
	public SeekableByteChannel truncate(long size) throws IOException {
		throwIfClosed();

		// TODO Implementation: Method

		return this;
	}

	//
	// I/O: Read / Write

	@Override
	public int read(ByteBuffer dst) throws IOException {
		throwIfClosed();

		// Mechanism for interrupting read on close? (requires synchronization w/
		// throwIfClosed)
		// TODO Implementation: Method

		return 0;
	}

	@Override
	public int write(ByteBuffer src) throws IOException {
		throwIfClosed();

		// Mechanism for interrupting write on close? (requires synchronization w/
		// throwIfClosed)
		// TODO Implementation: Method

		return 0;
	}

}
