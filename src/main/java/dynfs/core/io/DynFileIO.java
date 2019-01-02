package dynfs.core.io;

import java.io.IOException;

import dynfs.core.DynFile;
import dynfs.core.DynSpace;
import dynfs.template.BufferLike;

public abstract class DynFileIO<Space extends DynSpace<Space>, Node extends DynFile<Space, Node>> extends BufferLike {

    //
    // Configuration: DynFile

    private final Node file;

    public final Node getFile() {
        return file;
    }

    //
    // Construction

    protected DynFileIO(Node file) {
        this.file = file;
    }

    //
    // Interface: File Size

    @Override
    public final long size() {
        return file.size();
    }

    void setSize(long newSize) throws IOException {
        file.setSize(newSize);
    }

    //
    // Implementation Stub Exposure: Unchecked I/O

    @Override
    protected abstract void uncheckedRead(long off, byte[] dst, int dstOff, int len);

    @Override
    protected abstract void uncheckedWrite(long off, byte[] src, int srcOff, int len);

    @Override
    protected abstract byte uncheckedReadByte(long off);

    @Override
    protected abstract void uncheckedWriteByte(long off, byte val);

}
