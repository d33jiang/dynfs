package dynfs.core;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.FileStore;
import java.util.Set;

import dynfs.core.store.DynFileIO;

public abstract class DynSpace extends FileStore implements Closeable {

	private static void validateSpace(long space, String label) {
		if (space < 0) {
			if (label == null)
				label = "The space argument";
			throw new IllegalArgumentException(label + " must be nonnegative");
		}
	}

	private static void validateSpace(long space, String label, long maxValue, String labelMaxValue) {
		if (space < 0) {
			if (label == null)
				label = "The space argument";
			throw new IllegalArgumentException(label + " must be nonnegative");
		}
		if (space > maxValue) {
			if (labelMaxValue == null)
				labelMaxValue = String.valueOf(maxValue);
			throw new IllegalArgumentException(label + " must be at most " + labelMaxValue);
		}
	}

	private final long totalSpace;
	private long usedSpace;

	protected DynSpace(long totalSpace) {
		this(totalSpace, 0);
	}

	protected DynSpace(long totalSpace, long usedSpace) {
		validateSpace(totalSpace, "totalSpace");
		validateSpace(usedSpace, "usedSpace", totalSpace, "totalSpace");

		this.totalSpace = totalSpace;
		this.usedSpace = usedSpace;
	}

	protected final long _getUsedSpace() {
		return usedSpace;
	}

	protected final void _setUsedSpace(long usedSpace) {
		validateSpace(usedSpace, "usedSpace", totalSpace, "totalSpace");
		this.usedSpace = usedSpace;
	}

	@Override
	public long getTotalSpace() throws IOException {
		return totalSpace;
	}

	@Override
	public long getUsableSpace() throws IOException {
		return getUnallocatedSpace();
	}

	@Override
	public long getUnallocatedSpace() throws IOException {
		return totalSpace - usedSpace;
	}

	public abstract Set<String> supportedFileAttributeViews();

	protected abstract DynFileIO getIOInterface();
}
