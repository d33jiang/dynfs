package dynfs.dynlm;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.stream.Collectors;

import com.google.common.collect.Streams;

import dynfs.template.Allocator;
import dynfs.template.ClosedAllocatorException;

public final class BlockWeakReferenceList extends BlockLike {

    //
    // Configuration: Allocator

    private final Allocator<LMFile, Block> allocator;

    //
    // State: Nested Blocks

    private final NavigableMap<Integer, WeakReference<Block>> nested;

    //
    // Construction

    BlockWeakReferenceList(Allocator<LMFile, Block> allocator, LMFile owner, Block... nested) throws IOException {
        super(nested.length * Block.BLOCK_SIZE, owner);

        this.allocator = allocator;
        this.nested = new TreeMap<>();

        int offset = 0;
        for (Block b : nested) {
            putBlock(offset, b);
            offset += b.capacity();
        }
    }

    //
    // Core Support: Conversion to String

    List<Integer> getBlockIndices() {
        return nested.values().stream().map(this::getBlock).map(Block::getIndex).collect(Collectors.toList());
    }

    @Override
    public String toString() {
        return String.format("[BlockWeakReferenceList: %s]", getBlockIndices());
    }

    //
    // Support: Put Block as WeakReference<Block>

    private void putBlock(Integer index, Block block) {
        nested.put(index, new WeakReference<>(block));
    }

    //
    // Support: Get Block from WeakReference<Block>

    private Block getBlock(WeakReference<Block> block) throws ClosedAllocatorException {
        try {
            return block.get();
        } finally {
            if (allocator.isClosed()) {
                throw new ClosedAllocatorException(allocator);
            }
        }
    }

    //
    // Static Support: Get List<Block> from Iterable<WeakReference<Block>>

    private List<Block> getBlocks(Iterable<WeakReference<Block>> blocks) throws ClosedAllocatorException {
        return Streams.stream(blocks).map(this::getBlock).collect(Collectors.toCollection(ArrayList::new));
    }

    //
    // Implementation: Capacity

    @Override
    protected int ensureCapacityImpl(int minCapacity) throws IOException {
        int numNewBlocks = Block.numBlocks(minCapacity - capacity());
        int offset = capacity();

        Iterable<Block> allocatedBlocks = allocator.allocate(getOwner(), numNewBlocks);
        for (Block b : allocatedBlocks) {
            putBlock(offset, b);
            offset += b.capacity();
        }

        return offset;
    }

    @Override
    protected int trimCapacityImpl(int minCapacity) throws IOException {
        Collection<WeakReference<Block>> toBeFreed = nested.tailMap(minCapacity).values();
        List<Block> freedBlocks = getBlocks(toBeFreed);

        toBeFreed.clear();
        allocator.free(getOwner(), freedBlocks);

        return calculateTrueCapacity();
    }

    private int calculateTrueCapacity() {
        Map.Entry<Integer, WeakReference<Block>> lastBlock = nested.lastEntry();
        return lastBlock.getKey() + getBlock(lastBlock.getValue()).capacity();
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
        Map.Entry<Integer, WeakReference<Block>> entry = nested.floorEntry(off);

        return new Map.Entry<Integer, Block>() {
            @Override
            public Integer getKey() {
                return entry.getKey();
            }

            @Override
            public Block getValue() {
                return getBlock(entry.getValue());
            }

            @Override
            public Block setValue(Block value) {
                Block oldValue = getValue();
                putBlock(getKey(), value);
                return oldValue;
            }
        };
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
            NavigableMap<Integer, WeakReference<Block>> tail = nested.subMap(start.getKey(), false, end.getKey(), true);
            {
                // Copy start block
                Block startBlock = start.getValue();
                int offsetWithinStart = off - start.getKey();
                int sizeWithinStart = startBlock.capacity() - offsetWithinStart;
                startBlock.uncheckedTransfer(offsetWithinStart, other, otherOff, sizeWithinStart, read);
                otherOff += sizeWithinStart;
                len -= sizeWithinStart;
            }
            for (Block b : getBlocks(tail.values())) {
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
