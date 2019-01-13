package dynfs.core.util;

import java.nio.file.spi.FileSystemProvider;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import dynfs.core.DynFileSystemProvider;
import dynfs.core.DynRoute;

public final class ProviderUtil {

    //
    // DynFileSystemProvider Constants

    public static final String S_ROOT = DynRoute.ROOT_PATH_STRING;
    public static final DynRoute R_ROOT = DynRoute.fromRouteNames(S_ROOT);

    //
    // Construction

    private ProviderUtil() {}

    //
    // DynFileSystemProvider Provision

    private static DynFileSystemProvider provider;
    static {
        initProvider();
    }

    public static DynFileSystemProvider provider() {
        return provider;
    }

    private static void initProvider() {
        List<FileSystemProvider> fsps = FileSystemProvider.installedProviders();
        Optional<FileSystemProvider> dfspo = fsps.stream()
                .filter(fsp -> fsp instanceof DynFileSystemProvider)
                .findFirst();

        if (dfspo.isPresent()) {
            provider = (DynFileSystemProvider) dfspo.get();
        } else {
            Assertions.fail("No DynFileSystemProvider instance was found");
        }
    }

    //
    // Test: Provider Installation

    @Test
    public void testProvider() {
        Assertions.assertNotNull(provider());
    }

}
