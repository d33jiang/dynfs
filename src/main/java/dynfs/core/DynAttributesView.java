package dynfs.core;

import java.io.IOException;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;

import dynfs.core.options.LinkOptions;
import dynfs.core.path.DynRoute;

public final class DynAttributesView<Space extends DynSpace<Space>> implements BasicFileAttributeView {

    // TODO: Rename to DynFileAttributeView (+ "File", singular "Attribute")

    //
    // Configuration: DynNode Reference

    private final Space store;
    private final DynRoute route;

    //
    // Configuration: Link Options

    private final LinkOptions linkOptions;

    //
    // Construction

    public DynAttributesView(Space store, DynRoute route) {
        this(store, route, LinkOptions.newInstance(false));
    }

    public DynAttributesView(Space store, DynRoute route, LinkOptions linkOptions) {
        this.store = store;
        this.route = route;

        this.linkOptions = linkOptions;
    }

    //
    // Support: DynNode Resolution

    private DynNode<Space, ?> node() throws IOException {
        return store.resolve(route, !linkOptions.nofollowLinks).testExistence();
    }

    //
    // Interface: BasicFileAttributesView

    @Override
    public String name() {
        return route.getFileName();
    }

    @Override
    public void setTimes(FileTime lastModifiedTime, FileTime lastAccessTime, FileTime createTime)
            throws IOException {
        DynAttributes<Space> attr = readAttributes();
        // TODO: create method in DynAttributes to duplicate all fields in it and
        // subclasses and change times
        // TODO: invoke writeAttributes
        // TODO: should DynNode provide explicit support for updating only the times?
        // only modified&access? only access? all three?
        attr.creationTime(createTime);
        attr.lastModifiedTime(lastModifiedTime);
        attr.lastAccessTime(lastAccessTime);
        // TODO: Beware of read-only DynSpace instances
    }

    @Override
    public DynAttributes<Space> readAttributes() throws IOException {
        return node().attributes();
    }

    // TODO: private DynAttributes<Space> writeAttributes()
    // TODO: Beware of read-only DynSpace instances
    // TODO: DynAttributes & subclasses: DynAttributes.getBuilder() copies all
    // writable state into builder; actually, have DynAttributes separate writable
    // attributes from non-writable attributes

}
