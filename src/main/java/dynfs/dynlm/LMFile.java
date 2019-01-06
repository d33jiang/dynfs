package dynfs.dynlm;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.NotImplementedException;

import dynfs.core.DynFile;
import dynfs.core.DynFileIO;
import dynfs.core.DynNode;
import dynfs.core.DynNodeAttribute;

public class LMFile extends DynFile<LMSpace, LMFile> {

    //
    // State: Data

    private BlockWeakReferenceList<LMFile> data;

    //
    // State: Size

    private long size;

    @Override
    public long readSize() {
        return size;
    }

    @Override
    protected void writeSize(long size) throws IOException {
        int minCapacity = BlockLike.getIntValue(size);
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
        data = new BlockWeakReferenceList<>(store.getMemory(), this);
    }

    //
    // Core Support: Conversion to String

    @Override
    public String toString() {
        return String.format("[LMFile: %s | size = %d | blocks = %s]", getRouteString(), size, data.getBlockIndices());
    }

    //
    // Implementation: DynFileIO

    @Override
    protected DynFileIO getIOInterface() throws IOException {
        return new DynFileIO(this) {
            @Override
            protected void uncheckedWriteByte(long off, byte val) {
                data.uncheckedWriteByte(off, val);
            }

            @Override
            protected void uncheckedWrite(long off, byte[] src, int srcOff, int len) {
                data.uncheckedWrite(off, src, srcOff, len);
            }

            @Override
            protected byte uncheckedReadByte(long off) {
                return data.uncheckedReadByte(off);
            }

            @Override
            protected void uncheckedRead(long off, byte[] dst, int dstOff, int len) {
                data.uncheckedRead(off, dst, dstOff, len);
            }
        };
    }

    //
    // Implementation: I/O, Equality Check

    @Override
    protected boolean isSameFile(DynNode<LMSpace, ?> other) {
        return this == other;
    }

    //
    // Implementation: I/O, Node Deletion

    @Override
    protected void deleteImpl() throws IOException {
        writeSize(0);
    }

    //
    // Implementation: Attribute I/O

    @Override
    protected Map<DynNodeAttribute, Object> readAttributesImpl(Set<DynNodeAttribute> keys) throws IOException {
        throw new NotImplementedException("DynFile Attributes are not yet supported by LMSpace");
    }

    @Override
    protected Map<DynNodeAttribute, Object> readAllAttributes() throws IOException {
        throw new NotImplementedException("DynFile Attributes are not yet supported by LMSpace");
    }

    @Override
    protected Map<String, Object> writeAttributesImpl(Map<String, ?> newMappings) throws IOException {
        throw new NotImplementedException("DynFile Attributes are not yet supported by LMSpace");
    }

}
