package dynfs.core.file;

import dynfs.core.DynSpace;

public abstract class DynRootDirectory<Space extends DynSpace<Space>, Node extends DynRootDirectory<Space, Node>>
        extends DynDirectory<Space, Node> {

    //
    // Construction

    protected DynRootDirectory(Space store) {
        super(store);
    }

}
