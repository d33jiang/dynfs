package dynfs.core.store;

import java.util.Map;

import dynfs.core.DynSpace;

public interface DynSpaceFactory<T extends DynSpace> {
	public DynSpace createStore(Map<String, ?> env);
}
