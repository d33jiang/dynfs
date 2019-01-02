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

public final class LMMemory implements Allocator<LMFile, Block> {

    //
    // Interface Field: Allocated Space

    private final IntConsumer setAllocatedSpace;

    //
    // Field: Internal Data

    private final Block[] blocks;

    private final Map<Integer, LMFile> reservedBlocks;
    private final Deque<Integer> freeBlocks;

    public LMMemory(IntConsumer setAllocatedSpace, int totalSpace) throws IOException {
        this.setAllocatedSpace = setAllocatedSpace;

        this.blocks = new Block[Block.numBlocks(totalSpace)];
        for (int i = 0; i < blocks.length; i++) {
            this.blocks[i] = new Block(i);
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
    public Iterable<Block> allocate(LMFile f, int nblocks) throws IOException {
        if (nblocks > freeBlocks.size())
            throw new FileSystemException(f.getRouteString(), null, "Out of memory");

        List<Block> allocated = new LinkedList<>();
        for (int i = 0; i < nblocks; i++) {
            Integer index = freeBlocks.removeFirst();

            Block block = blocks[index];
            allocated.add(block);

            block.setOwner(f);
            reservedBlocks.put(index, f);
        }

        updateUsedSpace();

        return allocated;
    }

    @Override
    public void free(LMFile f, Iterable<Block> blocks) {
        for (Block block : blocks) {
            if (reservedBlocks.get(block.getIndex()) != f)
                throw new IllegalArgumentException("Attempt to free wrongly associated block");
        }

        for (Block block : blocks) {
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
    public void close() throws IOException {
        for (int i = 0; i < blocks.length; i++) {
            blocks[i] = null;
        }
    }

    //
    // Debug: Core Dump

    public CoreDump getCoreDump() {
        return new CoreDump(blocks, reservedBlocks, freeBlocks);
    }

    public static final class CoreDump {
        private final Block[] blocks;
        private final Map<Integer, LMFile> reservedBlocks;
        private final Deque<Integer> freeBlocks;

        private String lastDump;

        private CoreDump(Block[] blocks, Map<Integer, LMFile> reservedBlocks, Deque<Integer> freeBlocks) {
            this.blocks = blocks;
            this.reservedBlocks = reservedBlocks;
            this.freeBlocks = freeBlocks;
            this.lastDump = null;
        }

        public CoreDump build() {
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
