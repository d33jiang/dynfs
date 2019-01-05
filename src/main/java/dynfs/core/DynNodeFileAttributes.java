package dynfs.core;

import java.io.IOException;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.ImmutableSet;

public final class DynNodeFileAttributes implements BasicFileAttributes {

    //
    // Configuration: DynNode

    private final DynNode<?, ?> node;

    //
    // Configuration: DynNode Size, Cache on Read

    private final long size;

    @Override
    public final long size() {
        return size;
    }

    //
    // Configuration: DynNode Time Attributes

    private final FileTime creationTime;
    private final FileTime lastModifiedTime;
    private final FileTime lastAccessTime;

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
    // Construction

    private static final Set<DynNodeAttribute> FIELDS_AS_DYN_ATTRIBUTE_SET = ImmutableSet.of(
            DynNodeAttribute.Base.CREATION_TIME,
            DynNodeAttribute.Base.LAST_MODIFIED_TIME,
            DynNodeAttribute.Base.LAST_ACCESS_TIME);

    DynNodeFileAttributes(DynNode<?, ?> node) throws IOException {
        this.node = node;

        this.size = node.readSize();

        Map<DynNodeAttribute, Object> fileTimeAttributes = node.readAttributes(FIELDS_AS_DYN_ATTRIBUTE_SET);
        this.creationTime = (FileTime) fileTimeAttributes.get(DynNodeAttribute.Base.CREATION_TIME);
        this.lastModifiedTime = (FileTime) fileTimeAttributes.get(DynNodeAttribute.Base.LAST_MODIFIED_TIME);
        this.lastAccessTime = (FileTime) fileTimeAttributes.get(DynNodeAttribute.Base.LAST_ACCESS_TIME);
    }

    //
    // Interface: DynNode Type Attributes

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
    // Interface: DynNode FileKey

    @Override
    public final Object fileKey() {
        return node.fileKey();
    }

}
