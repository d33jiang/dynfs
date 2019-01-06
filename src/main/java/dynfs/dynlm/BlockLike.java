package dynfs.dynlm;

import java.io.IOException;

import dynfs.template.BufferLike;

abstract class BlockLike<Owner> extends BufferLike {

    //
    // State: Capacity

    private int capacity;

    public final int capacity() {
        return capacity;
    }

    //
    // State: Owner

    private Owner owner;

    public final Owner getOwner() {
        return owner;
    }

    protected final Owner setOwner(Owner newOwner) {
        Owner oldOwner = this.owner;
        this.owner = newOwner;
        return oldOwner;
    }

    //
    // Construction

    protected BlockLike(int initialCapacity) throws IOException {
        this(initialCapacity, null);
    }

    protected BlockLike(int initialCapacity, Owner initialOwner) throws IOException {
        checkLength("initialCapacity", initialCapacity);
        this.capacity = initialCapacity;
        this.owner = initialOwner;
    }

    //
    // Interface: Size

    @Override
    public final long size() {
        return capacity;
    }

    //
    // Implementation Stub: Capacity

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
    // Implementation Stub Redefinition: Integer Offsets in I/O

    protected static int getIntValue(long v) {
        if (v > Integer.MAX_VALUE || v < Integer.MIN_VALUE)
            throw new UnsupportedOperationException("Long values are not supported");

        return (int) v;
    }

    protected abstract void uncheckedRead(int off, byte[] dst, int dstOff, int len);

    protected abstract void uncheckedWrite(int off, byte[] src, int srcOff, int len);

    protected abstract byte uncheckedReadByte(int off);

    protected abstract void uncheckedWriteByte(int off, byte val);

    @Override
    protected final void uncheckedRead(long off, byte[] dst, int dstOff, int len) {
        uncheckedRead(getIntValue(off), dst, dstOff, len);
    }

    @Override
    protected final void uncheckedWrite(long off, byte[] src, int srcOff, int len) {
        uncheckedWrite(getIntValue(off), src, srcOff, len);
    }

    @Override
    protected final byte uncheckedReadByte(long off) {
        return uncheckedReadByte(getIntValue(off));
    }

    @Override
    protected final void uncheckedWriteByte(long off, byte val) {
        uncheckedWriteByte(getIntValue(off), val);
    }

    //
    // Package Support: Unified Bulk Transfer I/O

    final void uncheckedTransfer(int off, byte[] other, int otherOff, int len, boolean read) {
        if (read) {
            uncheckedRead(off, other, otherOff, len);
        } else {
            uncheckedWrite(off, other, otherOff, len);
        }
    }

}
