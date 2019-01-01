package dynfs.core;

import java.io.IOException;

import dynfs.core.io.DynFileIO;

public abstract class DynFile<Space extends DynSpace<Space>, Node extends DynFile<Space, Node>>
        extends DynNode<Space, Node> {

    //
    // Implementation: Attributes

    @Override
    public final boolean isRegularFile() {
        return true;
    }

    //
    // Construction

    protected <DirNode extends DynDirectory<Space, DirNode>> DynFile(Space store, DirNode parent, String name) {
        super(store, parent, name);
        validateName(name);
    }

    //
    // Implementation Stub: Size (Abstract)

    public abstract void setSize(long newSize) throws IOException;

    //
    // Interface: File I/O

    public abstract DynFileIO<Space, Node> getIOInterface() throws IOException;

}
