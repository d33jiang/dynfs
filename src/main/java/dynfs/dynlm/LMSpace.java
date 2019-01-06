package dynfs.dynlm;

import java.io.IOException;

import dynfs.core.DynSpace;
import dynfs.core.DynSpaceType;
import dynfs.core.DynSpaceType.Locality;
import dynfs.core.DynSpaceType.Storage;

public final class LMSpace extends DynSpace<LMSpace> {

    //
    // Constant: DynSpace Type

    private static final DynSpaceType DS_TYPE = new DynSpaceType(Locality.LOCAL, Storage.MEMORY);

    @Override
    public DynSpaceType getType() {
        return DS_TYPE;
    }

    //
    // Configuration: DynSpace Name

    private final String name;

    @Override
    public String name() {
        return name;
    }

    //
    // Configuration: DynSpace Read-Only Property

    private final boolean isReadOnly = false;

    @Override
    public boolean isReadOnly() {
        return isReadOnly;
    }

    //
    // State: Memory

    private final BlockMemory<LMFile> memory;

    BlockMemory<LMFile> getMemory() {
        return memory;
    }

    //
    // State: Directory Structure

    private LMDirectory root;

    // Javac fails to determine that LMDirectory satisfies requirements of DirNode
    @SuppressWarnings("unchecked")
    @Override
    public LMDirectory getRootDirectory() {
        return root;
    }

    //
    // Construction

    public LMSpace(String name, int totalSpace) throws IOException {
        super(totalSpace);

        this.name = name;

        this.memory = new BlockMemory<>(this::setAllocatedSpace, totalSpace);
        this.root = new LMDirectory(this);
    }

    // FUTURE: Loading / Saving - Load from file, save to file

    //
    // Implementation: Close

    @Override
    public void closeImpl() throws IOException {
        memory.close();
        root = null;
    }

}
