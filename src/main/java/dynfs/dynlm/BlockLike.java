package dynfs.dynlm;

abstract class BlockLike {

	//
	// Validation

	protected static void checkLength(String lblLen, int len) {
		if (len < 0)
			throw new IllegalArgumentException(lblLen + " must be nonnegative");
	}

	protected static void checkInterval(String lblOff, int off, String lblLen, int len, String lblArr, int size) {
		if (off < 0 || off + len > size)
			throw new IllegalArgumentException(
					lblOff + " and " + lblLen + " do not denote an interval within " + lblArr);
	}

	protected static void checkIndex(String lblOff, int off, String lblArr, int size) {
		if (off < 0 || off >= size)
			throw new IllegalArgumentException(lblOff + " does not denote an element within " + lblArr);
	}

	protected final void checkBlockInterval(int off, int len) {
		checkLength("len", len);
		checkInterval("off", off, "len", len, "the block", size());
	}

	protected final void checkBlockIndex(int off) {
		checkIndex("off", off, "the block", size());
	}

	//
	// Internal State

	private int size;
	private LMFile owner;

	//
	// Construction

	protected BlockLike(int initialSize) {
		this(initialSize, null);
	}

	protected BlockLike(int initialSize, LMFile initialOwner) {
		setSize(initialSize);
		setOwner(initialOwner);
	}

	//
	// Size

	public final int size() {
		return size;
	}

	protected final int setSize(int newSize) {
		int temp = this.size;
		this.size = newSize;
		return temp;
	}

	//
	// Owner

	public final LMFile getOwner() {
		return owner;
	}

	protected final LMFile setOwner(LMFile newOwner) {
		LMFile temp = this.owner;
		this.owner = newOwner;
		return temp;
	}

	//
	// Interface: I/O

	public final void read(int off, byte[] dst, int dstOff, int len) {
		checkBlockInterval(off, len);
		checkInterval("dstOff", dstOff, "len", len, "dst", dst.length);

		uncheckedRead(off, dst, dstOff, len);
	}

	public final byte[] read(int off, int len) {
		checkBlockInterval(off, len);

		byte[] buf = new byte[len];
		uncheckedRead(off, buf, 0, len);

		return buf;
	}

	public final void write(int off, byte[] src, int srcOff, int len) {
		checkBlockInterval(off, len);
		checkInterval("srcOff", srcOff, "len", len, "src", src.length);

		uncheckedWrite(off, src, srcOff, len);
	}

	public final byte readByte(int off) {
		checkBlockIndex(off);
		return uncheckedReadByte(off);
	}

	public final void writeByte(int off, byte val) {
		checkBlockIndex(off);
		uncheckedWriteByte(off, val);
	}

	// Implementation: I/O

	protected abstract void uncheckedRead(int off, byte[] dst, int dstOff, int len);

	protected abstract void uncheckedWrite(int off, byte[] src, int srcOff, int len);

	protected final void uncheckedTransfer(int off, byte[] other, int otherOff, int len, boolean read) {
		if (read) {
			uncheckedRead(off, other, otherOff, len);
		} else {
			uncheckedWrite(off, other, otherOff, len);
		}
	}

	protected abstract byte uncheckedReadByte(int off);

	protected abstract void uncheckedWriteByte(int off, byte val);
}
