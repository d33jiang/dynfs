package dynfs.dynlm;

import java.io.IOException;

import dynfs.template.BufferLike;

abstract class BlockLike extends BufferLike {

    //
    // Field: Internal State

    private int capacity;
    private LMFile owner;

    //
    // Interface: Capacity

    public final int capacity() {
        return capacity;
    }

    @Override
    public final long size() {
        return capacity;
    }

    public final void ensureCapacity(int minCapacity) throws IOException {
        if (minCapacity <= capacity) {
            return;
        }

        int newCapacity = ensureCapacityImpl(minCapacity);
        if (newCapacity < minCapacity)
            throw new AssertionError("The new capacity must be at least minCapacity if ensureCapacityImpl returns");

        capacity = newCapacity;
    }

    protected abstract int ensureCapacityImpl(int minCapacity) throws IOException;

    public final void trimCapacity(int minCapacity) throws IOException {
        if (minCapacity > capacity)
            throw new IllegalArgumentException("minCapacity is greater than current capacity");
        if (minCapacity == capacity)
            return;

        int newCapacity = trimCapacityImpl(minCapacity);
        if (newCapacity < minCapacity)
            throw new AssertionError("The new capacity must be at least minCapacity");
        if (newCapacity < 0)
            throw new AssertionError("The new capacity must be nonnegative");

    }

    protected abstract int trimCapacityImpl(int minCapacity) throws IOException;

    //
    // Interface: Owner

    public final LMFile getOwner() {
        return owner;
    }

    protected final LMFile setOwner(LMFile newOwner) {
        LMFile temp = this.owner;
        this.owner = newOwner;
        return temp;
    }

    //
    // Construction

    protected BlockLike(int initialCapacity) throws IOException {
        this(initialCapacity, null);
    }

    protected BlockLike(int initialCapacity, LMFile initialOwner) throws IOException {
        checkLength("initialCapacity", initialCapacity);
        this.capacity = initialCapacity;
        this.owner = initialOwner;
    }

    //
    // Implementation Stub Redefinition

    protected abstract void uncheckedRead(int off, byte[] dst, int dstOff, int len);

    protected abstract void uncheckedWrite(int off, byte[] src, int srcOff, int len);

    final void uncheckedTransfer(int off, byte[] other, int otherOff, int len, boolean read) {
        if (read) {
            uncheckedRead(off, other, otherOff, len);
        } else {
            uncheckedWrite(off, other, otherOff, len);
        }
    }

    protected abstract byte uncheckedReadByte(int off);

    protected abstract void uncheckedWriteByte(int off, byte val);

    @Override
    protected final void uncheckedRead(long off, byte[] dst, int dstOff, int len) {
        uncheckedRead(LMSpace.getIntValue(off), dst, dstOff, len);
    }

    @Override
    protected final void uncheckedWrite(long off, byte[] src, int srcOff, int len) {
        uncheckedWrite(LMSpace.getIntValue(off), src, srcOff, len);
    }

    @Override
    protected final byte uncheckedReadByte(long off) {
        return uncheckedReadByte(LMSpace.getIntValue(off));
    }

    @Override
    protected final void uncheckedWriteByte(long off, byte val) {
        uncheckedWriteByte(LMSpace.getIntValue(off), val);
    }

}
