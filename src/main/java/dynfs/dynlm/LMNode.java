package dynfs.dynlm;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import dynfs.core.DynSpace;

public abstract class LMNode implements Iterable<LMNode> {

	//
	// Validation

	protected static void validateName(String name) {
		if (name == null)
			throw new NullPointerException("Core node name cannot be null");
		if (name.isEmpty())
			throw new IllegalArgumentException("Core node name must be non-empty");
		if (name.contains("/"))
			throw new IllegalArgumentException("Core node name cannot contain '/'");
	}

	//
	// Identity

	private final DynSpace store;
	private final LMNode parent;
	private final String name;

	//
	// Children

	private final Map<String, LMNode> children;

	//
	// Construction

	LMNode(DynSpace store, LMNode parent, String name, boolean hasChildren) {
		this.store = store;
		this.parent = parent;
		this.name = name;

		if (hasChildren) {
			children = new HashMap<>();
		} else {
			children = null;
		}

		this.attr = new LMFileAttributes(this);
	}

	//
	// Identity

	public final String getName() {
		return name;
	}

	public final LMNode getParent() {
		return parent;
	}

	public final DynSpace getStore() {
		return store;
	}

	//
	// Path

	private final void __buildPath(StringBuilder sb) {
		if (parent != null) {
			parent.__buildPath(sb);
		}

		if (getName() != null) {
			sb.append(getName());
		}
		if (isDirectory()) {
			sb.append('/');
		}
	}

	public final String getPath() {
		StringBuilder sb = new StringBuilder();
		__buildPath(sb);
		return sb.toString();
	}

	//
	// Children

	@Override
	public final Iterator<LMNode> iterator() {
		return children.values().iterator();
	}

	public final LMNode resolve(String name) {
		return children.get(name);
	}

	//
	// Attributes

	private final LMFileAttributes attr;

	public abstract boolean isRegularFile();

	public abstract boolean isDirectory();

	public abstract boolean isSymbolicLink();

	public abstract boolean isOther();

	public abstract long size();

}
