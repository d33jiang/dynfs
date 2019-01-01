package dynfs.dynlm;

import java.io.IOException;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.FileAttributeView;
import java.nio.file.attribute.FileStoreAttributeView;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.NotImplementedException;

import dynfs.core.DynDirectory;
import dynfs.core.DynFile;
import dynfs.core.DynNode;
import dynfs.core.DynSpace;
import dynfs.core.options.LinkOptions;
import dynfs.core.path.DynRoute;
import dynfs.core.store.DynSpaceIO;

public class LMSpace extends DynSpace<LMSpace> {

    public static int getIntValue(long v) {
        if (v > Integer.MAX_VALUE || v < Integer.MIN_VALUE)
            throw new UnsupportedOperationException("Long values are not supported");

        return (int) v;
    }

    //
    // Implementation: DynSpace Properties

    private static final String DS_NAME = "";
    private static final String DS_TYPE = "local.memory.rw";
    private static final boolean DS_IS_RO = false;

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
    // Implementation: DynSpace Attributes

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
    // Implementation: Supported File Attribute Views

    private static final Set<Class<? extends FileAttributeView>> SUPPORTED_FILE_ATTRIBUTE_VIEWS;
    static {
        Set<Class<? extends FileAttributeView>> supportedViews = new HashSet<>();
        supportedViews.add(BasicFileAttributeView.class);
        SUPPORTED_FILE_ATTRIBUTE_VIEWS = Collections.unmodifiableSet(supportedViews);
    }

    private static final Set<String> SUPPORTED_FILE_ATTRIBUTE_VIEWS_BY_NAME;
    static {
        Set<String> supportedViews = new HashSet<>();
        supportedViews.add("basic");
        SUPPORTED_FILE_ATTRIBUTE_VIEWS_BY_NAME = Collections.unmodifiableSet(supportedViews);
    }

    @Override
    public Set<Class<? extends FileAttributeView>> supportedFileAttributeViews() {
        return SUPPORTED_FILE_ATTRIBUTE_VIEWS;
    }

    @Override
    public Set<String> supportedFileAttributeViewsByName() {
        return SUPPORTED_FILE_ATTRIBUTE_VIEWS_BY_NAME;
    }

    //
    // Field: Memory Management

    private final LMMemory memory;

    //
    // Interface: Memory Management

    // TODO: package-private; public for testing
    public LMMemory getMemory() {
        return memory;
    }

    //
    // Field: Directory Structure

    private LMDirectory root;

    //
    // Implementation: Root Directory

    // Javac fails to determine that LMDirectory satisfies DirNode
    @SuppressWarnings("unchecked")
    @Override
    public LMDirectory getRootDirectory() {
        return root;
    }

    //
    // Construction

    public LMSpace(int totalSpace) throws IOException {
        super(totalSpace);

        this.memory = new LMMemory(this::setAllocatedSpace, totalSpace);
        this.root = new LMDirectory(this);
    }

    //
    // Implementation: Close

    @Override
    public void closeImpl() throws IOException {
        memory.close();
        root = null;
    }

    //
    // Implementation: I/O Interface

    private final LMSpaceIO io = new LMSpaceIO();

    @Override
    protected DynSpaceIO<LMSpace> getIOInterface() {
        return io;
    }

    public class LMSpaceIO implements DynSpaceIO<LMSpace> {

        // TODO: Forego DynSpaceIO interface; put abstract methods in DynFile,
        // DynDirectory

        @Override
        public DynFile<LMSpace, ?> createFile(DynDirectory<LMSpace, ?> parent, String name, FileAttribute<?>... attrs)
                throws IOException {
            LMFile file = new LMFile(LMSpace.this, (LMDirectory) parent, name);
            ((LMDirectory) parent).children().put(name, file);
            return file;
        }

        @Override
        public DynDirectory<LMSpace, ?> createDirectory(DynDirectory<LMSpace, ?> parent, String name,
                FileAttribute<?>... attrs) throws IOException {
            LMDirectory file = new LMDirectory(LMSpace.this, (LMDirectory) parent, name);
            ((LMDirectory) parent).children().put(name, file);
            return file;
        }

        @Override
        public void delete(DynNode<LMSpace, ?> node) throws IOException {
            if (node instanceof LMFile) {
                LMFile file = (LMFile) node;
                file.setSize(0);
            } else if (node instanceof LMDirectory) {
                LMDirectory dir = (LMDirectory) node;
                for (DynNode<LMSpace, ?> n : dir) {
                    n.delete();
                }
            }
        }

        @Override
        public void copy(DynNode<LMSpace, ?> src, DynNode<LMSpace, ?> dstParent, String dstName, boolean deleteSrc)
                throws IOException {
            // TODO: Auto-generated method stub
            throw new NotImplementedException("Method stub");
        }

        @Override
        public <V extends FileAttributeView> V getFileAttributeView(DynRoute route, Class<V> type,
                LinkOptions options) {
            // TODO: Auto-generated method stub
            throw new NotImplementedException("Method stub");
        }

        @Override
        public <A extends BasicFileAttributes> A readAttributes(DynRoute route, Class<A> type, LinkOptions options)
                throws IOException {
            // TODO: Auto-generated method stub
            throw new NotImplementedException("Method stub");
        }

        @Override
        public Map<String, Object> readAttributes(DynRoute route, String attributes, LinkOptions options)
                throws IOException {
            // TODO: Auto-generated method stub
            throw new NotImplementedException("Method stub");
        }

        @Override
        public void setAttribute(DynRoute route, String attribute, Object value, LinkOptions options)
                throws IOException {
            // TODO: Auto-generated method stub
            throw new NotImplementedException("Method stub");
        }
        // TODO: LMSpaceIO Implementation
    }

}
