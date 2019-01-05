package dynfs.core;

import java.io.IOException;

import dynfs.template.BufferLike;

public abstract class DynFileIO extends BufferLike {

    //
    // Configuration: DynFile

    private final DynFile<?, ?> file;

    //
    // Construction

    protected DynFileIO(DynFile<?, ?> file) {
        this.file = file;
    }

    //
    // Interface: DynFile Size

    @Override
    public final long size() throws IOException {
        return file.readSize();
    }

    void setSize(long newSize) throws IOException {
        file.writeSize(newSize);
    }

    //
    // Implementation Stub Exposure: Unchecked I/O

    @Override
    protected abstract void uncheckedRead(long off, byte[] dst, int dstOff, int len) throws IOException;

    @Override
    protected abstract void uncheckedWrite(long off, byte[] src, int srcOff, int len) throws IOException;

    @Override
    protected abstract byte uncheckedReadByte(long off) throws IOException;

    @Override
    protected abstract void uncheckedWriteByte(long off, byte val) throws IOException;

}
