package dynfs.dynlm;

import java.io.IOException;

public class LMFile extends LMNode {

	private long size;
	private BlockList data;

	LMFile(LMNode parent, String name) throws IOException {
		super(parent.getStore(), parent, name, false);
		validateName(name);

		size = 0;
		data = new BlockList(parent.getStore(), this);
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

	void setSize(int size) throws IOException {
		this.data.setSize(size);
		this.size = size;
	}
	
	BlockLike getData() {
		// TODO: Restrict interface to expose only read/write methods
		return data;
	}
	
}
