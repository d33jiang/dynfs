package dynfs.core.store;

import java.io.IOException;

import dynfs.core.DynSpace;

public interface DynSpaceLoader<Space extends DynSpace<Space>> {
    public Space loadStore() throws IOException;
}
