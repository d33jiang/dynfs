package dynfs.core;

import java.io.IOException;

public abstract class DynLink<Space extends DynSpace<Space>, Node extends DynLink<Space, Node>>
        extends DynNode<Space, Node> {

    //
    // Interface Implementation: DynNode Type Attributes

    @Override
    public final boolean isSymbolicLink() {
        return true;
    }

    //
    // Construction

    protected <DirNode extends DynDirectory<Space, DirNode>> DynLink(Space store, DirNode parent, String name) {
        super(store, parent, name);
        validateName(name);
    }

    //
    // Implementation Stub: Link Target

    public final DynRoute follow() throws IOException {
        // TODO: Check access control
        return followImpl();
    }

    protected abstract DynRoute followImpl() throws IOException;

}
