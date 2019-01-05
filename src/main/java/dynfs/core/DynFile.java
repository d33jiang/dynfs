package dynfs.core;

import java.io.IOException;

public abstract class DynFile<Space extends DynSpace<Space>, Node extends DynFile<Space, Node>>
        extends DynNode<Space, Node> {

    //
    // Interface Implementation: DynNode Type Attributes

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
    // Implementation Stub: DynFile I/O

    protected abstract DynFileIO getIOInterface() throws IOException;

}
