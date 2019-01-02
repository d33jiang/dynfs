package dynfs.core;

import java.io.IOException;

import dynfs.core.io.DynFileIO;

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
    // Implementation Stub: DynFile Size

    // TODO: concrete implementation of package-private setSize + protected abstract
    // ensureCapacity / limitCapacity?
    public abstract void setSize(long newSize) throws IOException;

    //
    // Implementation Stub: DynFile I/O

    // TODO: Change to protected
    public abstract DynFileIO<Space, Node> getIOInterface() throws IOException;

}
