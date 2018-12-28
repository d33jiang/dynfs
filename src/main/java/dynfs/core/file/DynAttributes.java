package dynfs.core.file;

import java.io.IOException;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.time.Instant;

import dynfs.core.DynSpace;

public class DynAttributes<Space extends DynSpace<Space>, Node extends DynNode<Space, Node>>
        implements BasicFileAttributes {

    //
    // Field: Node

    private final DynNode<Space, Node> node;

    //
    // Field: FileTime Attributes

    private FileTime creationTime;
    private FileTime lastModifiedTime;
    private FileTime lastAccessTime;

    @Override
    public final FileTime creationTime() {
        return creationTime;
    }

    @Override
    public final FileTime lastModifiedTime() {
        return lastModifiedTime;
    }

    @Override
    public final FileTime lastAccessTime() {
        return lastAccessTime;
    }

    //
    // Interface: File Attributes

    @Override
    public final boolean isRegularFile() {
        return node.isRegularFile();
    }

    @Override
    public final boolean isDirectory() {
        return node.isDirectory();
    }

    @Override
    public final boolean isSymbolicLink() {
        return node.isSymbolicLink();
    }

    @Override
    public final boolean isOther() {
        return node.isOther();
    }

    //
    // Interface: File Size

    @Override
    public final long size() {
        return node.size();
    }

    //
    // Interface: FileKey

    @Override
    public final Object fileKey() {
        return node.fileKey();
    }

    //
    // Implementation: FileAttributesView

    public final BasicFileAttributeView basicFileAttributeView() {
        return new BasicFileAttributeView() {
            @Override
            public void setTimes(FileTime lastModifiedTime, FileTime lastAccessTime, FileTime createTime)
                    throws IOException {
                // TODO: Check if closed
                DynAttributes.this.creationTime = createTime;
                DynAttributes.this.lastModifiedTime = lastModifiedTime;
                DynAttributes.this.lastAccessTime = lastAccessTime;
            }

            @Override
            public BasicFileAttributes readAttributes() throws IOException {
                // TODO: Check if closed
                return DynAttributes.this;
            }

            @Override
            public String name() {
                return node.getName();
            }
        };
    }

    //
    // Construction

    public DynAttributes(DynNode<Space, Node> node) {
        this.node = node;

        FileTime t = FileTime.from(Instant.now());
        this.creationTime = t;
        this.lastModifiedTime = t;
        this.lastAccessTime = t;
    }

}
