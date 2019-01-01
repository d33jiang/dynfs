package dynfs.core;

import java.io.IOException;
import java.nio.file.LinkOption;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;

import dynfs.core.options.LinkOptions;
import dynfs.core.path.DynRoute;

public class DynAttributesView<Space extends DynSpace<Space>> implements BasicFileAttributeView {

    //
    // Field: Reference

    private final Space store;
    private final DynRoute route;

    private final LinkOptions linkOptions;

    //
    // Construction

    public DynAttributesView(Space store, DynRoute route) {
        this(store, route, LinkOptions.parse(new LinkOption[0]));
    }

    public DynAttributesView(Space store, DynRoute route, LinkOptions linkOptions) {
        this.store = store;
        this.route = route;

        this.linkOptions = linkOptions;
    }

    //
    // Helper: Node

    private DynNode<Space, ?> node() throws IOException {
        return store.resolve(route, !linkOptions.nofollowLinks).testExistence();
    }

    //
    // Interface: BasicFileAttributesView

    @Override
    public String name() {
        return route.getFileNameAsString();
    }

    @Override
    public void setTimes(FileTime lastModifiedTime, FileTime lastAccessTime, FileTime createTime)
            throws IOException {
        DynAttributes<Space, ?> attr = node().attributes();
        attr.creationTime(createTime);
        attr.lastModifiedTime(lastModifiedTime);
        attr.lastAccessTime(lastAccessTime);
    }

    @Override
    public BasicFileAttributes readAttributes() throws IOException {
        return node().attributes();
    }

}
