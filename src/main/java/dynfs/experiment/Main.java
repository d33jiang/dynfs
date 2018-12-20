package dynfs.experiment;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.spi.FileSystemProvider;
import java.util.List;
import java.util.Optional;

import dynfs.core.DynFileSystem;
import dynfs.core.DynFileSystemProvider;
import dynfs.dynlm.Block;
import dynfs.dynlm.LMSpace;

public class Main {

	public static void main(String[] args) throws IOException {

		List<FileSystemProvider> fsps = FileSystemProvider.installedProviders();
		FileSystem fspDefault = FileSystems.getDefault();
		System.out.println(fsps);
		System.out.println(fspDefault);
		System.out.println();

		Optional<FileSystemProvider> dfspo = fsps.stream()
				.filter(fsp -> fsp instanceof DynFileSystemProvider)
				.findFirst();

		if (!dfspo.isPresent())
			System.exit(1);

		DynFileSystemProvider dfsp = (DynFileSystemProvider) dfspo.get();
		System.out.println(dfsp);
		System.out.println();

		DynFileSystem fs = dfsp.newFileSystem("asdf", p -> new LMSpace(Block.sizeOfNBlocks(12)), null);
		LMSpace store = (LMSpace) fs.getStore();
		System.out.println();

		System.out.println("##");
		System.out.println(store.getTreeDump().build());
		System.out.println("##");
		System.out.println(store.getCoreDump().build());
		System.out.println("##");
		System.out.println(store.dumpBlock(0).build());
		System.out.println("##");

	}

}
