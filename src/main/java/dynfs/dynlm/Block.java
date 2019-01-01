package dynfs.dynlm;

import java.io.IOException;

import dynfs.debug.Dumpable;

public class Block extends BlockLike implements Dumpable {

    //
    // Constant: Block Size

    public static final int BLOCK_SIZE = 4096;

    //
    // Interface: Block Size Calculation

    public static int numBlocks(int size) {
        return (size + BLOCK_SIZE - 1) / BLOCK_SIZE;
    }

    public static int sizeOfNBlocks(int numBlocks) {
        return numBlocks * BLOCK_SIZE;
    }

    //
    // Field: Block Management

    private int index;
    private final byte[] data;

    //
    // Construction

    Block(int index) throws IOException {
        super(BLOCK_SIZE);

        this.index = index;
        this.data = new byte[BLOCK_SIZE];
    }

    //
    // Interface: Index

    int getIndex() {
        return index;
    }

    int setIndex(int newIndex) {
        int temp = index;
        index = newIndex;
        return temp;
    }

    //
    // Interface: Size

    @Override
    protected int setSize(int newSize) {
        throw new UnsupportedOperationException("Blocks cannot be resized");
    }

    //
    // Implementation: I/O

    @Override
    protected void uncheckedRead(int off, byte[] dst, int dstOff, int len) {
        System.arraycopy(data, off, dst, dstOff, len);
    }

    @Override
    protected void uncheckedWrite(int off, byte[] src, int srcOff, int len) {
        System.arraycopy(src, srcOff, data, off, len);
    }

    @Override
    protected byte uncheckedReadByte(int off) {
        return data[off];
    }

    @Override
    protected void uncheckedWriteByte(int off, byte val) {
        data[off] = val;
    }

    //
    // Debug: Dump

    private static String byteToHexString(byte i) {
        return String.format("%02d", i);
    }

    @Override
    public void __dump(DumpBuilder db) {
        final int NUM_BYTES_PER_LINE = 32;

        db.writeLine("index: " + getIndex());
        db.writeLine("owner: " + getOwner());

        db.newline();

        db.insertIndent();
        db.write("CONTENT BEGIN");
        db.pushIndent(2);

        int lineOffset = 0;
        for (int off = 0; off < BLOCK_SIZE; off++) {
            if (lineOffset == 0) {
                db.newline();
                db.insertIndent();
            } else {
                db.write(' ');
            }

            db.write(byteToHexString(data[off]));

            if (NUM_BYTES_PER_LINE == ++lineOffset)
                lineOffset = 0;
        }
        if (lineOffset == 0)
            db.newline();

        db.popIndent();
        db.writeLine("CONTENT END");
    }

}
