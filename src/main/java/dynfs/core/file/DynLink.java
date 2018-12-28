package dynfs.core.file;

import dynfs.core.DynSpace;
import dynfs.core.path.DynRoute;

public abstract class DynLink<Space extends DynSpace<Space>, Node extends DynLink<Space, Node>>
        extends DynNode<Space, Node> {

    //
    // Implementation: Attributes

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
    // Implementation Stub: Link Target (Abstract)

    public abstract DynRoute follow();

}
