package dynfs.template;

public abstract class BufferLike {

    //
    // Helper: Validation

    protected static void checkLength(String lblLen, long len) {
        if (len < 0)
            throw new IllegalArgumentException(lblLen + " must be nonnegative");
    }

    protected static void checkInterval(String lblOff, long off, String lblLen, long len, String lblArr, long size) {
        if (off < 0 || off + len > size)
            throw new IllegalArgumentException(
                    lblOff + " and " + lblLen + " do not denote an interval within " + lblArr);
    }

    protected static void checkOffset(String lblOff, long off, String lblArr, long size) {
        if (off < 0 || off >= size)
            throw new IllegalArgumentException(lblOff + " does not denote an offset within " + lblArr);
    }

    protected final void checkBufferInterval(long off, long len) {
        checkLength("len", len);
        checkInterval("off", off, "len", len, "the buffer", size());
    }

    protected final void checkBufferOffset(long off) {
        checkOffset("off", off, "the buffer", size());
    }

    //
    // Interface: Size

    public abstract long size();

    //
    // Interface: I/O

    public final void read(long off, byte[] dst, int dstOff, int len) {
        checkBufferInterval(off, len);
        checkInterval("dstOff", dstOff, "len", len, "dst", dst.length);

        uncheckedRead(off, dst, dstOff, len);
    }

    public final byte[] read(long off, int len) {
        checkBufferInterval(off, len);

        byte[] buf = new byte[len];
        uncheckedRead(off, buf, 0, len);

        return buf;
    }

    public final void write(long off, byte[] src, int srcOff, int len) {
        checkBufferInterval(off, len);
        checkInterval("srcOff", srcOff, "len", len, "src", src.length);

        uncheckedWrite(off, src, srcOff, len);
    }

    public final byte readByte(long off) {
        checkBufferOffset(off);
        return uncheckedReadByte(off);
    }

    public final void writeByte(long off, byte val) {
        checkBufferOffset(off);
        uncheckedWriteByte(off, val);
    }

    //
    // Implementation: I/O

    protected abstract void uncheckedRead(long off, byte[] dst, int dstOff, int len);

    protected abstract void uncheckedWrite(long off, byte[] src, int srcOff, int len);

    protected abstract byte uncheckedReadByte(long off);

    protected abstract void uncheckedWriteByte(long off, byte val);

}
