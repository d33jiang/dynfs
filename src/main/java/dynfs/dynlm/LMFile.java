package dynfs.dynlm;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

public class LMFile extends LMNode {

	private long size;
	private List<WeakReference<Block>> data;

	LMFile(LMNode parent, String name) {
		super(parent.getStore(), parent, name, false);
		validateName(name);

		size = 0;
		data = new ArrayList<>();
	}

	@Override
	public boolean isRegularFile() {
		return true;
	}

	@Override
	public boolean isDirectory() {
		return false;
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
		return size;
	}

}
