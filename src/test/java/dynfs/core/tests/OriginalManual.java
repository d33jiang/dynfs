package dynfs.core.tests;

import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.StandardOpenOption;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import com.google.common.collect.ImmutableSet;

import dynfs.core.DynPath;
import dynfs.core.base.SystemBase;
import dynfs.dynlm.BlockMemory;
import dynfs.dynlm.LMDirectory;
import dynfs.dynlm.LMFile;

public class OriginalManual extends SystemBase {

    @Test
    @Disabled
    public void testOriginalExperiment() throws Exception {

        ByteBuffer buf = ByteBuffer.allocate(256);

        provider().createDirectory(DynPath.newPath(fs(), "/foo"));

        //

        SeekableByteChannel chan = provider().newByteChannel(DynPath.newPath(fs(), "/foo/bar"),
                ImmutableSet.of(StandardOpenOption.WRITE, StandardOpenOption.CREATE_NEW));

        buf.put("ABCDEFG".getBytes());
        buf.flip();

        System.out.println(chan.write(buf));
        buf.flip();

        chan.close();

        //
        // /*

        chan = provider().newByteChannel(DynPath.newPath(fs(), "/foo/bar"),
                ImmutableSet.of(StandardOpenOption.WRITE, StandardOpenOption.APPEND));

        buf.put("HIJK".getBytes());
        buf.flip();

        System.out.println(chan.write(buf));
        buf.flip();

        chan.close();

        // */

        @SuppressWarnings("unchecked")
        BlockMemory<LMFile> memory = (BlockMemory<LMFile>) invoke(store(), "getMemory");

        System.out.println("##");
        System.out.println(store().<LMDirectory>getRootDirectory().getTreeDump().build());
        System.out.println("##");
        System.out.println(memory.getCoreDump().build());
        System.out.println("##");
        System.out.println(memory.dumpBlock(0).dump().build());
        System.out.println("##");
        System.out.println(memory.dumpBlock(1).dump().build());
        System.out.println("##");

    }

}
