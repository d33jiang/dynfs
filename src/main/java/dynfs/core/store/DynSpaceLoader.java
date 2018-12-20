package dynfs.core.store;

import java.util.Map;

import dynfs.core.DynSpace;

public interface DynSpaceLoader<T extends DynSpace> {
	public DynSpace loadStore(Map<String, ?> env);
}
