package dynfs.core.io;

import java.io.IOException;

import dynfs.core.DynFile;
import dynfs.core.DynSpace;

public abstract class DynFileIO<Space extends DynSpace<Space>, Node extends DynFile<Space, Node>> {

    //
    // Implementation: Validation

    protected static void checkLength(String lblLen, long len) {
        if (len < 0)
            throw new IllegalArgumentException(lblLen + " must be nonnegative");
    }

    protected static void checkInterval(String lblOff, long off, String lblLen, long len, String lblArr, long size) {
        if (off < 0 || off + len > size)
            throw new IllegalArgumentException(
                    lblOff + " and " + lblLen + " do not denote an interval within " + lblArr);
    }

    protected static void checkIndex(String lblOff, long off, String lblArr, long size) {
        if (off < 0 || off >= size)
            throw new IllegalArgumentException(lblOff + " does not denote an element within " + lblArr);
    }

    protected final void checkFileInterval(long off, long len) {
        checkLength("len", len);
        checkInterval("off", off, "len", len, "the file", size());
    }

    protected final void checkFileIndex(long off) {
        checkIndex("off", off, "the file", size());
    }

    //
    // Field: Internal State

    private final Node file;

    //
    // Interface: Size

    public final long size() {
        return file.size();
    }

    protected void setSize(long newSize) throws IOException {
        file.setSize(newSize);
    }

    //
    // Interface: File

    public final Node getFile() {
        return file;
    }

    //
    // Construction

    protected DynFileIO(Node file) {
        this.file = file;
    }

    //
    // Interface: I/O

    public final void read(long off, byte[] dst, int dstOff, int len) {
        checkFileInterval(off, len);
        checkInterval("dstOff", dstOff, "len", len, "dst", dst.length);

        uncheckedRead(off, dst, dstOff, len);
    }

    public final byte[] read(long off, int len) {
        checkFileInterval(off, len);

        byte[] buf = new byte[len];
        uncheckedRead(off, buf, 0, len);

        return buf;
    }

    public final void write(long off, byte[] src, int srcOff, int len) {
        checkFileInterval(off, len);
        checkInterval("srcOff", srcOff, "len", len, "src", src.length);

        uncheckedWrite(off, src, srcOff, len);
    }

    public final byte readByte(long off) {
        checkFileIndex(off);
        return uncheckedReadByte(off);
    }

    public final void writeByte(long off, byte val) {
        checkFileIndex(off);
        uncheckedWriteByte(off, val);
    }

    // Implementation: I/O

    protected abstract void uncheckedRead(long off, byte[] dst, int dstOff, int len);

    protected abstract void uncheckedWrite(long off, byte[] src, int srcOff, int len);

    protected final void uncheckedTransfer(long off, byte[] other, int otherOff, int len, boolean read) {
        if (read) {
            uncheckedRead(off, other, otherOff, len);
        } else {
            uncheckedWrite(off, other, otherOff, len);
        }
    }

    protected abstract byte uncheckedReadByte(long off);

    protected abstract void uncheckedWriteByte(long off, byte val);

}
