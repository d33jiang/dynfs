package dynfs.dynlm;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.stream.Collectors;

import dynfs.template.Allocator;

public final class BlockList<Owner> extends BlockLike<Owner> {

    //
    // Configuration: Allocator

    private final Allocator<Owner, Block<Owner>> allocator;

    //
    // State: Nested Blocks

    private final NavigableMap<Integer, Block<Owner>> nested;

    //
    // Construction

    @SafeVarargs // nested is never written to
    BlockList(Allocator<Owner, Block<Owner>> allocator, Owner owner, Block<Owner>... nested) throws IOException {
        super(nested.length * Block.BLOCK_SIZE, owner);

        this.allocator = allocator;
        this.nested = new TreeMap<>();

        int offset = 0;
        for (Block<Owner> b : nested) {
            this.nested.put(offset, b);
            offset += b.capacity();
        }
    }

    //
    // Core Support: Conversion to String

    List<Integer> getBlockIndices() {
        return nested.values().stream().map(Block::getIndex).collect(Collectors.toList());
    }

    @Override
    public String toString() {
        return String.format("[BlockList: %s]", getBlockIndices());
    }

    //
    // Implementation: Capacity

    @Override
    protected int ensureCapacityImpl(int minCapacity) throws IOException {
        int numNewBlocks = Block.numBlocks(minCapacity - capacity());
        int offset = capacity();

        Iterable<Block<Owner>> allocatedBlocks = allocator.allocate(getOwner(), numNewBlocks);
        for (Block<Owner> b : allocatedBlocks) {
            nested.put(offset, b);
            offset += b.capacity();
        }

        return offset;
    }

    @Override
    protected int trimCapacityImpl(int minCapacity) throws IOException {
        Collection<Block<Owner>> toBeFreed = nested.tailMap(minCapacity).values();
        List<Block<Owner>> freedBlocks = new ArrayList<>(toBeFreed);

        toBeFreed.clear();
        allocator.free(getOwner(), freedBlocks);

        return calculateTrueCapacity();
    }

    private int calculateTrueCapacity() {
        Map.Entry<Integer, Block<Owner>> lastBlock = nested.lastEntry();
        return lastBlock == null ? 0 : lastBlock.getKey() + lastBlock.getValue().capacity();
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
        Map.Entry<Integer, Block<Owner>> block = uncheckedGetChildBlock(off);
        int offsetWithinBlock = off - block.getKey();

        return block.getValue().uncheckedReadByte(offsetWithinBlock);
    }

    @Override
    public void uncheckedWriteByte(int off, byte val) {
        Map.Entry<Integer, Block<Owner>> block = uncheckedGetChildBlock(off);
        int offsetWithinBlock = off - block.getKey();

        block.getValue().uncheckedWriteByte(offsetWithinBlock, val);
    }

    private Map.Entry<Integer, Block<Owner>> uncheckedGetChildBlock(int off) {
        return nested.floorEntry(off);
    }

    private void __uncheckedTransfer(int off, byte[] other, int otherOff, int len, boolean read) {
        if (len == 0)
            return;

        Map.Entry<Integer, Block<Owner>> start = uncheckedGetChildBlock(off);
        Map.Entry<Integer, Block<Owner>> end = uncheckedGetChildBlock(off + len - 1);

        if (start.getValue() == end.getValue()) {
            // start and end are both in same child Block
            int offsetWithinBlock = off - start.getKey();
            start.getValue().uncheckedTransfer(offsetWithinBlock, other, otherOff, len, read);
        } else {
            // start and end are in distinct child Blocks
            NavigableMap<Integer, Block<Owner>> tail = nested.subMap(start.getKey(), false, end.getKey(), true);
            {
                // Copy start block
                Block<Owner> startBlock = start.getValue();
                int offsetWithinStart = off - start.getKey();
                int sizeWithinStart = startBlock.capacity() - offsetWithinStart;
                startBlock.uncheckedTransfer(offsetWithinStart, other, otherOff, sizeWithinStart, read);
                otherOff += sizeWithinStart;
                len -= sizeWithinStart;
            }
            for (Block<Owner> b : tail.values()) {
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
