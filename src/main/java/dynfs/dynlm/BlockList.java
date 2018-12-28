package dynfs.dynlm;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

public class BlockList extends BlockLike {

	private static int __calculateConstructorArgumentSize(Block[] args) {
		return Arrays.stream(args).mapToInt(Block::size).sum();
	}

	private final LMSpace store;
	private final LMFile file;
	private final NavigableMap<Integer, Block> nested;

	public BlockList(LMSpace store, LMFile file, Block... nested) throws IOException {
		super(__calculateConstructorArgumentSize(nested));
		this.store = store;
		this.file = file;
		this.nested = new TreeMap<>();
		int index = 0;
		for (Block b : nested) {
			this.nested.put(index, b);
			index += b.size();
		}
	}

	@Override
	protected int setSize(int newSize) throws IOException {
		int newNumBlocks = Block.numBlocks(newSize);
		if (newNumBlocks > nested.size()) {
			Map.Entry<Integer, Block> lastBlock = nested.lastEntry();
			int offset = lastBlock.getKey() + lastBlock.getValue().size();
			
			List<Block> allocatedBlocks = store.allocateBlocks(file, newNumBlocks - nested.size());
			for (Block b : allocatedBlocks) {
				nested.put(offset, b);
				offset += b.size();
			}
		} else if (newNumBlocks < nested.size()) {
			Collection<Block> toBeFreed = nested.tailMap(newSize).values();
			List<Block> freedBlocks = new ArrayList<>(toBeFreed);
			toBeFreed.clear();
			store.freeBlocks(file, freedBlocks);
		}
		return super.setSize(newSize);
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
			// start and end are in distinct children Block
			NavigableMap<Integer, Block> tail = nested.subMap(start.getKey(), false, end.getKey(), true);
			{
				// Copy start block
				Block startBlock = start.getValue();
				int offsetWithinStart = off - start.getKey();
				int sizeWithinStart = startBlock.size() - offsetWithinStart;
				startBlock.uncheckedTransfer(offsetWithinStart, other, otherOff, sizeWithinStart, read);
				otherOff += sizeWithinStart;
				len -= sizeWithinStart;
			}
			for (Block b : tail.values()) {
				// Copy remaining blocks
				if (len > b.size()) {
					b.uncheckedTransfer(0, other, otherOff, b.size(), read);
					otherOff += b.size();
					len -= b.size();
				} else {
					b.uncheckedTransfer(0, other, otherOff, len, read);
					otherOff += len;
					len = -1;
				}
			}
		}
	}

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

}
