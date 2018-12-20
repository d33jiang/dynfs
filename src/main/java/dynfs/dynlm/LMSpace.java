package dynfs.dynlm;

import java.io.IOException;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.AccessMode;
import java.nio.file.CopyOption;
import java.nio.file.DirectoryStream;
import java.nio.file.DirectoryStream.Filter;
import java.nio.file.FileSystemException;
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.FileAttributeView;
import java.nio.file.attribute.FileStoreAttributeView;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import dynfs.core.DynPath;
import dynfs.core.DynPath.DynPathBody;
import dynfs.core.DynSpace;
import dynfs.core.store.DynFileIO;
import dynfs.debug.Dumpable.DumpBuilder;

public class LMSpace extends DynSpace {

	//
	// Constants: Properties

	private static final String DS_NAME = "";
	private static final String DS_TYPE = "local.memory.rw";
	private static final boolean DS_IS_RO = false;

	//
	// Memory Management

	private final Block[] blocks;
	private final Map<Integer, LMNode> reservedBlocks;
	private final List<Integer> freeBlocks;

	//
	// Directory Structure

	private LMNode root;

	//
	// Construction

	public LMSpace(int totalSpace) {
		super(totalSpace);

		this.blocks = new Block[Block.numBlocks(totalSpace)];
		for (int i = 0; i < blocks.length; i++) {
			this.blocks[i] = new Block(i);
		}

		this.reservedBlocks = new HashMap<>();
		this.freeBlocks = IntStream.range(0, blocks.length).boxed().collect(Collectors.toCollection(LinkedList::new));

		this.root = LMDirectory.createRootDirectory(this);
	}

	//
	// Identity

	@Override
	public String name() {
		return DS_NAME;
	}

	@Override
	public String type() {
		return DS_TYPE;
	}

	@Override
	public boolean isReadOnly() {
		return DS_IS_RO;
	}

	//
	// File Attributes

	private static final Set<String> SUPPORTED_FILE_ATTRIBUTE_VIEWS;
	static {
		Set<String> supportedViews = new HashSet<>();
		supportedViews.add("basic");
		SUPPORTED_FILE_ATTRIBUTE_VIEWS = Collections.unmodifiableSet(supportedViews);
	}

	@Override
	public Set<String> supportedFileAttributeViews() {
		return SUPPORTED_FILE_ATTRIBUTE_VIEWS;
	}

	@Override
	public boolean supportsFileAttributeView(Class<? extends FileAttributeView> type) {
		// NOTE: Hack
		return type.isAssignableFrom(BasicFileAttributeView.class);
	}

	@Override
	public boolean supportsFileAttributeView(String name) {
		// NOTE: Hack
		return "basic".equals(name);
	}

	//
	// File Store Attributes

	@Override
	public <V extends FileStoreAttributeView> V getFileStoreAttributeView(Class<V> type) {
		// NOTE: Hack
		throw new UnsupportedOperationException();
	}

	@Override
	public Object getAttribute(String attribute) throws IOException {
		// NOTE: Hack
		throw new UnsupportedOperationException();
	}

	//
	// Memory Management

	private void updateUsedSpace() {
		_setUsedSpace(Block.sizeOfNBlocks(reservedBlocks.size()));
	}

	List<Block> allocateBlocks(LMFile f, int nblocks) throws IOException {
		if (nblocks > freeBlocks.size()) {
			throw new FileSystemException(f.getPath(), null, "Out of memory");
		}

		List<Block> allocated = new LinkedList<>();
		for (int i = 0; i < nblocks; i++) {
			int index = freeBlocks.remove(0);

			Block block = blocks[index];
			allocated.add(block);

			block.setOwner(f);
			reservedBlocks.put(index, f);
		}
		updateUsedSpace();

		return allocated;
	}

	void freeBlocks(LMFile f, List<Block> blocks) {
		for (Block b : blocks) {
			if (reservedBlocks.get(b.getIndex()) != f)
				throw new IllegalArgumentException("Attempt to free wrongly associated block");
		}

		for (Block b : blocks) {
			reservedBlocks.remove(b.getIndex());
			b.setOwner(null);

			freeBlocks.add(b.getIndex());
		}
		updateUsedSpace();
	}

	//
	// Close

	@Override
	public void close() throws IOException {
		for (int i = 0; i < blocks.length; i++) {
			blocks[i] = null;
		}
		root = null;
	}

	//
	// I/O

	private final FileIO io = new FileIO();

	@Override
	protected DynFileIO getIOInterface() {
		return io;
	}

	private static class PathResolution {
		private final LMNode n;
		private final int i;

		private PathResolution(LMNode n, int i) {
			this.n = n;
			this.i = i;
		}
	}

	private PathResolution resolvePathBody(DynPathBody path) {
		return resolvePathBody(path, path.pathSize());
	}

	private PathResolution resolvePathBody(DynPathBody path, int limit) {
		limit = Math.min(path.pathSize(), limit);

		Iterator<String> iter = path.path().iterator();
		LMNode n = root;

		while (limit > 0) {
			LMNode m = n.resolve(iter.next());
			if (m == null) {
				return new PathResolution(n, path.pathSize() - limit);
			}
			n = m;
			limit--;
		}
		return new PathResolution(n, 0);
	}

	public class FileIO implements DynFileIO {

		@Override
		public SeekableByteChannel newByteChannel(DynPathBody path, Set<? extends OpenOption> options,
				FileAttribute<?>... attrs) throws IOException {
			PathResolution res = resolvePathBody(path);

			// TODO Implementation: Method

			// Create parent directories?
			return null;
		}

		@Override
		public DirectoryStream<Path> newDirectoryStream(DynPathBody dir, Filter<? super Path> filter)
				throws IOException {
			PathResolution res = resolvePathBody(dir);

			// TODO Implementation: Method
			return null;
		}

		@Override
		public void createDirectory(DynPathBody dir, FileAttribute<?>... attrs) throws IOException {
			PathResolution res = resolvePathBody(dir);

			// TODO Implementation: Method
		}

		@Override
		public void delete(DynPathBody path) throws IOException {
			PathResolution res = resolvePathBody(path);

			// TODO Implementation: Method
		}

		@Override
		public void copy(Path source, DynPathBody target, boolean deleteSource, CopyOption... options)
				throws IOException {
			if (source instanceof DynPath) {
				copy(((DynPath) source).body(), target, deleteSource, options);
			} else {
				// TODO Implementation: Method

			}
		}

		private void copy(DynPathBody source, DynPathBody target, boolean deleteSource, CopyOption[] options)
				throws IOException {
			PathResolution resSrc = resolvePathBody(source);
			PathResolution resDst = resolvePathBody(target);

			// TODO Implementation: Method
		}

		@Override
		public boolean isSameFile(DynPathBody path1, DynPathBody path2) throws IOException {
			PathResolution res1 = resolvePathBody(path1);
			PathResolution res2 = resolvePathBody(path2);

			// TODO Implementation: Method
			return false;
		}

		@Override
		public boolean isHidden(DynPathBody path) throws IOException {
			PathResolution res = resolvePathBody(path);

			// TODO Implementation: Method
			return false;
		}

		@Override
		public void checkAccess(DynPathBody path, AccessMode... modes) throws IOException {
			PathResolution res = resolvePathBody(path);

			// TODO Implementation: Method
		}

		@Override
		public <V extends FileAttributeView> V getFileAttributeView(DynPathBody path, Class<V> type,
				LinkOption... options) {
			PathResolution res = resolvePathBody(path);

			// TODO Implementation: Method
			return null;
		}

		@Override
		public <A extends BasicFileAttributes> A readAttributes(DynPathBody path, Class<A> type, LinkOption... options)
				throws IOException {
			PathResolution res = resolvePathBody(path);

			// TODO Implementation: Method
			return null;
		}

		@Override
		public Map<String, Object> readAttributes(DynPathBody path, String attributes, LinkOption... options)
				throws IOException {
			PathResolution res = resolvePathBody(path);

			// TODO Implementation: Method
			return null;
		}

		@Override
		public void setAttribute(DynPathBody path, String attribute, Object value, LinkOption... options)
				throws IOException {
			PathResolution res = resolvePathBody(path);

			// TODO Implementation: Method

		}

	}

	//
	// Debug: Core Dump

	public CoreDump getCoreDump() {
		return new CoreDump(blocks, reservedBlocks, freeBlocks);
	}

	public static final class CoreDump {
		private final Block[] blocks;
		private final Map<Integer, LMNode> reservedBlocks;
		private final List<Integer> freeBlocks;

		private String lastDump;

		private CoreDump(Block[] blocks, Map<Integer, LMNode> reservedBlocks, List<Integer> freeBlocks) {
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

			sb.deleteCharAt(sb.length() - 1);
			lastDump = sb.toString();

			return this;
		}

		@Override
		public String toString() {
			return lastDump;
		}
	}

	//
	// Debug: Block Dump

	public DumpBuilder dumpBlock(int i) {
		return blocks[i].dump();
	}

	//
	// Debug: Tree Dump

	public TreeDump getTreeDump() {
		return new TreeDump(root);
	}

	public static final class TreeDump {
		private final LMNode root;
		private String lastDump;

		private TreeDump(LMNode root) {
			this.root = root;
			this.lastDump = null;
		}

		public TreeDump build() {
			StringBuilder sb = new StringBuilder();

			dump(sb, 0, root);

			sb.deleteCharAt(sb.length() - 1);
			lastDump = sb.toString();

			return this;
		}

		private void dump(StringBuilder sb, int newDepth, LMNode newRoot) {
			sb.append(newRoot.getPath());
			sb.append('\n');
			for (LMNode child : newRoot) {
				dump(sb, newDepth + 1, child);
			}
		}

		@Override
		public String toString() {
			return lastDump;
		}
	}

}
