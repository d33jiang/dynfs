package dynfs.core.store;

import java.io.IOException;
import java.util.Map;

import dynfs.core.DynSpace;

public interface DynSpaceLoader<Space extends DynSpace<Space>> {
    public Space loadStore() throws IOException;
}
