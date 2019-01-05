package dynfs.template;

import java.io.IOException;

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

    protected final void checkBufferInterval(long off, long len) throws IOException {
        checkLength("len", len);
        checkInterval("off", off, "len", len, "the buffer", size());
    }

    protected final void checkBufferOffset(long off) throws IOException {
        checkOffset("off", off, "the buffer", size());
    }

    //
    // Interface: Size

    public abstract long size() throws IOException;

    //
    // Interface: I/O

    public final void read(long off, byte[] dst, int dstOff, int len) throws IOException {
        checkBufferInterval(off, len);
        checkInterval("dstOff", dstOff, "len", len, "dst", dst.length);

        uncheckedRead(off, dst, dstOff, len);
    }

    public final byte[] read(long off, int len) throws IOException {
        checkBufferInterval(off, len);

        byte[] buf = new byte[len];
        uncheckedRead(off, buf, 0, len);

        return buf;
    }

    public final void write(long off, byte[] src, int srcOff, int len) throws IOException {
        checkBufferInterval(off, len);
        checkInterval("srcOff", srcOff, "len", len, "src", src.length);

        uncheckedWrite(off, src, srcOff, len);
    }

    public final byte readByte(long off) throws IOException {
        checkBufferOffset(off);
        return uncheckedReadByte(off);
    }

    public final void writeByte(long off, byte val) throws IOException {
        checkBufferOffset(off);
        uncheckedWriteByte(off, val);
    }

    //
    // Implementation: I/O

    protected abstract void uncheckedRead(long off, byte[] dst, int dstOff, int len) throws IOException;

    protected abstract void uncheckedWrite(long off, byte[] src, int srcOff, int len) throws IOException;

    protected abstract byte uncheckedReadByte(long off) throws IOException;

    protected abstract void uncheckedWriteByte(long off, byte val) throws IOException;

}
