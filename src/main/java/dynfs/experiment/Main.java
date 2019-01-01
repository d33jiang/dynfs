package dynfs.experiment;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.StandardOpenOption;
import java.nio.file.spi.FileSystemProvider;
import java.util.List;
import java.util.Optional;

import com.google.common.collect.ImmutableSet;

import dynfs.core.DynFileSystem;
import dynfs.core.DynFileSystemProvider;
import dynfs.core.path.DynPath;
import dynfs.core.store.DynSpaceFactory;
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

        DynSpaceFactory<LMSpace> fac = p -> new LMSpace(Block.sizeOfNBlocks(12));
        DynFileSystem<LMSpace> fs = dfsp.<LMSpace>newFileSystem("asdf", fac,
                null);
        LMSpace store = fs.getStore();
        System.out.println();

        dfsp.createDirectory(DynPath.newPath(fs, "/foo"));

        ByteBuffer buf = ByteBuffer.allocate(256);

        SeekableByteChannel chan = dfsp.newByteChannel(DynPath.newPath(fs, "/foo/bar"),
                ImmutableSet.of(StandardOpenOption.WRITE, StandardOpenOption.CREATE));

        buf.put("ABCDEFG".getBytes());
        chan.write(buf);

        chan.close();

        System.out.println("##");
        System.out.println(store.getRootDirectory().getTreeDump().build());
        System.out.println("##");
        System.out.println(store.getMemory().getCoreDump().build());
        System.out.println("##");
        System.out.println(store.getMemory().dumpBlock(0).dump().build());
        System.out.println("##");

    }

}
