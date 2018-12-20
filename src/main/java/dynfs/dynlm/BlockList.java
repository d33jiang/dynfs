package dynfs.dynlm;

import java.util.Arrays;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

public class BlockList extends BlockLike {

	private static <BL extends BlockLike> int __calculateConstructorArgumentSize(BL[] args) {
		return Arrays.stream(args).mapToInt(BlockLike::size).sum();
	}

	private final NavigableMap<Integer, BlockLike> nested;

	public BlockList(Block... nested) {
		super(__calculateConstructorArgumentSize(nested));
		this.nested = new TreeMap<>();
		int index = 0;
		for (BlockLike b : nested) {
			this.nested.put(index, b);
			index += b.size();
		}
	}

	private Map.Entry<Integer, BlockLike> uncheckedGetChildBlock(int off) {
		return nested.floorEntry(off);
	}

	private void __uncheckedTransfer(int off, byte[] other, int otherOff, int len, boolean read) {
		if (len == 0)
			return;

		Map.Entry<Integer, BlockLike> start = uncheckedGetChildBlock(off);
		Map.Entry<Integer, BlockLike> end = uncheckedGetChildBlock(off + len - 1);

		if (start.getValue() == end.getValue()) {
			// start and end are both in same child BlockLike
			int offsetWithinBlock = off - start.getKey();
			start.getValue().uncheckedTransfer(offsetWithinBlock, other, otherOff, len, read);
		} else {
			// start and end are in distinct children BlockLike
			NavigableMap<Integer, BlockLike> tail = nested.subMap(start.getKey(), false, end.getKey(), true);
			{
				// Copy start block
				BlockLike startBlock = start.getValue();
				int offsetWithinStart = off - start.getKey();
				int sizeWithinStart = startBlock.size() - offsetWithinStart;
				startBlock.uncheckedTransfer(offsetWithinStart, other, otherOff, sizeWithinStart, read);
				otherOff += sizeWithinStart;
				len -= sizeWithinStart;
			}
			for (BlockLike b : tail.values()) {
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
		Map.Entry<Integer, BlockLike> block = uncheckedGetChildBlock(off);
		int offsetWithinBlock = off - block.getKey();

		return block.getValue().uncheckedReadByte(offsetWithinBlock);
	}

	@Override
	public void uncheckedWriteByte(int off, byte val) {
		Map.Entry<Integer, BlockLike> block = uncheckedGetChildBlock(off);
		int offsetWithinBlock = off - block.getKey();

		block.getValue().uncheckedWriteByte(offsetWithinBlock, val);
	}

}
