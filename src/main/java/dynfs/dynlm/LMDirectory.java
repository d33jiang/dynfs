package dynfs.dynlm;

import dynfs.core.DynSpace;

public class LMDirectory extends LMNode {

	//
	// Construction: Root Directories

	static LMDirectory createRootDirectory(DynSpace store) {
		return new LMDirectory(store);
	}

	private LMDirectory(DynSpace store) {
		super(store, null, null, true);
	}

	//
	// Regular Directories

	LMDirectory(LMNode parent, String name) {
		super(parent.getStore(), parent, name, true);
		validateName(name);
	}

	@Override
	public boolean isRegularFile() {
		return false;
	}

	@Override
	public boolean isDirectory() {
		return true;
	}

	@Override
	public boolean isSymbolicLink() {
		return false;
	}

	@Override
	public boolean isOther() {
		return false;
	}

	@Override
	public long size() {
		// NOTE: Naive recursive definition
		long s = 0;

		for (LMNode n : this) {
			s += n.size();
		}

		return s;
	}

}
