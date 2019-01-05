package dynfs.core;

import java.io.IOException;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.FileTime;

import dynfs.core.options.LinkOptions;

public final class DynNodeFileAttributeView implements BasicFileAttributeView {

    //
    // Constant: File Attribute View Name

    public static final String FILE_ATTRIBUTE_VIEW_NAME = "dyn.core";

    @Override
    public String name() {
        return FILE_ATTRIBUTE_VIEW_NAME;
    }

    //
    // Configuration: DynNode Reference

    private final DynSpace<?> store;
    private final DynRoute route;

    //
    // Configuration: Link Options

    private final LinkOptions linkOptions;

    //
    // Construction

    public DynNodeFileAttributeView(DynSpace<?> store, DynRoute route) {
        this(store, route, LinkOptions.newInstance(false));
    }

    public DynNodeFileAttributeView(DynSpace<?> store, DynRoute route, LinkOptions linkOptions) {
        this.store = store;
        this.route = route;

        this.linkOptions = linkOptions;
    }

    //
    // Support: DynNode Resolution

    private DynNode<?, ?> node() throws IOException {
        return store.resolve(route, !linkOptions.nofollowLinks).testExistence();
    }

    //
    // Interface: Attribute Read

    @Override
    public DynNodeFileAttributes readAttributes() throws IOException {
        return node().readAttributesAsDynNodeFileAttributes();
    }

    //
    // Interface: FileTime Attributes Write

    @Override
    public void setTimes(FileTime lastModifiedTime, FileTime lastAccessTime, FileTime creationTime)
            throws IOException {
        node().writeTimes(lastAccessTime, lastModifiedTime, creationTime);
    }

}
