package dynfs.core;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ByteChannel;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.BasicFileAttributes;

import com.google.common.collect.ImmutableList;

import dynfs.core.io.DynFileSystemIO;
import dynfs.core.options.CopyOptions;
import dynfs.core.options.OpenOptions;
import dynfs.core.path.DynRoute;

// TODO: public for testing (change to package private)
public final class DynFileSystemGeneralCopier {

    //
    // Constant: Buffer Size

    private static final int BUFFER_SIZE = 4096;

    //
    // Construction: Disabled

    private DynFileSystemGeneralCopier() {}

    //
    // Interface Implementation: Copy

    // TODO: Adhere to API specification of Files.copy (re: copy to link, etc.)
    public static <S1 extends DynSpace<S1>, S2 extends DynSpace<S2>> void copy(DynFileSystem<S1> fsSrc,
            DynFileSystem<S2> fsDst, DynRoute src, DynRoute dst,
            CopyOptions copyOptions) throws IOException {
        // TODO: Check access control

        BasicFileAttributes srcAttributes = DynFileSystemIO.readAttributes(fsSrc, src, BasicFileAttributes.class,
                copyOptions.getLinkOptions());

        ResolutionResult<S2> dstResolution = fsDst.resolve(dst);
        if (dstResolution.exists()) {
            if (copyOptions.replaceExisting) {
                if ((dstResolution.node() instanceof DynDirectory)
                        && !((DynDirectory<S2, ?>) dstResolution.node()).isEmpty()) {
                    throw new DirectoryNotEmptyException(dst.toString());
                }
                dstResolution.node().delete();
            } else {
                throw new FileAlreadyExistsException(dst.toString());
            }
        }

        if (srcAttributes.isDirectory()) {
            dstResolution.lastParent().createDirectoryImpl(dst.getFileName());
        } else {
            OpenOptions readOptions = OpenOptions.parse(ImmutableList.of(StandardOpenOption.READ));
            OpenOptions writeOptions = OpenOptions
                    .parse(ImmutableList.of(StandardOpenOption.WRITE, StandardOpenOption.CREATE_NEW));
            if (copyOptions.nofollowLinks) {
                readOptions.nofollowLinks = true;
            }

            try (ByteChannel in = fsSrc.newByteChannel(src, readOptions);
                    ByteChannel out = fsDst.newByteChannel(dst, writeOptions)) {
                ByteBuffer buf = ByteBuffer.allocate(BUFFER_SIZE);

                while (in.read(buf) != -1) {
                    out.write(buf);
                }
            }
        }

        if (copyOptions.copyAttributes) {
            try {
                BasicFileAttributeView dstAttributes = DynFileSystemIO.getFileAttributeView(fsDst, dst,
                        BasicFileAttributeView.class,
                        copyOptions.getLinkOptions());
                dstAttributes.setTimes(srcAttributes.lastModifiedTime(), srcAttributes.lastAccessTime(),
                        srcAttributes.creationTime());
            } catch (IOException ex) {
                try {
                    fsDst.resolve(dst).testExistence().delete();
                } catch (IOException ex0) {
                    ex.addSuppressed(ex0);
                }

                throw ex;
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
