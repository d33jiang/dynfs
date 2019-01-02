package dynfs.dynlm;

import java.io.IOException;

import org.apache.commons.lang3.NotImplementedException;

import dynfs.core.DynFile;
import dynfs.core.DynNode;
import dynfs.core.io.DynFileIO;

public class LMFile extends DynFile<LMSpace, LMFile> {

    //
    // Field: Data

    private BlockList data;
    private long size;

    //
    // Interface: Data

    BlockLike getData() {
        // TODO: Restrict interface to expose only read/write methods?
        return data;
    }

    //
    // Interface: Size

    @Override
    public long size() {
        return size;
    }

    @Override
    public void setSize(long size) throws IOException {
        int minCapacity = LMSpace.getIntValue(size);
        if (size > this.size) {
            this.data.ensureCapacity(minCapacity);
        } else {
            this.data.trimCapacity(minCapacity);
        }

        this.size = size;
    }

    //
    // Construction

    protected LMFile(LMSpace store, LMDirectory parent, String name) throws IOException {
        super(store, parent, name);

        size = 0;
        data = new BlockList(store.getMemory(), this);
    }

    //
    // Interface: DynFileIO

    @Override
    public DynFileIO<LMSpace, LMFile> getIOInterface() throws IOException {
        return new DynFileIO<LMSpace, LMFile>(this) {
            @Override
            protected void uncheckedWriteByte(long off, byte val) {
                data.uncheckedWriteByte(LMSpace.getIntValue(off), val);
            }

            @Override
            protected void uncheckedWrite(long off, byte[] src, int srcOff, int len) {
                data.uncheckedWrite(LMSpace.getIntValue(off), src, srcOff, len);
            }

            @Override
            protected byte uncheckedReadByte(long off) {
                return data.uncheckedReadByte(LMSpace.getIntValue(off));
            }

            @Override
            protected void uncheckedRead(long off, byte[] dst, int dstOff, int len) {
                data.uncheckedRead(LMSpace.getIntValue(off), dst, dstOff, len);
            }
        };
    }

    //
    // Interface: Equals Node

    @Override
    public boolean isSameFile(DynNode<LMSpace, ?> other) {
        return this == other;
    }

    @Override
    protected void deleteImpl() throws IOException {
        setSize(0);
    }

    // TODO: DEBUG
    @Override
    public String toString() {
        return getRouteString() + " -> " + size;
    }

    @Override
    protected void setAttributeImpl(String attribute, Object value) throws IOException {
        // TODO: Auto-generated method stub
        throw new NotImplementedException("Method stub");
    }
}
