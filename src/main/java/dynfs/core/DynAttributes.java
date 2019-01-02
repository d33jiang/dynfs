package dynfs.core;

import java.io.IOException;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.util.Map;

import org.apache.commons.lang3.NotImplementedException;

public class DynAttributes<Space extends DynSpace<Space>>
        implements BasicFileAttributes {

    // TODO: Should represent an immutable moment-in-time snapshot of the attributes
    // of a file

    // TODO: Support for extensibility of DynAttributes class?
    // Requires design considerations across supportedFileAttributes(),
    // getFileAttributes(), getAttribute(), and setAttribute() methods and across
    // DynFileSystemProvider, DynFileSystem, DynStore, and DynNode classes

    //
    // Configuration: DynNode

    private final DynNode<Space, ?> node;

    //
    // State: DynNode Time Attributes

    // TODO: Should be final
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
    // Interface: DynNode Type Attributes

    // TODO: Should these be cached?
    // NOTE: 4 booleans -> 4 bytes; 1 reference -> 4 bytes

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
    // Interface: DynNode Size Attribute

    @Override
    public final long size() {
        return node.size();
    }

    //
    // Interface: DynNode FileKey

    @Override
    public final Object fileKey() {
        return node.fileKey();
    }

    //
    // Construction

    // TODO: protected constructor?
    public DynAttributes(DynNode<Space, ?> node) {
        this.node = node;

        FileTime t = FileTime.from(Instant.now());
        this.creationTime = t;
        this.lastModifiedTime = t;
        this.lastAccessTime = t;
    }

    //
    // Implementation: Read by Attribute Sets

    final Map<String, Object> readAttributes(String attributes)
            throws IOException {
        // TODO: Implementation
        // call impl
        // merge default in, overwriting
        // TODO: Consider access modifier
        throw new NotImplementedException("Not yet implemented");
    }

    // TODO: Call extra attributes "SpecialAttributes"?
    // TODO: protected abstract readNonDefaultAttributes(String)

    //
    // Implementation: Set Attribute Value

    final void setAttribute(String attribute, Object value) {
        // TODO: Should this method even be in this class? Consider the purpose of this
        // class
        // TODO: Implementation
        // TODO: Consider access modifier
        throw new NotImplementedException("Not yet implemented");
    }

    // TODO: protected abstract setNonDefaultAttributes(String)

}
