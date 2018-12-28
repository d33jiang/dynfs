package dynfs.core.store;

import java.io.IOException;
import java.util.Map;

import dynfs.core.DynSpace;

public interface DynSpaceFactory<Space extends DynSpace<Space>> {
    public Space createStore(Map<String, ?> env) throws IOException;
}
