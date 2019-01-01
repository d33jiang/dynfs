package dynfs.core;

import java.io.IOException;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.util.Map;

import org.apache.commons.lang3.NotImplementedException;

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

    public final void creationTime(FileTime t) {
        creationTime = t;
    }

    public final void lastModifiedTime(FileTime t) {
        lastModifiedTime = t;
    }

    public final void lastAccessTime(FileTime t) {
        lastAccessTime = t;
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
        return new DynAttributesView<Space>(node.getStore(), node.getRoute());
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

    //
    // Implementation: Read by Attribute Sets

    Map<String, Object> readAttributes(String attributes)
            throws IOException {
        // TODO: Implementation
        throw new NotImplementedException("Not yet implemented");
    }

    //
    // Implementation: Set Attribute Value

    void setAttribute(String attribute, Object value) {
        // TODO: Implementation
        throw new NotImplementedException("Not yet implemented");
    }

}
