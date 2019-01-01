package dynfs.dynlm;

import java.io.Closeable;
import java.io.IOException;
import java.util.List;

public interface Allocator<Owner> extends Closeable {

    //
    // Interface: Memory Management

    public List<Block> allocateBlocks(Owner f, int nblocks) throws IOException;

    public void freeBlocks(Owner f, Iterable<Block> blocks);

    //
    // Interface: Close

    @Override
    public void close() throws IOException;
}
