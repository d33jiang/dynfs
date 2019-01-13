package dynfs.core.invariants;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Assertions;

import com.google.common.collect.Sets;
import com.google.common.collect.Streams;

import dynfs.core.DynDirectory;
import dynfs.core.DynNode;
import dynfs.core.DynRoute;
import dynfs.core.DynSpace;
import dynfs.core.invariants.NodeAssertions.DynNodeType;
import dynfs.core.util.ProviderUtil;
import dynfs.dynlm.LMSpace;

public final class DirectoryAssertions {

    //
    // Construction

    private DirectoryAssertions() {}

    //
    // DynDirectory Invariants

    public static DynDirectory<LMSpace, ?> assertDirectory(
            DynNode<LMSpace, ?> node,
            DynSpace<LMSpace> store,
            DynRoute expectedRoute,
            String expectedName)
            throws IOException {
        NodeAssertions.assertNodeInvariants(node, store, expectedRoute, expectedName, DynNodeType.DIRECTORY);
        NodeAssertions.assertNodeSize(node, 0);

        DynDirectory<LMSpace, ?> dir = (DynDirectory<LMSpace, ?>) node;
        assertParentChildCorrespondence(dir);
        assertDirectoryIsEmptyCorrectness(dir);

        return dir;
    }

    //
    // Parent-Child Correspondence

    static void assertParentChildCorrespondence(DynDirectory<LMSpace, ?> parentNode) {
        Map<String, DynNode<LMSpace, ?>> childrenNameToNodeMap = Streams.stream(parentNode)
                .collect(Collectors.toMap(
                        childNode -> childNode.getName(),
                        childNode -> childNode));

        childrenNameToNodeMap.forEach((childName, childNode) -> {
            try {
                Assertions.assertEquals(childNode, parentNode.resolveChild(childName));
            } catch (IOException ex) {
                Assertions.fail("I/O Exception while resolving child", ex);
            }

            Assertions.assertEquals(parentNode, childNode.getParent());
        });
    }

    //
    // Emptiness Check

    public static void assertDirectoryIsEmpty(DynDirectory<LMSpace, ?> dir, boolean isEmpty) {
        Assertions.assertEquals(isEmpty, dir.isEmpty());
        assertDirectoryIsEmptyCorrectness(dir);
    }

    static void assertDirectoryIsEmptyCorrectness(DynDirectory<LMSpace, ?> dir) {
        Assertions.assertEquals(getChildrenOf(dir).isEmpty(), dir.isEmpty());
    }

    //
    // Children

    public static void assertDirectoryChildrenContains(
            DynDirectory<LMSpace, ?> parentNode,
            Set<String> setOfExpectedChildren)
            throws IOException {
        Set<String> setOfActualChildren = getChildrenOf(parentNode);
        Assertions.assertTrue(Sets.difference(setOfExpectedChildren, setOfActualChildren).isEmpty());
    }

    public static void assertDirectoryChildrenEquals(
            DynDirectory<LMSpace, ?> parentNode,
            Set<String> setOfExpectedChildren)
            throws IOException {
        Assertions.assertEquals(setOfExpectedChildren, getChildrenOf(parentNode));
    }

    static Set<String> getChildrenOf(DynDirectory<LMSpace, ?> parentNode) {
        return Streams.stream(parentNode)
                .map(DynNode::getName)
                .collect(Collectors.toSet());
    }

    //
    // Root Directory

    public static void assertRootDirectory(DynDirectory<LMSpace, ?> dir, DynSpace<LMSpace> store) throws IOException {
        assertDirectory(dir, store, ProviderUtil.R_ROOT, null);
    }

}
