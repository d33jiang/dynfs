package dynfs.dynlm;

import java.io.IOException;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.FileAttributeView;
import java.nio.file.attribute.FileStoreAttributeView;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import dynfs.core.DynSpace;

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

}
