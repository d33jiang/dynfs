package dynfs.dynlm;

import java.util.Arrays;

@Deprecated
public class HeterogeneousBlockList<Owner> {

    // TODO: Future feature?

    //
    // State: Nested Blocks

    // private final NavigableMap<Integer, BlockLike> nested;

    //
    // Static Support: Block Size Calculation

    @SuppressWarnings("unused")
    private static int calculateTotalSize(BlockLike<?>[] args) {
        return Arrays.stream(args).mapToInt(BlockLike::capacity).sum();
    }

}
