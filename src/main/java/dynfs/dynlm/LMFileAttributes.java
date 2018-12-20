package dynfs.dynlm;

import java.io.IOException;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.time.Instant;

public class LMFileAttributes implements BasicFileAttributes {

	private final LMNode n;

	private FileTime lastModifiedTime;
	private FileTime lastAccessTime;
	private FileTime creationTime;

	public LMFileAttributes(LMNode n) {
		this.n = n;

		FileTime t = FileTime.from(Instant.now());
		this.lastModifiedTime = t;
		this.lastAccessTime = t;
		this.creationTime = t;
	}

	@Override
	public FileTime lastModifiedTime() {
		return lastModifiedTime;
	}

	@Override
	public FileTime lastAccessTime() {
		return lastAccessTime;
	}

	@Override
	public FileTime creationTime() {
		return creationTime;
	}

	@Override
	public boolean isRegularFile() {
		return n.isRegularFile();
	}

	@Override
	public boolean isDirectory() {
		return n.isDirectory();
	}

	@Override
	public boolean isSymbolicLink() {
		return n.isSymbolicLink();
	}

	@Override
	public boolean isOther() {
		return n.isOther();
	}

	@Override
	public long size() {
		return n.size();
	}

	@Override
	public Object fileKey() {
		return System.identityHashCode(n);
	}

	public BasicFileAttributeView basicFileAttributeView() {
		return new BasicFileAttributeView() {
			@Override
			public void setTimes(FileTime lastModifiedTime, FileTime lastAccessTime, FileTime createTime)
					throws IOException {
				LMFileAttributes.this.lastModifiedTime = lastModifiedTime;
				LMFileAttributes.this.lastAccessTime = lastAccessTime;
				LMFileAttributes.this.creationTime = createTime;
			}

			@Override
			public BasicFileAttributes readAttributes() throws IOException {
				return LMFileAttributes.this;
			}

			@Override
			public String name() {
				return n.getName();
			}
		};
	}

}
