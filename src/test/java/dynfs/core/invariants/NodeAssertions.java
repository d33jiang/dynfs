package dynfs.core.invariants;

import java.io.IOException;

import org.junit.jupiter.api.Assertions;

import dynfs.core.DynDirectory;
import dynfs.core.DynFile;
import dynfs.core.DynLink;
import dynfs.core.DynNode;
import dynfs.core.DynRoute;
import dynfs.core.DynSpace;
import dynfs.dynlm.LMSpace;

public final class NodeAssertions {

    //
    // Construction

    private NodeAssertions() {}

    //
    // DynNode Invariants

    public static void assertNodeInvariants(
            DynNode<LMSpace, ?> node,
            DynSpace<LMSpace> store,
            DynRoute expectedRoute,
            String expectedName,
            DynNodeType expectedType) {
        assertStore(node, store);
        assertRoute(node, expectedRoute);
        assertName(node, expectedName);
        assertType(node, expectedType);
    }

    //
    // Store

    public static void assertStore(DynNode<LMSpace, ?> node, DynSpace<LMSpace> store) {
        Assertions.assertEquals(store, node.getStore());
    }

    //
    // Route

    public static void assertRoute(DynNode<LMSpace, ?> node, DynRoute expectedRoute) {
        Assertions.assertEquals(expectedRoute, node.getRoute());
    }

    //
    // Name

    public static void assertName(DynNode<LMSpace, ?> node, String expectedName) {
        Assertions.assertEquals(expectedName, node.getName());
        Assertions.assertEquals(expectedName == null, node.isRoot());
    }

    //
    // Type

    public static enum DynNodeType {
        REGULAR_FILE, DIRECTORY, SYMBOLIC_LINK;
    }

    public static void assertType(DynNode<LMSpace, ?> node, DynNodeType expectedType) {
        Assertions.assertEquals(expectedType == DynNodeType.REGULAR_FILE, node.isRegularFile());
        Assertions.assertEquals(expectedType == DynNodeType.DIRECTORY, node.isDirectory());
        Assertions.assertEquals(expectedType == DynNodeType.SYMBOLIC_LINK, node.isSymbolicLink());
        Assertions.assertEquals(expectedType == null, node.isOther());

        switch (expectedType) {
            case REGULAR_FILE:
                Assertions.assertTrue(node instanceof DynFile);
                break;
            case DIRECTORY:
                Assertions.assertTrue(node instanceof DynDirectory);
                break;
            case SYMBOLIC_LINK:
                Assertions.assertTrue(node instanceof DynLink);
                break;
            default:
                break;
        }
    }

    //
    // Size

    public static void assertNodeSize(DynNode<LMSpace, ?> node, long expectedSize) {
        try {
            Assertions.assertEquals(expectedSize, node.readSize());
        } catch (IOException ex) {
            Assertions.fail("I/O Exception during size check", ex);
        }
    }

}
