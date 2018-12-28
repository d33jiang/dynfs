package dynfs.core.file;

import dynfs.core.DynSpace;

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

}
