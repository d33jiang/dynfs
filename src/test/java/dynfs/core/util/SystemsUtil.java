package dynfs.core.util;

import java.io.IOException;
import java.nio.file.FileSystemNotFoundException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Assertions;

import com.google.common.collect.ImmutableSet;

import dynfs.core.DynFileSystem;
import dynfs.core.store.DynSpaceFactory;
import dynfs.dynlm.LMSpace;

public final class SystemsUtil {

    //
    // Construction

    private SystemsUtil() {}

    //
    // DynFileSystem Management

    private static Map<String, DynFileSystem<LMSpace>> systems;
    static {
        initSystems();
    }

    private static void initSystems() {
        systems = new HashMap<>();
    }

    public static void closeAllSystems() {
        Set<DynFileSystem<LMSpace>> closedSystems = ImmutableSet.copyOf(systems.values());
        closedSystems.forEach(fs -> {
            try {
                fs.close();
            } catch (IOException ex) {}
        });

        systems.values().removeAll(closedSystems);
    }

    //
    // DynFileSystem Existence Check

    private static void checkExistsDynFileSystem(String domain, boolean expectedExists) {
        if (expectedExists) {
            Assertions.assertTrue(systems.containsKey(domain));
            Assertions.assertNotNull(ProviderUtil.provider().getFileSystem(domain));
        } else {
            Assertions.assertFalse(systems.containsKey(domain));
            Assertions.assertThrows(FileSystemNotFoundException.class,
                    () -> ProviderUtil.provider().getFileSystem(domain));
        }

    }

    //
    // DynFileSystem Creation / Access / Destruction

    public static DynFileSystem<LMSpace> openSystem(String domain, int totalSpace) {
        checkExistsDynFileSystem(domain, false);

        try {
            DynFileSystem<LMSpace> fs = newDynFileSystem(domain, totalSpace);
            systems.put(domain, fs);

            return fs;
        } finally {
            checkExistsDynFileSystem(domain, true);
        }
    }

    public static DynFileSystem<LMSpace> getDynFileSystem(String domain) {
        checkExistsDynFileSystem(domain, true);
        return systems.get(domain);
    }

    public static void closeSystem(String domain) throws NullPointerException {
        checkExistsDynFileSystem(domain, true);

        try {
            DynFileSystem<LMSpace> fs = systems.get(domain);
            try {
                fs.close();
            } catch (IOException ex) {}
            systems.remove(domain, fs);
        } finally {
            checkExistsDynFileSystem(domain, false);
        }
    }

    private static DynFileSystem<LMSpace> newDynFileSystem(String domain, int totalSpace) {
        String fsName = String.format("[DynFileSystem: %s (Test)]", domain);
        DynSpaceFactory<LMSpace> fac = env -> new LMSpace(fsName, totalSpace);
        try {
            DynFileSystem<LMSpace> fs = ProviderUtil.provider().newFileSystem(domain, fac, null);
            return fs;
        } catch (IOException ex) {
            Assertions.fail("Could not create DynFileSystem", ex);
            return null;
        }
    }

}
