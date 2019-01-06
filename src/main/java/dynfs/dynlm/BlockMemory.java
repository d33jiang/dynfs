package dynfs.dynlm;

import java.io.IOException;
import java.nio.file.FileSystemException;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.IntConsumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import dynfs.debug.Dumpable;
import dynfs.template.Allocator;

public final class BlockMemory<BlockOwner> implements Allocator<BlockOwner, Block<BlockOwner>> {

    //
    // Configuration: Callback, Set Allocated Space

    private final IntConsumer setAllocatedSpace;

    //
    // State: Blocks

    private Block<BlockOwner>[] blocks;

    private final Map<Integer, BlockOwner> reservedBlocks;
    private final Deque<Integer> freeBlocks;

    //
    // Construction

    @SuppressWarnings("unchecked")
    public BlockMemory(IntConsumer setAllocatedSpace, int totalSpace) throws IOException {
        this.setAllocatedSpace = setAllocatedSpace;

        this.blocks = new Block[Block.numBlocks(totalSpace)];
        for (int i = 0; i < blocks.length; i++) {
            this.blocks[i] = new Block<>(i);
        }

        this.reservedBlocks = new HashMap<>();
        this.freeBlocks = IntStream.range(0, blocks.length).boxed().collect(Collectors.toCollection(LinkedList::new));
    }

    //
    // Implementation: Memory Management

    private void updateUsedSpace() {
        setAllocatedSpace.accept(Block.sizeOfNBlocks(reservedBlocks.size()));
    }

    @Override
    public Iterable<Block<BlockOwner>> allocate(BlockOwner owner, int nblocks) throws IOException {
        if (nblocks > freeBlocks.size())
            throw new FileSystemException(owner.toString(), null, "Out of memory");

        List<Block<BlockOwner>> allocated = new LinkedList<>();
        for (int i = 0; i < nblocks; i++) {
            Integer index = freeBlocks.removeFirst();

            Block<BlockOwner> block = blocks[index];
            allocated.add(block);

            block.setOwner(owner);
            reservedBlocks.put(index, owner);
        }

        updateUsedSpace();

        return allocated;
    }

    @Override
    public void free(BlockOwner owner, Iterable<Block<BlockOwner>> blocks) {
        for (Block<BlockOwner> block : blocks) {
            if (reservedBlocks.get(block.getIndex()) != owner)
                throw new IllegalArgumentException("Attempt to free wrongly associated block");
        }

        for (Block<BlockOwner> block : blocks) {
            Integer index = block.getIndex();

            reservedBlocks.remove(index);
            block.setOwner(null);

            freeBlocks.add(index);
        }

        updateUsedSpace();
    }

    //
    // Implementation: Close

    @Override
    public void close() {
        blocks = null;
    }

    @Override
    public boolean isClosed() {
        return blocks == null;
    }

    //
    // Debug: Core Dump

    public CoreDump<BlockOwner> getCoreDump() {
        return new CoreDump<BlockOwner>(blocks, reservedBlocks, freeBlocks);
    }

    public static final class CoreDump<BlockOwner> {
        private final Block<BlockOwner>[] blocks;
        private final Map<Integer, BlockOwner> reservedBlocks;
        private final Deque<Integer> freeBlocks;

        private String lastDump;

        private CoreDump(Block<BlockOwner>[] blocks, Map<Integer, BlockOwner> reservedBlocks,
                Deque<Integer> freeBlocks) {
            this.blocks = blocks;
            this.reservedBlocks = reservedBlocks;
            this.freeBlocks = freeBlocks;
            this.lastDump = null;
        }

        public CoreDump<BlockOwner> build() {
            StringBuilder sb = new StringBuilder();

            sb.append("       # Blocks: " + blocks.length + "\n");
            sb.append("Reserved Blocks:\n");
            reservedBlocks.entrySet().stream().map(e -> e.getKey() + " -> " + e.getValue())
                    .collect(Collectors.joining(" | "));
            sb.append("    Free Blocks: " + freeBlocks + "\n");

            lastDump = sb.substring(0, sb.length() - 1);

            return this;
        }

        @Override
        public String toString() {
            return lastDump;
        }
    }

    //
    // Debug: Block Dump

    public Dumpable dumpBlock(int i) {
        return blocks[i];
    }

}
