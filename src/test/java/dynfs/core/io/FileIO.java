package dynfs.core.io;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.OpenOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.FileAttribute;
import java.util.Arrays;
import java.util.Set;

import org.junit.jupiter.api.Assertions;

import com.google.common.collect.ImmutableSet;

import dynfs.core.DynFileSystem;
import dynfs.core.DynPath;
import dynfs.core.DynRoute;
import dynfs.core.util.ProviderUtil;
import dynfs.dynlm.LMSpace;

public final class FileIO {

    //
    // Construction

    private FileIO() {}

    //
    // I/O

    public static byte[] readFileData(
            DynFileSystem<LMSpace> fs,
            DynRoute route,
            long off,
            int len) {
        byte[] result = null;

        try (SeekableByteChannel in = tryByteChannelCreation(fs, route,
                ImmutableSet.of(StandardOpenOption.READ))) {
            in.position(off);

            byte[] data = new byte[len];
            ByteBuffer buf = ByteBuffer.wrap(data);

            while (buf.hasRemaining() && in.read(buf) != -1) {}

            if (buf.remaining() > 0) {
                result = Arrays.copyOf(data, buf.position());
            } else {
                result = data;
            }
        } catch (IOException ex) {
            Assertions.fail("I/O Exception during read", ex);
        }

        return result;
    }

    public static void writeFileContent(
            DynFileSystem<LMSpace> fs,
            DynRoute route,
            long off,
            byte[] src) {
        try (SeekableByteChannel in = tryByteChannelCreation(fs, route,
                ImmutableSet.of(StandardOpenOption.WRITE, StandardOpenOption.CREATE))) {
            in.position(off);
            ByteBuffer srcBuf = ByteBuffer.wrap(src);
            Assertions.assertEquals(src.length, in.write(srcBuf));
        } catch (IOException ex) {
            Assertions.fail("I/O Exception during write", ex);
        }
    }

    private static SeekableByteChannel tryByteChannelCreation(
            DynFileSystem<LMSpace> fs,
            DynRoute route,
            Set<? extends OpenOption> openOptions,
            FileAttribute<?>... attrs) throws IOException {
        DynPath p0 = DynPath.newPath(fs, route);
        return ProviderUtil.provider().newByteChannel(p0, openOptions, attrs);
    }

}
