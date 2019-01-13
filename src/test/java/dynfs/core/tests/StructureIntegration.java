package dynfs.core.tests;

import java.io.IOException;
import java.nio.file.DirectoryNotEmptyException;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.google.common.collect.ImmutableSet;

import dynfs.core.DynDirectory;
import dynfs.core.DynFile;
import dynfs.core.DynNode;
import dynfs.core.DynPath;
import dynfs.core.DynRoute;
import dynfs.core.ResolutionResult;
import dynfs.core.base.SystemBase;
import dynfs.core.invariants.DirectoryAssertions;
import dynfs.core.invariants.FileAssertions;
import dynfs.core.invariants.NodeAssertions;
import dynfs.core.io.DirectoryIO;
import dynfs.core.io.FileIO;
import dynfs.dynlm.LMSpace;

public class StructureIntegration extends SystemBase {

    //
    // Support: Existence Assertion

    private void assertExists(DynRoute route, boolean expectedExists) throws IOException {
        Assertions.assertEquals(expectedExists, fs().resolve(route).exists());
    }

    //
    // Test: Creation / Deletion

    @Test
    public void testCreationDeletion() throws IOException {

        assertEmptyRootDirectory();

        //

        String S_PRIMARY_DIRECTORY = "foo";
        DynDirectory<LMSpace, ?> nFoo = tryDirectoryCreation(rRoot(), S_PRIMARY_DIRECTORY, null);
        DynRoute rFoo = nFoo.getRoute();
        Assertions.assertEquals("/foo", rFoo.toString());

        //

        String S_NESTED_DIRECTORY = "bar";
        DynDirectory<LMSpace, ?> nFooBar = tryDirectoryCreation(rFoo, S_NESTED_DIRECTORY, null);
        DynRoute rFooBar = nFooBar.getRoute();
        Assertions.assertEquals("/foo/bar", rFooBar.toString());

        //

        String S_PRIMARY_FILE = "doc.txt";
        tryFileCreation(rRoot(), S_PRIMARY_FILE, "0123456".getBytes(), null);

        //

        String S_NESTED_FILE = ".fstmp";
        tryFileCreation(rFoo, S_NESTED_FILE, "asdfghjkl".getBytes(), null);

        DynRoute rFooFstmp = rFoo.resolve(S_NESTED_FILE);

        //

        tryDelete(rFoo, DirectoryNotEmptyException.class);
        tryDelete(rFooBar, null);
        tryDelete(rFoo, DirectoryNotEmptyException.class);
        tryDelete(rFooFstmp, null);
        tryDelete(rFoo, null);
    }

    //
    // Test Submodules

    private DynFile<LMSpace, ?> tryFileCreation(DynRoute parentRoute, String name, byte[] data,
            Class<? extends IOException> expectedFailure) throws IOException {
        DynRoute childRoute = parentRoute.resolve(name);

        if (expectedFailure == null) {
            assertExists(childRoute, false);
            FileIO.writeFileContent(fs(), childRoute, 0, data);
            assertExists(childRoute, true);

            ResolutionResult<LMSpace> resolution = fs().resolve(childRoute);

            DynNode<LMSpace, ?> node = resolution.testExistence();
            NodeAssertions.assertNodeSize(node, data.length);

            DirectoryAssertions.assertDirectoryChildrenContains(resolution.lastParent(), ImmutableSet.of(name));
            Assertions.assertFalse(node.isRoot());

            DynFile<LMSpace, ?> file = FileAssertions.assertRegularFile(node, fs(), childRoute, name);
            FileAssertions.assertFileData(file, fs(), data);

            return file;
        } else {
            Assertions.assertThrows(expectedFailure, () -> FileIO.writeFileContent(fs(), childRoute, 0, data));
            return null;
        }

    }

    private DynDirectory<LMSpace, ?> tryDirectoryCreation(DynRoute parentRoute, String name,
            Class<? extends IOException> expectedFailure) throws IOException {
        DynRoute childRoute = parentRoute.resolve(name);

        if (expectedFailure == null) {
            assertExists(childRoute, false);
            DirectoryIO.createDirectory(fs(), childRoute);
            assertExists(childRoute, true);

            ResolutionResult<LMSpace> resolution = fs().resolve(childRoute);

            DynNode<LMSpace, ?> node = resolution.testExistence();

            DirectoryAssertions.assertDirectoryChildrenContains(resolution.lastParent(), ImmutableSet.of(name));
            Assertions.assertFalse(node.isRoot());

            DynDirectory<LMSpace, ?> dir = DirectoryAssertions.assertDirectory(node, store(), childRoute, name);
            DirectoryAssertions.assertDirectoryIsEmpty(dir, true);

            return dir;
        } else {
            Assertions.assertThrows(expectedFailure, () -> DirectoryIO.createDirectory(fs(), childRoute));
            return null;
        }
    }

    private void tryDelete(DynRoute route, Class<? extends IOException> expectedFailure) throws IOException {
        assertExists(route, true);

        DynPath path = DynPath.newPath(fs(), route);

        if (expectedFailure == null) {
            provider().delete(path);
            assertExists(route, false);
        } else {
            Assertions.assertThrows(expectedFailure, () -> provider().delete(path));
            assertExists(route, true);
        }

    }

}
