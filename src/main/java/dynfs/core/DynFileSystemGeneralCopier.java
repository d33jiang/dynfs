package dynfs.core;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ByteChannel;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.StandardOpenOption;
import java.util.Map;

import com.google.common.collect.ImmutableList;

import dynfs.core.options.CopyOptions;
import dynfs.core.options.OpenOptions;

final class DynFileSystemGeneralCopier {

    //
    // Constant: Buffer Size

    private static final int BUFFER_SIZE = 4096;

    //
    // Construction: Disabled

    private DynFileSystemGeneralCopier() {}

    //
    // Interface Implementation: Copy

    public static <S1 extends DynSpace<S1>, S2 extends DynSpace<S2>> void copy(DynFileSystem<S1> fsSrc,
            DynFileSystem<S2> fsDst, DynRoute src, DynRoute dst,
            CopyOptions copyOptions) throws IOException {
        // FUTURE: Access Control - Check access control

        ResolutionResult<S1> srcResolution = fsSrc.resolve(src, !copyOptions.nofollowLinks, true);
        DynNode<S1, ?> srcNode = srcResolution.testExistence();

        ResolutionResult<S2> dstResolution = fsDst.resolve(dst);
        if (dstResolution.exists()) {
            if (copyOptions.replaceExisting) {
                if (dstResolution.node() instanceof DynDirectory) {
                    if (!((DynDirectory<S2, ?>) dstResolution.node()).isEmpty())
                        throw new DirectoryNotEmptyException(dst.toString());
                } else if (dstResolution.node() instanceof DynLink) {
                    throw new FileAlreadyExistsException(dst.toString(), null, "Target file is a symbolic link");
                }

                dstResolution.node().delete();
            } else {
                throw new FileAlreadyExistsException(dst.toString());
            }
        }

        if (srcNode.isDirectory()) {
            dstResolution.lastParent().createDirectoryImpl(dst.getFileName());
        } else {
            OpenOptions readOptions = OpenOptions.parse(ImmutableList.of(StandardOpenOption.READ));
            OpenOptions writeOptions = OpenOptions
                    .parse(ImmutableList.of(StandardOpenOption.WRITE, StandardOpenOption.CREATE_NEW));
            if (copyOptions.nofollowLinks) {
                readOptions.nofollowLinks = true;
            }

            try (ByteChannel in = DynFileSystemProviderIO.newByteChannel(fsSrc, src, readOptions);
                    ByteChannel out = DynFileSystemProviderIO.newByteChannel(fsDst, dst, writeOptions)) {
                ByteBuffer buf = ByteBuffer.allocate(BUFFER_SIZE);

                while (in.read(buf) != -1) {
                    buf.flip();
                    out.write(buf);
                    buf.flip();
                }
            }
        }

        DynNode<S2, ?> dstNode = fsDst.resolve(dst).testExistence();

        if (copyOptions.copyAttributes) {
            try {
                Map<DynNodeAttribute, Object> srcAttributes = srcNode.readAllAttributes();
                dstNode.writeAttributes(srcAttributes);
            } catch (IOException ex) {
                // Makes a best effort to copy the DynNode attributes.
                // If an exception is encountered, the DynFileSystem should be left in a
                // consistent state by DynNode.writeAttributes.
            }
        }
    }

    //
    // Interface Implementation: Move

    public static void move(DynFileSystem<?> fsSrc, DynFileSystem<?> fsDst, DynRoute src, DynRoute dst,
            CopyOptions copyOptions) throws IOException {
        if (copyOptions.atomicMove)
            throw new IllegalArgumentException("Atomic move is not supported by DynFileSystemGeneralCopier");

        copy(fsSrc, fsDst, src, dst, copyOptions);
        fsSrc.resolve(src).testExistence().delete();
    }

}
