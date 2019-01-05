package dynfs.dynlm;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

import dynfs.template.Allocator;

public final class BlockList extends BlockLike {

    //
    // Configuration: Allocator

    private final Allocator<LMFile, Block> allocator;

    //
    // State: Nested Blocks

    private final NavigableMap<Integer, Block> nested;

    //
    // Construction

    BlockList(Allocator<LMFile, Block> allocator, LMFile owner, Block... nested) throws IOException {
        super(nested.length * Block.BLOCK_SIZE, owner);

        this.allocator = allocator;
        this.nested = new TreeMap<>();

        int offset = 0;
        for (Block b : nested) {
            this.nested.put(offset, b);
            offset += b.capacity();
        }
    }

    //
    // Implementation: Capacity

    @Override
    protected int ensureCapacityImpl(int minCapacity) throws IOException {
        int numNewBlocks = Block.numBlocks(minCapacity - capacity());
        int offset = capacity();

        Iterable<Block> allocatedBlocks = allocator.allocate(getOwner(), numNewBlocks);
        for (Block b : allocatedBlocks) {
            nested.put(offset, b);
            offset += b.capacity();
        }

        return offset;
    }

    @Override
    protected int trimCapacityImpl(int minCapacity) throws IOException {
        Collection<Block> toBeFreed = nested.tailMap(minCapacity).values();
        List<Block> freedBlocks = new ArrayList<>(toBeFreed);

        toBeFreed.clear();
        allocator.free(getOwner(), freedBlocks);

        return calculateTrueCapacity();
    }

    private int calculateTrueCapacity() {
        Map.Entry<Integer, Block> lastBlock = nested.lastEntry();
        return lastBlock.getKey() + lastBlock.getValue().capacity();
    }

    //
    // Implementation: I/O

    @Override
    public void uncheckedRead(int off, byte[] dst, int dstOff, int len) {
        __uncheckedTransfer(off, dst, dstOff, len, true);
    }

    @Override
    public void uncheckedWrite(int off, byte[] src, int srcOff, int len) {
        __uncheckedTransfer(off, src, srcOff, len, false);
    }

    @Override
    public byte uncheckedReadByte(int off) {
        Map.Entry<Integer, Block> block = uncheckedGetChildBlock(off);
        int offsetWithinBlock = off - block.getKey();

        return block.getValue().uncheckedReadByte(offsetWithinBlock);
    }

    @Override
    public void uncheckedWriteByte(int off, byte val) {
        Map.Entry<Integer, Block> block = uncheckedGetChildBlock(off);
        int offsetWithinBlock = off - block.getKey();

        block.getValue().uncheckedWriteByte(offsetWithinBlock, val);
    }

    private Map.Entry<Integer, Block> uncheckedGetChildBlock(int off) {
        return nested.floorEntry(off);
    }

    private void __uncheckedTransfer(int off, byte[] other, int otherOff, int len, boolean read) {
        if (len == 0)
            return;

        Map.Entry<Integer, Block> start = uncheckedGetChildBlock(off);
        Map.Entry<Integer, Block> end = uncheckedGetChildBlock(off + len - 1);

        if (start.getValue() == end.getValue()) {
            // start and end are both in same child Block
            int offsetWithinBlock = off - start.getKey();
            start.getValue().uncheckedTransfer(offsetWithinBlock, other, otherOff, len, read);
        } else {
            // start and end are in distinct child Blocks
            NavigableMap<Integer, Block> tail = nested.subMap(start.getKey(), false, end.getKey(), true);
            {
                // Copy start block
                Block startBlock = start.getValue();
                int offsetWithinStart = off - start.getKey();
                int sizeWithinStart = startBlock.capacity() - offsetWithinStart;
                startBlock.uncheckedTransfer(offsetWithinStart, other, otherOff, sizeWithinStart, read);
                otherOff += sizeWithinStart;
                len -= sizeWithinStart;
            }
            for (Block b : tail.values()) {
                // Copy remaining blocks
                if (len > b.capacity()) {
                    b.uncheckedTransfer(0, other, otherOff, b.capacity(), read);
                    otherOff += b.capacity();
                    len -= b.capacity();
                } else {
                    b.uncheckedTransfer(0, other, otherOff, len, read);
                    otherOff += len;
                    len = -1;
                }
            }
        }
    }

}
