package dynfs.core.base;

import java.io.IOException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.google.common.collect.ImmutableSet;

import dynfs.core.DynDirectory;
import dynfs.core.DynFileSystem;
import dynfs.core.DynPath;
import dynfs.core.DynSpace;
import dynfs.core.DynSpaceType;
import dynfs.core.DynSpaceType.Locality;
import dynfs.core.DynSpaceType.Storage;
import dynfs.core.invariants.DirectoryAssertions;
import dynfs.core.util.SystemsUtil;
import dynfs.dynlm.Block;
import dynfs.dynlm.LMSpace;

public class SystemBase extends TestBase {

    //
    // Parameters

    private static final String TEST_SYSTEM_DOMAIN = "test-domain";
    private static final int TEST_SYSTEM_TOTAL_SPACE = Math.max(65_536, Block.sizeOfNBlocks(12));

    //
    // Test System

    private DynFileSystem<LMSpace> fs;
    private DynSpace<LMSpace> store;

    protected final DynFileSystem<LMSpace> fs() {
        return fs;
    }

    protected final DynSpace<LMSpace> store() {
        return store;
    }

    //
    // Root Directory

    private DynPath pRoot;
    private DynDirectory<LMSpace, ?> nRoot;

    protected final DynPath pRoot() {
        return pRoot;
    }

    protected final DynDirectory<LMSpace, ?> nRoot() {
        return nRoot;
    }

    //
    // Construction

    protected SystemBase() {}

    //
    // DynFileSystem Provision

    @BeforeEach
    private void initSystem() {
        fs = SystemsUtil.openSystem(TEST_SYSTEM_DOMAIN, TEST_SYSTEM_TOTAL_SPACE);
        store = fs.getStore();

        pRoot = fs.getRootDirectory();
        nRoot = store.getRootDirectory();
    }

    @AfterEach
    private void destroySystem() {
        SystemsUtil.closeSystem(TEST_SYSTEM_DOMAIN);
    }

    //
    // Test: DynFileSystem Initialization

    @Test
    public final void testSystem() throws IOException {
        Assertions.assertNotNull(fs);
        Assertions.assertEquals(TEST_SYSTEM_DOMAIN, fs.domain());
        Assertions.assertTrue(fs.isOpen());
        Assertions.assertFalse(fs.isReadOnly());

        Assertions.assertNotNull(store);
        Assertions.assertNotNull(store.name());
        Assertions.assertNotNull(store.getType());
        Assertions.assertNotNull(store.getRootDirectory());

        Assertions.assertEquals(new DynSpaceType(Locality.LOCAL, Storage.MEMORY), store.getType());
        Assertions.assertTrue(store.getTotalSpace() >= TEST_SYSTEM_TOTAL_SPACE);
    }

    //
    // Test: Root Directory

    @Test
    public final void testRootDirectory() throws IOException {
        // pRoot from fs.getRootDirectory()
        Assertions.assertEquals(pRoot, DynPath.newPath(fs(), rRoot()));

        // nRoot from store.getRootDirectory()
        Assertions.assertEquals(nRoot, fs().resolve(rRoot()).testExistence());

        assertEmptyRootDirectory();
    }

    protected final void assertEmptyRootDirectory() throws IOException {
        DirectoryAssertions.assertRootDirectory(nRoot, store);
        DirectoryAssertions.assertDirectoryChildrenEquals(nRoot, ImmutableSet.of());
    }

}
