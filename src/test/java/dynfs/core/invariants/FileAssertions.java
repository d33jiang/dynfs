package dynfs.core.invariants;

import java.io.IOException;

import org.junit.jupiter.api.Assertions;

import dynfs.core.DynFile;
import dynfs.core.DynFileSystem;
import dynfs.core.DynNode;
import dynfs.core.DynRoute;
import dynfs.core.invariants.NodeAssertions.DynNodeType;
import dynfs.core.io.FileIO;
import dynfs.dynlm.LMSpace;

public final class FileAssertions {

    //
    // Construction

    private FileAssertions() {}

    //
    // DynNode Invariants

    public static DynFile<LMSpace, ?> assertRegularFile(
            DynNode<LMSpace, ?> node,
            DynFileSystem<LMSpace> fs,
            DynRoute expectedRoute,
            String expectedName)
            throws IOException {
        NodeAssertions.assertNodeInvariants(node, fs.getStore(), expectedRoute, expectedName, DynNodeType.REGULAR_FILE);

        DynFile<LMSpace, ?> file = (DynFile<LMSpace, ?>) node;
        assertNodeSizeCorrectness(file, fs);

        return file;
    }

    //
    // Node Size Correctness

    static void assertNodeSizeCorrectness(DynFile<LMSpace, ?> file, DynFileSystem<LMSpace> fs) throws IOException {
        assertFileDataSize(file, fs, (int) file.readSize());
    }

    //
    // Data Size

    public static void assertFileDataSize(DynFile<LMSpace, ?> file, DynFileSystem<LMSpace> fs, int expectedDataSize) {
        byte[] actualData = FileIO.readFileData(fs, file.getRoute(), 0, expectedDataSize + 1);
        Assertions.assertEquals(expectedDataSize, actualData.length);
    }

    //
    // Data

    public static void assertFileData(DynFile<LMSpace, ?> file, DynFileSystem<LMSpace> fs, byte[] expectedData) {
        byte[] actualData = FileIO.readFileData(fs, file.getRoute(), 0, expectedData.length + 1);
        Assertions.assertArrayEquals(expectedData, actualData);
    }

}
