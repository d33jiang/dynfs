package dynfs.core;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ByteChannel;
import java.nio.file.CopyOption;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Set;

import com.google.common.collect.Sets;

import dynfs.core.path.DynRoute;

final class DynFileSystemGeneralCopier {

    private DynFileSystemGeneralCopier() {}

    //
    // Constant: Buffer Size

    private static final int BUFFER_SIZE = 4096;

    //
    // Implementation: Copy

    static void copy(DynFileSystem<?> fsSrc, DynFileSystem<?> fsDst, DynRoute src, DynRoute dst,
            CopyOption... options) throws IOException {
        CopyOptions copyOptions = CopyOptions.parse(options);

        if (copyOptions.atomicMove) {
            throw new IllegalArgumentException("Atomic move is not supported by DynFileSystemGeneralCopier");
        }

        BasicFileAttributes srcAttributes = fsSrc.readAttributes(src, BasicFileAttributes.class,
                copyOptions.getLinkOptions());

        if (fsDst.exists(dst, copyOptions.nofollowLinks)) {
            if (copyOptions.replaceExisting) {
                fsDst.delete(dst);
            } else {
                throw new FileAlreadyExistsException(dst.toString());
            }
        }

        if (srcAttributes.isDirectory()) {
            fsDst.createDirectory(dst);
        } else {
            Set<OpenOption> readOptions = Sets.newHashSet(StandardOpenOption.READ);
            Set<OpenOption> writeOptions = Sets.newHashSet(StandardOpenOption.WRITE, StandardOpenOption.CREATE_NEW);
            if (copyOptions.nofollowLinks) {
                readOptions.add(LinkOption.NOFOLLOW_LINKS);
                writeOptions.add(LinkOption.NOFOLLOW_LINKS);
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
                BasicFileAttributeView dstAttributes = fsDst.getFileAttributeView(dst, BasicFileAttributeView.class,
                        copyOptions.getLinkOptions());
                dstAttributes.setTimes(srcAttributes.lastModifiedTime(), srcAttributes.lastAccessTime(),
                        srcAttributes.creationTime());
            } catch (IOException ex) {
                try {
                    fsDst.delete(dst);
                } catch (IOException ex0) {
                    ex.addSuppressed(ex0);
                }

                throw ex;
            }
        }
    }

    //
    // Implementation: Move

    static void move(DynFileSystem<?> fsSrc, DynFileSystem<?> fsDst, DynRoute src, DynRoute dst,
            CopyOption... options) throws IOException {
        copy(fsSrc, fsDst, src, dst, options);
        fsSrc.delete(src);
    }

    //
    // Helper Structure: Copy Options

    private static class CopyOptions {
        private boolean atomicMove;
        private boolean copyAttributes;
        private boolean replaceExisting;
        private boolean nofollowLinks;

        private CopyOptions() {
            atomicMove = false;
            copyAttributes = false;
            replaceExisting = false;
            nofollowLinks = false;
        }

        private static CopyOptions parse(CopyOption[] options) {
            CopyOptions result = new CopyOptions();

            for (CopyOption option : options) {
                if (option == StandardCopyOption.ATOMIC_MOVE) {
                    result.atomicMove = true;
                } else if (option == StandardCopyOption.COPY_ATTRIBUTES) {
                    result.copyAttributes = true;
                } else if (option == StandardCopyOption.REPLACE_EXISTING) {
                    result.replaceExisting = true;
                } else if (option == LinkOption.NOFOLLOW_LINKS) {
                    result.nofollowLinks = true;
                } else if (option == null) {
                    throw new NullPointerException("null CopyOption encountered");
                } else {
                    throw new IllegalArgumentException("CopyOption " + option + " is unavailable");
                }
            }

            return result;
        }

        private LinkOption[] getLinkOptions() {
            if (nofollowLinks) {
                return new LinkOption[] {
                    LinkOption.NOFOLLOW_LINKS
                };
            } else {
                return new LinkOption[] {};
            }
        }
    }

}
